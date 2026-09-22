-- ---------------------------------------------------------------------------
-- De cinco segmentos a nueve
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente. CORRIDO el 2026-09-22.
--
-- POR QUE
--
-- Los cinco de antes dejaban el 84% de los compradores en un solo grupo:
--
--     DORMIDO       264.360 personas    61,7% de las compras historicas
--     SIN PEDIDOS   135.385
--     EN RIESGO      22.400
--     ACTIVO         20.935
--     NUEVO           7.701
--
-- En DORMIDO cabia desde quien compro hace cuatro meses hasta quien compro hace
-- cinco anos. Cualquier campana que lo tomara como destinatario le escribia a
-- todos igual. Un segmento al que no se le puede escribir distinto no sirve
-- para segmentar.
--
-- LO QUE MOSTRARON LOS DATOS (medido el 2026-09-22)
--
-- 1. La frecuencia parte la base mucho mejor que la plata. Los 22.835 que han
--    pedido 10 veces o mas -7,2% de los compradores- valen el 42,7% de las
--    compras. Los 178.648 de un solo pedido valen el 16,7%.
--
-- 2. El ticket NO sirve para segmentar, y es al reves de lo que uno diria:
--    quien tiene ticket de 90 mil o mas promedia 1,9 pedidos, y quien esta
--    entre 40 y 60 mil promedia 3,6. El ticket alto no es un cliente premium,
--    es un pedido de grupo que se hizo una vez. Por eso NO hay un segmento de
--    ticket alto: juntaria a la gente equivocada.
--
-- 3. El canal si parte bien:
--       solo domicilio  179.313 personas   3,1 pedidos   90% autorizados
--       solo mostrador   96.415 personas   1,6 pedidos    3% autorizados
--       los dos          39.668 personas   8,2 pedidos   95% autorizados
--
-- 4. Comprar en mas de una tienda tambien marca: 2,7 pedidos promedio con una
--    tienda contra 7,3 con dos.
--
-- LOS NUEVE, Y CUANTA GENTE QUEDA EN CADA UNO
--
--     orden  segmento         personas  contactables   compras
--      10  SIN PEDIDOS        135.385      52.728           0
--      20  NUEVO                7.701       2.256     417 mill
--      30  ORO                  8.480       8.073   9.814 mill
--      40  FIEL                 5.253       4.476   1.900 mill
--      50  ACTIVO               7.202       4.597   1.085 mill
--      60  EN RIESGO           22.400      14.209   6.637 mill
--      70  POR RECUPERAR       15.749      14.736   8.614 mill
--      80  CASI PERDIDO        53.474      29.177   4.746 mill
--      90  PERDIDO            195.137     124.887  18.569 mill
--
-- El que hay que mirar es POR RECUPERAR: 15.749 personas que pedian seguido
-- -10,9 pedidos promedio- y llevan entre cuatro meses y un ano sin volver. El
-- 94% autorizo datos, o sea que se les puede escribir. Antes estaban enterradas
-- dentro de DORMIDO junto a 250 mil que no lo estan.
--
-- DORMIDO DESAPARECE COMO NOMBRE, Y ESO ES BUENO
--
-- No se quedo llamando DORMIDO con otro significado -"entre 121 dias y un
-- ano"-, sino que se renombro a CASI PERDIDO. Un nombre que sigue igual y por
-- dentro cambio es lo peor de los dos mundos: quien tuviera un filtro o un
-- informe apuntando a DORMIDO habria recibido 53 mil personas donde antes
-- recibia 264 mil, sin enterarse. Asi el que lo use se da cuenta de una.
--
-- LOS UMBRALES SIGUEN SIN CONFIRMAR CON MERCADEO
--
-- 60 dias para activo, 120 para en riesgo, 365 para casi perdido, 5 pedidos
-- para fiel y 10 para oro. Los tres primeros venian de antes; los dos ultimos
-- salen de mirar la distribucion, no de una decision de mercadeo. Si mercadeo
-- dice otra cosa se cambia desde la pantalla de Definicion de Segmentos y se
-- vuelve a clasificar: un minuto, no un despliegue.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LOS RENOMBRES, PRIMERO QUE TODO
--
-- Tienen que ir antes de los INSERT: si el nombre nuevo ya existiera, el
-- renombre chocaria contra la llave unica del nombre.
--
-- Se renombra la fila y no se crea una nueva a proposito. Creando una nueva
-- quedaria la vieja viva, activa y con su regla, peleandose la gente con la
-- nueva; y ademas se perderia el id, que es lo que amarra las reglas.
--
-- UPDATE IGNORE para poder correr esto dos veces: la segunda vez no hay nada
-- que renombrar y no pasa nada.
-- ===========================================================================

UPDATE IGNORE crm.segmento_definicion SET nombre = 'CASI PERDIDO' WHERE nombre = 'DORMIDO';
UPDATE IGNORE crm.segmento_definicion SET nombre = 'ORO'          WHERE nombre = 'CAMPEON';

-- ===========================================================================
-- 2. LAS DEFINICIONES
--
-- Se insertan por nombre y sin id: el nombre es unico, asi que correr esto dos
-- veces no crea duplicados ni pelea por un id que la pantalla pudo haber usado.
--
-- Los colores se escogieron para que se lean en blanco encima. El amarillo de
-- la marca -#FDC806- no sirve para ORO: con letra blanca encima no se lee, y
-- de letra sobre fondo claro tampoco. De ahi el dorado oscuro.
-- ===========================================================================

INSERT IGNORE INTO crm.segmento_definicion (nombre, descripcion, orden, color, activo) VALUES
 ('SIN PEDIDOS',   'Esta en la base pero nunca le hemos registrado una compra',   10, '#737B8A', 'S'),
 ('NUEVO',         'Compro por primera vez hace poco',                            20, '#1B4A9C', 'S'),
 ('ORO',           'Compra seguido y compro hace poco: lo mejor que tenemos',     30, '#97700F', 'S'),
 ('FIEL',          'Vuelve, y volvio hace poco',                                  40, '#16704F', 'S'),
 ('ACTIVO',        'Compro dentro de los ultimos 60 dias',                        50, '#267653', 'S'),
 ('EN RIESGO',     'Lleva entre 61 y 120 dias sin comprar',                       60, '#A85100', 'S'),
 ('POR RECUPERAR', 'Compraba seguido y lleva meses sin volver: vale perseguirlo', 70, '#B03A1A', 'S'),
 ('CASI PERDIDO',  'Lleva entre 121 dias y un ano sin comprar',                   80, '#41495A', 'S'),
 ('PERDIDO',       'Lleva mas de un ano sin comprar',                             90, '#6C7482', 'S');

-- Y se ajusta lo que ya existia, que INSERT IGNORE no toca.
UPDATE crm.segmento_definicion SET orden = 10, color = '#737B8A', activo = 'S',
       descripcion = 'Esta en la base pero nunca le hemos registrado una compra'
 WHERE nombre = 'SIN PEDIDOS';
UPDATE crm.segmento_definicion SET orden = 20, color = '#1B4A9C', activo = 'S',
       descripcion = 'Compro por primera vez hace poco'
 WHERE nombre = 'NUEVO';
UPDATE crm.segmento_definicion SET orden = 30, color = '#97700F', activo = 'S',
       descripcion = 'Compra seguido y compro hace poco: lo mejor que tenemos'
 WHERE nombre = 'ORO';
UPDATE crm.segmento_definicion SET orden = 50, color = '#267653', activo = 'S',
       descripcion = 'Compro dentro de los ultimos 60 dias'
 WHERE nombre = 'ACTIVO';
UPDATE crm.segmento_definicion SET orden = 60, color = '#A85100', activo = 'S',
       descripcion = 'Lleva entre 61 y 120 dias sin comprar'
 WHERE nombre = 'EN RIESGO';
UPDATE crm.segmento_definicion SET orden = 80, color = '#41495A', activo = 'S',
       descripcion = 'Lleva entre 121 dias y un ano sin comprar'
 WHERE nombre = 'CASI PERDIDO';

-- ===========================================================================
-- 3. LAS REGLAS
--
-- Se borran y se vuelven a poner enteras, en vez de ir comparando una por una:
-- son dos filas por segmento y asi correr esto dos veces da exactamente lo
-- mismo, sin quedar reglas viejas mezcladas con nuevas.
--
-- Las reglas de un mismo segmento se SUMAN: tienen que cumplirse todas.
-- ===========================================================================

DELETE FROM crm.segmento_regla
 WHERE idsegmento IN (SELECT idsegmento FROM crm.segmento_definicion
                       WHERE nombre IN ('SIN PEDIDOS','NUEVO','ORO','FIEL','ACTIVO',
                                        'EN RIESGO','POR RECUPERAR','CASI PERDIDO','PERDIDO'));

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '=', '0' FROM crm.segmento_definicion WHERE nombre='SIN PEDIDOS';

-- NUEVO va ANTES que ORO y FIEL, pero eso no le quita a nadie: quien lleva un
-- solo pedido no puede tener cinco. Va antes de ACTIVO, que si se lo llevaria
-- entero.
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '<=', '1' FROM crm.segmento_definicion WHERE nombre='NUEVO';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='NUEVO';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '>=', '10' FROM crm.segmento_definicion WHERE nombre='ORO';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='ORO';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '>=', '5' FROM crm.segmento_definicion WHERE nombre='FIEL';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='FIEL';

-- ACTIVO recoge a los demas que compraron hace poco. Por eso no lleva regla de
-- pedidos: los de muchos pedidos ya se los llevaron ORO y FIEL.
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '60' FROM crm.segmento_definicion WHERE nombre='ACTIVO';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '120' FROM crm.segmento_definicion WHERE nombre='EN RIESGO';

-- El importante. Va antes de CASI PERDIDO para sacarlo de ahi.
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'pedidos', '>=', '5' FROM crm.segmento_definicion WHERE nombre='POR RECUPERAR';
INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '365' FROM crm.segmento_definicion WHERE nombre='POR RECUPERAR';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '<=', '365' FROM crm.segmento_definicion WHERE nombre='CASI PERDIDO';

INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)
SELECT idsegmento, 'dias_sin_comprar', '>=', '366' FROM crm.segmento_definicion WHERE nombre='PERDIDO';

-- ===========================================================================
-- 4. COMO QUEDARON LAS DEFINICIONES
-- ===========================================================================

SELECT d.orden, d.nombre, d.activo, d.color,
       GROUP_CONCAT(CONCAT(r.campo,' ',r.operador,' ',r.valor) ORDER BY r.idregla SEPARATOR '  Y  ') AS reglas
  FROM crm.segmento_definicion d
  LEFT JOIN crm.segmento_regla r ON r.idsegmento = d.idsegmento
 GROUP BY d.idsegmento, d.orden, d.nombre, d.activo, d.color
 ORDER BY d.orden;

-- ===========================================================================
-- 5. Y AHORA SI, MOVER A LA GENTE (alrededor de un minuto)
-- ===========================================================================

CALL crm.pr_clasificar_segmentos('persona_resumen');

SELECT segmento, COUNT(*) AS personas,
       SUM(politica_datos='S') AS contactables,
       ROUND(SUM(valor)/1000000) AS millones
  FROM crm.persona_resumen GROUP BY segmento ORDER BY personas DESC;
