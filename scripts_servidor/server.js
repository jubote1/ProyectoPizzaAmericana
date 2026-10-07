// ======================================================================
// SERVER SOCKET.IO CON PERSISTENCIA DE SESIONES EN MYSQL - PIZZA AMERICANA
// Mejoras clave:
// 1. PLAN A: Transferencia limpia de sesión. Si el domiciliario cambia de
//    celular, formatea o reinstala la app, entra de inmediato. Si el celular
//    anterior sigue conectado en vivo, se le envía 'force_logout'.
// 2. PERSISTENCIA DE DUEÑOS OFICIALES: Guarda el dispositivo activo en MySQL
//    (domiciliario_dispositivo_sesion). Los reinicios de PM2 no pierden la sesión.
// 3. Soporte de salas por tienda ('tienda_${idtienda}') para monitores web.
// 4. Reconexión continua y tolerante a fallos de RabbitMQ.
// 5. Notificaciones push FCM para reactivación de servicio.
// ======================================================================

const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const amqp = require('amqplib');
const mysql = require('mysql');
const net = require('net');
const admin = require('firebase-admin');

const app = express();
const server = http.createServer(app);
const port = 49323;

// 1. Inicialización de Firebase Admin
try {
    const serviceAccount = require("./rastreo-domiciliario-fire.json");
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
} catch (e) {
    console.warn("⚠️ Advertencia: No se pudo inicializar Firebase Admin:", e.message);
}

// 2. Pool MySQL para persistencia de sesiones
const dbPool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '4m32017',
    database: 'pizzaamericana',
    connectionLimit: 5,
    charset: 'utf8mb4'
});

const io = socketIo(server, {
    cors: { origin: "*", methods: ["GET", "POST"] },
    path: "/socket.io/",
    pingInterval: 25000,
    pingTimeout: 60000,
});

let rabbitChannel = null;
let rabbitConnection = null;
let isConnectingRabbit = false;

const retryQueue = [];
const lastSeenByUser = {};
const lastPushSent = {};
const tokensByUser = {};

// Memoria rápida respaldada por MySQL
const activeSessions = new Map();
const authorizedDevices = new Map();

const LIMITE_INACTIVIDAD = 10 * 60 * 1000;

// Cálculo de distancia para Geocercas automáticas
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

// -------------------------------------------------------------
// CARGA Y PERSISTENCIA DE DISPOSITIVOS EN MYSQL
// -------------------------------------------------------------
function cargarDispositivosDesdeBD() {
    dbPool.query(
        "SELECT clave_dom, device_id, fcm_token FROM domiciliario_dispositivo_sesion WHERE estado = 'AUTORIZADO'",
        (err, results) => {
            if (err) {
                console.warn("⚠️ Tabla de sesiones aún no creada o error al leer:", err.message);
                return;
            }
            results.forEach(row => {
                authorizedDevices.set(row.clave_dom, row.device_id);
                if (row.fcm_token) {
                    tokensByUser[row.clave_dom] = row.fcm_token;
                }
            });
            console.log(`🔐 ${results.length} dispositivos autorizados cargados desde MySQL.`);
        }
    );
}

function guardarDispositivoEnBD(clave, deviceId, nombre, token) {
    const query = `
        INSERT INTO domiciliario_dispositivo_sesion
        (clave_dom, device_id, fcm_token, nombre_usuario, estado, ultima_conexion)
        VALUES (?, ?, ?, ?, 'AUTORIZADO', NOW())
        ON DUPLICATE KEY UPDATE
            device_id = VALUES(device_id),
            fcm_token = COALESCE(VALUES(fcm_token), fcm_token),
            nombre_usuario = COALESCE(VALUES(nombre_usuario), nombre_usuario),
            estado = 'AUTORIZADO',
            ultima_conexion = NOW()
    `;
    dbPool.query(query, [clave, deviceId, token || null, nombre], (err) => {
        if (err) console.error(`❌ Error guardando dispositivo ${clave} en BD:`,                                                                                                                                                              err.message);
        else console.log(`💾 Dispositivo oficial persistido en MySQL: ${clave} -> ${deviceId}`);
    });
}

function actualizarTokenEnBD(clave, token) {
    dbPool.query(
        "UPDATE domiciliario_dispositivo_sesion SET fcm_token = ?, ultima_conexion = NOW() WHERE clave_dom = ?",
        [token, clave],
        (err) => {
            if (err) console.error("❌ Error actualizando token en BD:", err.message);
        }
    );
}

// -------------------------------------------------------------
// COLA RABBITMQ
// -------------------------------------------------------------
setInterval(() => {
    if (rabbitChannel && retryQueue.length > 0) {
        const data = retryQueue.shift();
        sendToQueue(data);
    }
}, 1500);

function sendToQueue(data) {
    try {
        if (!rabbitChannel) {
            if (retryQueue.length < 500) retryQueue.push(data);
            return;
        }

        const ok = rabbitChannel.sendToQueue(
            'ubicaciones',
            Buffer.from(JSON.stringify(data)),
            { persistent: true }
        );

        if (!ok && retryQueue.length < 500) {
            retryQueue.push(data);
        }
    } catch (err) {
        if (retryQueue.length < 500) retryQueue.push(data);
    }
}

const connectToRabbitMQ = async () => {
    if (isConnectingRabbit) return;
    isConnectingRabbit = true;

    try {
        console.log("🔄 Conectando a RabbitMQ...");
        rabbitConnection = await amqp.connect('amqp://localhost');
        rabbitChannel = await rabbitConnection.createChannel();
        await rabbitChannel.assertQueue('ubicaciones', { durable: true });
        console.log("✅ Conectado a RabbitMQ exitosamente.");

        isConnectingRabbit = false;

        rabbitConnection.on('error', (err) => {
            console.error("❌ Error en conexión RabbitMQ:", err.message);
            reconnectRabbitMQ();
        });

        rabbitConnection.on('close', () => {
            console.warn("⚠️ Conexión RabbitMQ cerrada. Reconectando...");
            reconnectRabbitMQ();
        });

    } catch (error) {
        console.error("❌ Falló conexión a RabbitMQ:", error.message);
        isConnectingRabbit = false;
        reconnectRabbitMQ();
    }
};

const reconnectRabbitMQ = () => {
    rabbitChannel = null;
    rabbitConnection = null;
    setTimeout(connectToRabbitMQ, 4000);
};

const checkPortAndStartServer = async (portToUse) => {
    const isPortInUse = await new Promise((resolve) => {
        const testServer = net.createServer()
            .once('error', (err) => resolve(err.code === 'EADDRINUSE'))
            .once('listening', () => testServer.close(() => resolve(false)))
            .listen(portToUse);
    });

    if (isPortInUse) {
        console.error(`❌ Puerto ${portToUse} ya está en uso.`);
        process.exit(1);
    }

    server.listen(portToUse, () => console.log(`🚀 Location Gateway corriendo en puerto ${portToUse}`));
};

// -------------------------------------------------------------
// NOTIFICACIONES PUSH
// -------------------------------------------------------------
const enviarPushReactivacion = async (clave) => {
    try {
        const token = tokensByUser[clave];
        const deviceId = authorizedDevices.get(clave);

        if (!token || !deviceId) return;

        await admin.messaging().send({
            token,
            data: {
                type: "REACTIVAR_SERVICIO",
                clave,
                device_id: deviceId
            },
            android: { priority: "high" }
        });
    } catch (error) {
        if (error.code === 'messaging/registration-token-not-registered') {
            delete tokensByUser[clave];
        }
    }
};

// -------------------------------------------------------------
// AUTORIZACIÓN Y EXPULSIÓN DE SESIONES
// -------------------------------------------------------------
function authorizeSocket(socket, clave, nombre, deviceId, token) {
    const oldSession = activeSessions.get(clave);

    // Si había una sesión activa previa
    if (oldSession && oldSession.socketId !== socket.id) {
        const oldSocket = io.sockets.sockets.get(oldSession.socketId);

        // Si es un dispositivo diferente, desconectar al anterior (force_logout)
        if (oldSession.deviceId !== deviceId) {
            console.log(`⚠️ Expulsando sesión anterior en otro dispositivo (${oldSession.deviceId}) para: ${clave}`);
            io.to(oldSession.socketId).emit('force_logout');

            if (oldSocket) {
                oldSocket.replacedByNewSocket = true;
                oldSocket.isAuthorized = false;
                setTimeout(() => {
                    if (oldSocket.connected) oldSocket.disconnect(true);
                }, 500);
            }
        } else if (oldSocket) {
            // Mismo dispositivo reconectando
            oldSocket.replacedByNewSocket = true;
            setTimeout(() => {
                const sesionActual = activeSessions.get(clave);
                if (sesionActual && sesionActual.socketId === socket.id && oldSocket.id !== socket.id && oldSocket.connected) {
                    oldSocket.disconnect(true);
                }
            }, 5000);
        }
    }

    socket.isAuthorized = true;
    socket.datosDomiciliario = { clave, nombre, deviceId };

    activeSessions.set(clave, { socketId: socket.id, deviceId });
    lastSeenByUser[clave] = Date.now();

    if (token) tokensByUser[clave] = token;

    // Persistir nuevo dispositivo en MySQL
    guardarDispositivoEnBD(clave, deviceId, nombre, token);

    socket.emit('login_approved');
    console.log(`✅ Login aprobado: ${nombre} (${clave}) en dispositivo: ${deviceId}`);
}

// -------------------------------------------------------------
// GESTIÓN DE EVENTOS SOCKET.IO
// -------------------------------------------------------------
io.on('connection', (socket) => {
    socket.isAuthorized = false;
    socket.replacedByNewSocket = false;

    // Salas por tienda para monitores web
    socket.on('unirse_tienda', (idtienda) => {
        const room = `tienda_${idtienda}`;
        socket.join(room);
    });

    socket.on('salir_tienda', (idtienda) => {
        const room = `tienda_${idtienda}`;
        socket.leave(room);
    });

    // Login desde App Android (PLAN A - Transferencia directa con force_logout)
    socket.on('conection', (data) => {
        if (!data || !data.clave_usuario || !data.device_id) return;

        const clave = String(data.clave_usuario).trim();
        const deviceId = String(data.device_id).trim();
        const nombre = data.nombre_usuario || "Desconocido";
        const token = data.token;

        console.log(`📱 Solicitud de conexión: ${nombre} (${clave}) [ID: ${deviceId}]`);

        // Registrar nuevo dispositivo autorizado y aprobar sesión
        authorizedDevices.set(clave, deviceId);
        authorizeSocket(socket, clave, nombre, deviceId, token);
    });

    // Recepción de coordenadas GPS
    socket.on('location', (data) => {
        if (!socket.isAuthorized || !socket.datosDomiciliario?.clave) return;

        const clave = socket.datosDomiciliario.clave;
        const duenoOficial = authorizedDevices.get(clave);

        if (duenoOficial !== socket.datosDomiciliario.deviceId) {
            socket.emit('force_logout');
            socket.disconnect(true);
            return;
        }

        data.clave_usuario = clave;
        lastSeenByUser[clave] = Date.now();

        // 1. Determinar Geocerca Operativa (En Tienda vs En Ruta vs Fuera de Tienda)
        data.estado = determinarEstadoOperativo(data.latitude, data.longitude, data.idtienda, data.pedidos_activos);

        // 2. Encolar en RabbitMQ para inserción en Base de Datos (en lote)
        sendToQueue(data);

        // 3. Emitir en tiempo real a monitores web (global y sala de tienda)
        const idtienda = data.idtienda ? String(data.idtienda) : "0";
        const salas = ['tienda_0'];
        if (idtienda !== "0") {
            salas.push(`tienda_${idtienda}`);
        }

        let emisor = io;
        for (const sala of salas) {
            emisor = emisor.to(sala);
        }
        emisor.emit('updateLocation', data);
    });

    socket.on('disconnect', (reason) => {
        const info = socket.datosDomiciliario;
        if (info) {
            const sesion = activeSessions.get(info.clave);
            if (sesion && sesion.socketId === socket.id) {
                activeSessions.delete(info.clave);
            }
        }
    });

    socket.on('update_token', (data) => {
        if (socket.isAuthorized && data.token && socket.datosDomiciliario?.clave) {
            const clave = socket.datosDomiciliario.clave;
            const deviceId = socket.datosDomiciliario.deviceId;
            if (authorizedDevices.get(clave) === deviceId) {
                tokensByUser[clave] = data.token;
                actualizarTokenEnBD(clave, data.token);
            }
        }
    });
});

// Chequeo de inactividad
setInterval(() => {
    const ahora = Date.now();
    Object.entries(lastSeenByUser).forEach(([clave, lastTime]) => {
        const diff = ahora - lastTime;
        if (diff > LIMITE_INACTIVIDAD) {
            const ultimoPush = lastPushSent[clave] || 0;
            if (ahora - ultimoPush > 5 * 60 * 1000) {
                if (tokensByUser[clave]) {
                    enviarPushReactivacion(clave);
                    lastPushSent[clave] = ahora;
                }
            }
        }
    });
}, 60 * 1000);

// Limpieza en memoria cada 24 horas
setInterval(() => {
    const ahora = Date.now();
    const LIMPIEZA_MAX = 24 * 60 * 60 * 1000;
    Object.keys(lastSeenByUser).forEach((clave) => {
        if (ahora - lastSeenByUser[clave] > LIMPIEZA_MAX) {
            delete lastSeenByUser[clave];
            delete lastPushSent[clave];
            activeSessions.delete(clave);
        }
    });
}, 60 * 60 * 1000);

process.on('SIGINT', () => {
    console.log("\n🛑 Apagando servidor limpiamente...");
    process.exit(0);
});

// Inicialización
cargarDispositivosDesdeBD();
connectToRabbitMQ();
checkPortAndStartServer(port);