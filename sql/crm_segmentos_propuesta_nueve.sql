-- ---------------------------------------------------------------------------
-- PROPUESTA: pasar de cinco segmentos a nueve
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente. NO SE HA CORRIDO.
--
-- ESTO NO ES UN ARREGLO, ES UNA DECISION DE MERCADEO
--
-- Cambia como quedan clasificadas 450 mil personas y cambia lo que significan
-- dos nombres que ya se estan usando. Se deja escrito para que se pueda mirar y
-- discutir; correrlo es de quien decide, no del que lo escribio.
--
-- POR QUE SE PROPONE
--
-- Los cinco de hoy dejan el 84% de los compradores en un solo grupo:
--
--     DORMIDO       264.360 personas    61,7% de las compras historicas
--     SIN PEDIDOS   135.385
--     EN RIESGO      22.400
--     ACTIVO         20.935
--     NUEVO           7.701
--
-- En DORMIDO cabe desde quien compro hace cuatro meses hasta quien compro hace
-- cinco anos. Son 264 mil personas: cualquier campana que lo tome como
-- destinatario le escribe a todos igual, y no hay forma de separar al que vale
-- la pena perseguir del que ya no existe. Un segmento al que no se le puede
-- escribir distinto no sirve para segmentar.
--
-- LO QUE MUESTRAN LOS DATOS (medido el 2026-09-22)
--
-- 1. La frecuencia parte la base mucho mejor que la plata. Los 22.835 que han
--    pedido 10 veces o mas -7,2% de los compradores- valen el 42,7% de las
--    compras. Los 178.648 de un solo pedido valen el 16,7%.
--
-- 2. El ticket NO sirve para segmentar, y es al reves de lo que uno diria:
--    quien tiene ticket de 90 mil o mas promedia 1,9 pedidos, y quien esta
--    entre 40 y 60 mil promedia 3,6. El ticket alto no es un cliente premium,
--    es un pedido de grupo que se hizo una vez. Un segmento "de ticket alto"
--    juntaria a la gente equivocada.
--
-- 3. El canal si parte bien:
--       solo domicilio  179.313 personas   3,1 pedidos   90% autorizados
--       solo mostrador   96.415 personas   1,6 pedidos    3% autorizados
--       los dos          39.668 personas   8,2 pedidos   95% autorizados
--    Quien compra por los dos lados es el 12,6% de los compradores y el 31% de
--    las compras. Y al de mostrador casi no se le puede escribir, que es otro
--    problema y no se arregla con segmentos.
--
-- 4. Comprar en mas de una tienda tambien marca: 2,7 pedidos promedio con una
--    tienda contra 7,3 con dos.
--
-- LOS NUEVE, Y CUANTA GENTE QUEDA EN CADA UNO
--
--     orden  segmento         personas  contactables   compras
--      10  SIN PEDIDOS        135.385      52.728           0
--      20  NUEVO                7.701       2.256     417 mill
--      30  CAMPEON              8.480       8.073   9.814 mill
--      40  FIEL                 5.253       4.476   1.900 mill
--      50  ACTIVO               7.202       4.597   1.085 mill
--      60  EN RIESGO           22.400      14.209   6.637 mill
--      70  POR RECUPERAR       15.749      14.736   8.614 mill
--      80  DORMIDO             53.474      29.177   4.746 mill
--      90  PERDIDO            195.137     124.887  18.569 mill
--
-- El que hay que mirar es POR RECUPERAR: 15.749 personas que pedian seguido
-- -10,9 pedidos promedio- y llevan entre cuatro meses y un ano sin volver. El
-- 94% autorizo datos, o sea que se les puede escribir. Hoy estan enterradas
-- dentro de DORMIDO junto a 250 mil que no lo estan.
--
-- DOS NOMBRES CAMBIAN DE SIGNIFICADO
--
-- DORMIDO deja de ser "mas de 120 dias" y pasa a ser "entre 121 dias y un
-- ano". Lo de mas de un ano se llama ahora PERDIDO. Quien tenga un filtro, un
-- informe o una campana guardada apuntando a DORMIDO va a recibir 53 mil
-- personas donde antes recibia 264 mil. Eso es lo que se busca, pero hay que
-- saberlo antes y no despues.
--
-- LOS UMBRALES SIGUEN SIN CONFIRMAR
--
-- 60 dias para activo, 120 para en riesgo, 365 para dormido, 5 pedidos para
-- fiel y 10 para campeon. Los tres primeros venian de antes y los dos ultimos
-- salen de mirar la distribucion, no de una decision de mercadeo. Si mercadeo
-- dice otra cosa, se cambia desde la pantalla y se vuelve a clasificar: son 55
-- segundos, no un despliegue.
--
-- DESPUES DE CORRER ESTO HAY QUE CLASIFICAR
--
-- Esto solo cambia las definiciones. La gente no se mueve hasta que corra
--     CALL crm.pr_clasificar_segmentos('persona_resumen');
-- que esta al final, o hasta el proceso de la noche.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LAS DEFINICIONES
--
-- Se insertan por nombre y sin id: el nombre es unico y asi correrlo dos veces
-- no crea duplicados ni pelea por un id que la pantalla pudo haber usado ya.
-- ===========================================================================

INSERT IGNORE INTO crm.segmento_definicion (nombre, descripcion, orden, color, activo) VALUES
 ('SIN PEDIDOS',   'Esta en la base pero nunca le hemos registrado una compra', 10, '#8A919E', 'S'),
 ('NUEVO',         'Compro por primera vez hace poco',                          20, '#1B4A9C', 'S'),
 ('CAMPEON',       'Compra seguido y compro hace poco: lo mejor que tenemos',   30, '#0F5138', 'S'),
 ('FIEL',          'Vuelve, y volvio hace poco',                                40, '#16704F', 'S'),
 ('ACTIVO',        'Compro dentro de los ultimos 60 dias',                      50, '#2E8B63', 'S'),
 ('EN RIESGO',     'Lleva entre 61 y 120 dias sin comprar',                     60, '#8A6200', 'S'),
 ('POR RECUPERAR', 'Compraba seguido y lleva meses sin volver: vale perseguirlo', 70, '#C25E00', 'S'),
 ('DORMIDO',       'Lleva entre 121 dias y un ano sin comprar',                 80, '#41495A', 'S'),
 ('PERDIDO',       'Lleva mas de un ano sin comprar',                           90, '#6C7482', 'S');

-- Y se ajusta lo que ya existia, que INSERT IGNORE no toca.
UPDATE crm.segmento_definicion SET orden = 10, color = '#8A919E', activo = 'S',
       descripcion = 'Esta en la base pero nunca le hemos registrado una compra'
 WHERE nombre = 'SIN PEDIDOS';
UPDATE crm.segmento_definicion SET orden = 20, color = '#1B4A9C', activo = 'S',
       descripcion = 'Compro por primera vez hace poco'
 WHERE nombre = 'NUEVO';
UPDATE crm.segmento_definicion SET orden = 50, color = '#2E8B63', activo = 'S',
       descripcion = 'Compro dentro de los ultimos 60 dias'
 WHERE nombre = 'ACTIVO';
UPDATE crm.segmento_definicion SET orden = 60, color = '#8A6200', activo = 'S',
       descripcion = 'Lleva entre 61 y 120 dias sin comprar'
 WHERE nombre = 'EN RIESGO';
UPDATE crm.segmento_definicion SET orden = 80, color = '#41495A', activo = 'S',
       descripcion = 'Lleva entre 121 dias y un ano sin comprar'
 WHERE nombre = 'DORMIDO';

-- ===========================================================================
-- 2. LAS REGLAS
--
-- Se borran y se vuelven a poner enteras, en vez de ir comparando una por una:
-- son dos filas por segmento y asi correr esto dos veces da exactamente lo
-- mismo, sin quedar reglas viejas mezcladas con nuevas.
--
-- Las reglas de un mismo segmento se SUMAN: tienen que cumplirse todas.
-- ===========================================================================

DELETE FROM crm.segmento_regla
 WHERE idsegmento IN (SELECT idsegmento FROM crm.segmento_definicion
                       WHERE nombre IN ('SIN PEDIDOS','NUEVO','CAMPEON','FIEL','ACTIVO',
                                        'EN RIESGO','POR RECUPERAR','DORMIDO','PERDIDO'));

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '=', '0' FROM crm.segmento_definicion WHERE nombre='SIN PEDIDOS';

-- NUEVO va ANTES que CAMPEON y FIEL, pero eso no le quita a nadie: quien lleva
-- un solo pedido no puede tener cinco. Va antes de ACTIVO, que si se lo
-- llevaria entero.
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '<=', '1' FROM crm.segmento_definicion WHERE nombre='NUEVO';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='NUEVO';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '>=', '10' FROM crm.segmento_definicion WHERE nombre='CAMPEON';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='CAMPEON';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '>=', '5' FROM crm.segmento_definicion WHERE nombre='FIEL';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='FIEL';

-- ACTIVO recoge a los demas que compraron hace poco. Por eso no lleva regla de
-- pedidos: los de muchos pedidos ya se los llevaron CAMPEON y FIEL.
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='ACTIVO';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '120' FROM crm.segmento_definicion WHERE nombre='EN RIESGO';

-- El importante. Va antes de DORMIDO para sacarlo de ahi.
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '>=', '5' FROM crm.segmento_definicion WHERE nombre='POR RECUPERAR';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '365' FROM crm.segmento_definicion WHERE nombre='POR RECUPERAR';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '365' FROM crm.segmento_definicion WHERE nombre='DORMIDO';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '>=', '366' FROM crm.segmento_definicion WHERE nombre='PERDIDO';

-- ===========================================================================
-- 3. COMO QUEDARON LAS DEFINICIONES
-- ===========================================================================

SELECT d.orden, d.nombre, d.activo,
       GROUP_CONCAT(CONCAT(r.campo,' ',r.operador,' ',r.valor) ORDER BY r.idregla SEPARATOR '  Y  ') AS reglas
  FROM crm.segmento_definicion d
  LEFT JOIN crm.segmento_regla r ON r.idsegmento = d.idsegmento
 GROUP BY d.idsegmento, d.orden, d.nombre, d.activo
 ORDER BY d.orden;

-- ===========================================================================
-- 4. Y AHORA SI, MOVER A LA GENTE (alrededor de un minuto)
-- ===========================================================================

CALL crm.pr_clasificar_segmentos('persona_resumen');

SELECT segmento, COUNT(*) AS personas,
       SUM(politica_datos='S') AS contactables,
       ROUND(SUM(valor)/1000000) AS millones
  FROM crm.persona_resumen GROUP BY segmento ORDER BY personas DESC;
