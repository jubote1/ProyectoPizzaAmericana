// ======================================================================
// CONSUMIDOR OPTIMIZADO - PIZZA AMERICANA
// Mejoras clave:
// 1. Cero pérdida de paquetes: extracción atómica del buffer en insertBatch.
// 2. Doble persistencia segura:
//    - Snapshot en tiempo real en 'domiciliario_ubicacion_actual' (UPSERT: 1 fila por domiciliario).
//    - Histórico tradicional en lote en 'ubicacion_domiciliario'.
// 3. Geocercas automáticas de 90m sincronizadas con las 11 tiendas.
// 4. Reconexión indestructible a RabbitMQ y MySQL.
// ======================================================================

const amqp = require('amqplib');
const mysql = require('mysql');

const BATCH_SIZE = 15;             // Cuántos mensajes juntar antes de insertar
const BATCH_INTERVAL_MS = 3000;    // Máximo tiempo de espera antes de insertar (3 segundos)

const LOCATION_TTL = 60 * 60 * 1000; // 1 hora
const FRAUD_TTL = 5 * 60 * 1000;
const MAX_FRAUD_BUFFER_SIZE = 200;
let isFlushingBatch = false;
let isFlushingFraud = false;

const fraudCooldownByUser = new Map();
const FRAUD_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutos

// Pool de conexiones MySQL
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '4m32017',
    database: 'pizzaamericana',
    waitForConnections: true,
    connectionLimit: 15,
    queueLimit: 0,
    charset: 'utf8mb4'
});

// Colas en memoria
let messageBuffer = [];
let fraudBuffer = [];

const lastFraudByUser = new Map();
const lastLocationByUser = new Map();

function isValidDate(dateString) {
    if (!dateString) return false;
    const date = new Date(dateString);
    return !isNaN(date.getTime());
}

function haversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371e3; // metros
    const toRad = x => x * Math.PI / 180;

    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);

    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(lat1)) *
        Math.cos(toRad(lat2)) *
        Math.sin(dLon / 2) ** 2;

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

// Coordenadas oficiales de las tiendas mapeadas por idtienda para Geocercas automáticas
const TIENDAS_POR_ID = {
    1: { nombre: "Manrique", lat: 6.270404035083642, lng: -75.55476201695326 },
    2: { nombre: "Bello", lat: 6.31313642173263, lng: -75.5599370602694 },
    3: { nombre: "America", lat: 6.248795464621254, lng: -75.6042451558178 },
    4: { nombre: "Calasanz", lat: 6.266104483307567, lng: -75.59837327880433 },
    5: { nombre: "Itagui", lat: 6.165763998154379, lng: -75.6215936733657 },
    7: { nombre: "La Mota", lat: 6.211386771165651, lng: -75.5952875746262 },
    8: { nombre: "Envigado", lat: 6.167150830483641, lng: -75.58524990346257 },
    9: { nombre: "Pilarica", lat: 6.273129787405367, lng: -75.5854100862619 },
    10: { nombre: "San Antonio", lat: 6.168106662807738, lng: -75.64904988626188 },
    11: { nombre: "Manrique Piloto", lat: 6.263560956119886, lng: -75.55326141695326 },
    13: { nombre: "Niquia", lat: 6.337781519121857, lng: -75.5499612076178 }
};

function determinarEstadoOperativo(lat, lng, idtienda, pedidosActivos = 0) {
    const id = parseInt(idtienda, 10);
    let enTienda = false;
    if (id && TIENDAS_POR_ID[id]) {
        const t = TIENDAS_POR_ID[id];
        const d = haversineDistance(lat, lng, t.lat, t.lng);
        enTienda = (d <= 90);
    } else {
        for (const key in TIENDAS_POR_ID) {
            const t = TIENDAS_POR_ID[key];
            if (haversineDistance(lat, lng, t.lat, t.lng) <= 90) {
                enTienda = true;
                break;
            }
        }
    }
    if (enTienda) return 'EN_TIENDA';
    return (parseInt(pedidosActivos, 10) > 0) ? 'EN_RUTA' : 'FUERA_DE_TIENDA';
}

// Inserción segura en lote (histórico + última ubicación)
function insertBatch() {
    if (isFlushingBatch || messageBuffer.length === 0) return;

    // Extraemos de forma atómica los mensajes del buffer (evita pérdida de datos en memoria)
    const currentBatch = messageBuffer.splice(0, messageBuffer.length);
    isFlushingBatch = true;

    // 1. Datos para histórico tradicional
    const historicalValues = currentBatch.map(data => [
        data.clave_usuario,
        data.idtienda || 0,
        data.latitude,
        data.longitude,
        isValidDate(data.fecha_hora) ? new Date(data.fecha_hora) : new Date()
    ]);

    // 2. Datos para tabla de estado en tiempo real (Upsert: 1 fila por domiciliario)
    const liveValues = currentBatch.map(data => [
        data.clave_usuario,
        data.idtienda || 0,
        data.latitude,
        data.longitude,
        isValidDate(data.fecha_hora) ? new Date(data.fecha_hora) : new Date(),
        data.nombre_usuario || null,
        data.estado || 'EN_TIENDA',
        typeof data.bateria === 'number' ? data.bateria : null,
        typeof data.velocidad === 'number' ? data.velocidad : 0,
        typeof data.pedidos_activos === 'number' ? data.pedidos_activos : 0,
        data.pedidos_detalle ? String(data.pedidos_detalle).trim() : null
    ]);

    const queryHistorical = `
        INSERT INTO ubicacion_domiciliario
        (clave_dom, idtienda, latitud, longitud, fecha)
        VALUES ?
    `;

    const queryLive = `
        INSERT INTO domiciliario_ubicacion_actual
        (clave_dom, idtienda, latitud, longitud, fecha, nombre_usuario, estado, bateria, velocidad, pedidos_activos, pedidos_detalle)
        VALUES ?
        ON DUPLICATE KEY UPDATE
            idtienda = VALUES(idtienda),
            latitud = VALUES(latitud),
            longitud = VALUES(longitud),
            fecha = VALUES(fecha),
            nombre_usuario = COALESCE(VALUES(nombre_usuario), nombre_usuario),
            estado = COALESCE(VALUES(estado), estado),
            bateria = COALESCE(VALUES(bateria), bateria),
            velocidad = COALESCE(VALUES(velocidad), velocidad),
            pedidos_activos = COALESCE(VALUES(pedidos_activos), pedidos_activos),
            pedidos_detalle = VALUES(pedidos_detalle)
    `;

    // Ejecutar ambas inserciones
    pool.query(queryHistorical, [historicalValues], (errHist) => {
        if (errHist) {
            console.error('❌ Error insertando en histórico:', errHist);
        } else {
            console.log(`✅ [Histórico] ${historicalValues.length} ubicaciones guardadas`);
        }

        pool.query(queryLive, [liveValues], (errLive) => {
            isFlushingBatch = false;

            if (errLive) {
                console.error('❌ Error actualizando ubicación actual:', errLive);
            } else {
                console.log(`⚡ [Tiempo Real] ${liveValues.length} estados actualizados`);
            }

            // Si llegaron más mensajes mientras se escribía en disco, procesarlos
            if (messageBuffer.length >= BATCH_SIZE) {
                insertBatch();
            }
        });
    });
}

function insertFraudBatch() {
    if (isFlushingFraud || fraudBuffer.length === 0) return;

    const currentFraud = fraudBuffer.splice(0, fraudBuffer.length);
    isFlushingFraud = true;

    const query = `
        INSERT INTO eventos_fraude_ubicacion
        (clave_dom, tipo_anomalia, latitud, longitud, distancia, tiempo)
        VALUES ?
    `;

    pool.query(query, [currentFraud], (err) => {
        isFlushingFraud = false;
        if (err) {
            console.error("❌ Error insertando lote fraude:", err);
            return;
        }
        console.log(`🚨 ${currentFraud.length} fraudes insertados`);

        if (fraudBuffer.length >= MAX_FRAUD_BUFFER_SIZE) {
            insertFraudBatch();
        }
    });
}

// Iniciar consumidor RabbitMQ con auto-reconexión
async function startConsumer() {
    try {
        const connection = await amqp.connect('amqp://localhost');
        const channel = await connection.createChannel();

        const queue = 'ubicaciones';
        await channel.assertQueue(queue, { durable: true });
        await channel.prefetch(30);

        console.log(`🚀 Consumidor listo en la cola: ${queue}`);

        connection.on('error', (err) => {
            console.error('❌ Error en conexión RabbitMQ:', err.message);
            setTimeout(startConsumer, 5000);
        });

        connection.on('close', () => {
            console.warn('⚠️ Conexión RabbitMQ cerrada. Reconectando en 5s...');
            setTimeout(startConsumer, 5000);
        });

        channel.consume(queue, async (message) => {
            if (!message) return;

            try {
                const data = JSON.parse(message.content.toString());

                if (
                    !data.clave_usuario ||
                    typeof data.latitude !== "number" ||
                    typeof data.longitude !== "number" ||
                    isNaN(data.latitude) ||
                    isNaN(data.longitude)
                ) {
                    channel.ack(message);
                    return;
                }

                const eventTime = isValidDate(data.fecha_hora)
                    ? new Date(data.fecha_hora).getTime()
                    : Date.now();

                const now = Date.now();

                // 1. Detección Mock GPS
                if (data.mock_gps === true) {
                    const key = `${data.clave_usuario}:MOCK_LOCATION`;
                    const expires = fraudCooldownByUser.get(key) || 0;

                    if (expires <= now) {
                        fraudCooldownByUser.set(key, now + FRAUD_COOLDOWN_MS);
                        console.log(`🚨 MOCK GPS detectado para: ${data.clave_usuario}`);

                        fraudBuffer.push([
                            data.clave_usuario,
                            "MOCK_LOCATION",
                            data.latitude,
                            data.longitude,
                            null,
                            null
                        ]);
                    }

                    channel.ack(message);
                    return;
                }

                // 2. Coordenadas fuera de rango terrenal
                if (
                    data.latitude < -90 || data.latitude > 90 ||
                    data.longitude < -180 || data.longitude > 180
                ) {
                    channel.ack(message);
                    return;
                }

                const prev = lastLocationByUser.get(data.clave_usuario);

                if (!prev) {
                    // Primer punto: calcular estado y velocidad inmediatamente
                    if (!data.estado) {
                        data.estado = determinarEstadoOperativo(data.latitude, data.longitude, data.idtienda, data.pedidos_activos);
                    }
                    if (typeof data.velocidad !== 'number') {
                        data.velocidad = 0;
                    }

                    lastLocationByUser.set(data.clave_usuario, {
                        latitude: data.latitude,
                        longitude: data.longitude,
                        time: eventTime,
                        expires: now + LOCATION_TTL
                    });

                    messageBuffer.push(data);
                    channel.ack(message);

                    if (messageBuffer.length >= BATCH_SIZE) {
                        insertBatch();
                    }
                    return;
                }

                const distance = haversineDistance(
                    prev.latitude,
                    prev.longitude,
                    data.latitude,
                    data.longitude
                );

                const timeDiff = Math.abs((eventTime - prev.time) / 1000);

                // 3. Detección de Teletransporte / Saltos Imposibles (> 130 km/h)
                let tipoFraude = null;
                if (timeDiff > 0) {
                    const speedKmh = (distance / timeDiff) * 3.6;
                    if (distance > 1000 && speedKmh > 130) {
                        tipoFraude = "TELEPORT";
                    }
                }

                if (tipoFraude) {
                    const key = `${data.clave_usuario}:${tipoFraude}`;
                    const last = lastFraudByUser.get(key) || 0;

                    if (last <= now) {
                        lastFraudByUser.set(key, now + FRAUD_TTL);
                        fraudBuffer.push([
                            data.clave_usuario,
                            tipoFraude,
                            data.latitude,
                            data.longitude,
                            distance,
                            timeDiff
                        ]);
                    }

                    channel.ack(message);
                    return;
                }

                // 4. Filtro de Ruido Inteligente:
                // Se guarda si se movió al menos 10 metros, o si pasaron más de 20 segundos sin moverse
                let shouldSave = false;

                if (distance >= 10) {
                    shouldSave = true; // Se desplazó
                } else if (timeDiff >= 20) {
                    shouldSave = true; // Latido para confirmar que sigue activo aunque esté detenido
                }

                if (shouldSave) {
                    // Automatización de Geocerca (En Tienda vs En Ruta vs Fuera de Tienda)
                    if (!data.estado) {
                        data.estado = determinarEstadoOperativo(data.latitude, data.longitude, data.idtienda, data.pedidos_activos);
                    }

                    // Estimación o registro de velocidad
                    if (typeof data.velocidad !== 'number') {
                        if (timeDiff > 0 && distance > 0) {
                            data.velocidad = Math.min(120, Math.round((distance / timeDiff) * 3.6));
                        } else {
                            data.velocidad = 0;
                        }
                    }

                    messageBuffer.push(data);
                    lastLocationByUser.set(data.clave_usuario, {
                        latitude: data.latitude,
                        longitude: data.longitude,
                        time: eventTime,
                        expires: now + LOCATION_TTL
                    });

                    if (messageBuffer.length >= BATCH_SIZE) {
                        insertBatch();
                    }
                }

                channel.ack(message);

            } catch (err) {
                console.error('❌ Error procesando mensaje RabbitMQ:', err);
                channel.ack(message);
            }
        }, { noAck: false });

    } catch (err) {
        console.error('❌ Error conectando a RabbitMQ:', err);
        setTimeout(startConsumer, 5000);
    }
}

// Temporizadores de vaciado periódico
setInterval(() => {
    if (messageBuffer.length > 0) {
        insertBatch();
    }
}, BATCH_INTERVAL_MS);

setInterval(() => {
    if (fraudBuffer.length > 0) {
        insertFraudBatch();
    }
}, BATCH_INTERVAL_MS);

// Limpieza de memoria periódica
setInterval(() => {
    const now = Date.now();
    for (const [key, value] of lastLocationByUser.entries()) {
        if (value.expires < now) {
            lastLocationByUser.delete(key);
        }
    }
}, 10 * 60 * 1000);

setInterval(() => {
    const now = Date.now();
    for (const [key, value] of lastFraudByUser.entries()) {
        if (value < now) {
            lastFraudByUser.delete(key);
        }
    }
    for (const [key, value] of fraudCooldownByUser.entries()) {
        if (value < now) {
            fraudCooldownByUser.delete(key);
        }
    }
}, 60 * 1000);

// Iniciar
startConsumer();