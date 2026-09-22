-- ---------------------------------------------------------------------------
-- Los segmentos dejan de estar escritos en el codigo
--
-- Se ejecuta en el CENTRAL (172.19.0.25). Idempotente.
--
-- QUE CAMBIA
--
-- Hoy los cinco segmentos -NUEVO, ACTIVO, EN RIESGO, DORMIDO, SIN PEDIDOS-
-- viven dentro de un CASE en crm.pr_recalcular_persona_resumen, con dos
-- umbrales en crm.parametro_segmento. Para crear un segmento nuevo -"los de
-- ticket alto", "los que solo compran en mostrador", "los de una tienda"- hay
-- que tocar el procedimiento.
--
-- Con estas tablas, un segmento es una fila con sus reglas. El recalculo
-- nocturno las lee y clasifica a cada persona. Crear uno nuevo deja de ser un
-- cambio de codigo.
--
-- GANA LA PRIMERA DEFINICION QUE CUMPLA, POR ORDEN
--
-- Una persona puede cumplir varias -quien tiene un solo pedido reciente es
-- NUEVO y tambien ACTIVO- y cada persona tiene que quedar en UNO. Por eso el
-- orden no es decorativo: es la regla de desempate, y es lo primero que hay que
-- entender al crear un segmento nuevo. Si se pone de ultimo detras de uno mas
-- amplio, nunca va a atrapar a nadie y va a parecer que no funciona.
--
-- LAS REGLAS DE UN MISMO SEGMENTO SE SUMAN (Y)
--
-- "pedidos >= 5" y "dias_sin_comprar <= 30" en el mismo segmento quiere decir
-- las dos cosas a la vez. Para un O, se crean dos segmentos con el mismo
-- nombre... o mejor, se piensa otra vez el criterio: un segmento que necesita
-- un O casi siempre son dos segmentos distintos.
--
-- EL CATALOGO DE CAMPOS ES LA LISTA BLANCA, Y NO SOBRA
--
-- El recalculo arma la condicion con el nombre del campo, asi que si ese nombre
-- viniera libre seria una inyeccion de SQL con permisos de root. Una regla cuyo
-- campo NO este en crm.segmento_campo se ignora: no se ejecuta y no rompe nada.
-- Esa tabla hace doble oficio, porque tambien es lo que llena la lista
-- desplegable de la pantalla.
--
-- LO SEMBRADO REPRODUCE EXACTAMENTE LO QUE HAY HOY, para que al prenderlo nadie
-- cambie de segmento. Despues se edita con calma.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LOS CAMPOS SOBRE LOS QUE SE PUEDE SEGMENTAR
-- ===========================================================================

CREATE TABLE IF NOT EXISTS crm.segmento_campo (
  campo     VARCHAR(30)  NOT NULL,
  etiqueta  VARCHAR(60)  NOT NULL,
  tipo      VARCHAR(10)  NOT NULL DEFAULT 'NUMERO',
  ayuda     VARCHAR(200) NULL,
  orden     INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (campo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3
  COMMENT='Lista blanca de columnas de persona_resumen que una regla puede usar.';

INSERT IGNORE INTO crm.segmento_campo (campo, etiqueta, tipo, ayuda, orden) VALUES
 ('pedidos',            'Pedidos en total',        'NUMERO', 'Domicilio mas mostrador', 10),
 ('pedidos_central',    'Pedidos a domicilio',     'NUMERO', 'Incluye virtual recoger', 20),
 ('pedidos_tienda',     'Pedidos en mostrador',    'NUMERO', 'Punto de venta y para llevar', 30),
 ('valor',              'Total comprado',          'NUMERO', 'En pesos', 40),
 ('ticket_promedio',    'Ticket promedio',         'NUMERO', 'En pesos', 50),
 ('dias_sin_comprar',   'Dias sin comprar',        'NUMERO', 'Desde el ultimo pedido', 60),
 ('dias_entre_pedidos', 'Dias entre pedidos',      'NUMERO', 'Cada cuanto vuelve', 70),
 ('tiendas_distintas',  'Tiendas donde compra',    'NUMERO', NULL, 80),
 ('tienda_habitual',    'Tienda habitual',         'NUMERO', 'El id de la tienda', 90),
 ('politica_datos',     'Autorizo datos',          'TEXTO',  'S o N', 100);

-- ===========================================================================
-- 2. LA DEFINICION DE CADA SEGMENTO
-- ===========================================================================

CREATE TABLE IF NOT EXISTS crm.segmento_definicion (
  idsegmento  INT          NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(20)  NOT NULL,
  descripcion VARCHAR(255) NULL,
  -- Menor numero, mayor prioridad. Es la regla de desempate.
  orden       INT          NOT NULL DEFAULT 100,
  -- Para pintarlo igual en la pantalla y en los correos.
  color       VARCHAR(7)   NULL,
  activo      CHAR(1)      NOT NULL DEFAULT 'S',
  PRIMARY KEY (idsegmento),
  UNIQUE KEY uk_segmento_nombre (nombre),
  KEY idx_segmento_orden (orden)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- ===========================================================================
-- 3. LAS REGLAS
-- ===========================================================================

CREATE TABLE IF NOT EXISTS crm.segmento_regla (
  idregla    INT         NOT NULL AUTO_INCREMENT,
  idsegmento INT         NOT NULL,
  campo      VARCHAR(30) NOT NULL,
  -- Solo estos seis. Cualquier otro se ignora al recalcular.
  operador   VARCHAR(2)  NOT NULL,
  valor      VARCHAR(40) NOT NULL,
  PRIMARY KEY (idregla),
  KEY idx_regla_segmento (idsegmento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- ===========================================================================
-- 4. LO QUE HAY HOY, TAL CUAL
--
-- El orden reproduce el CASE actual: primero los que no tienen pedidos,
-- despues NUEVO -que es ACTIVO con un solo pedido- y solo entonces ACTIVO.
-- Invertir esos dos dejaria a NUEVO sin nadie.
-- ===========================================================================

INSERT IGNORE INTO crm.segmento_definicion (idsegmento, nombre, descripcion, orden, color, activo) VALUES
 (1, 'SIN PEDIDOS', 'Esta en la base pero nunca le hemos registrado una compra', 10, '#8A919E', 'S'),
 (2, 'NUEVO',       'Compro por primera vez hace poco', 20, '#1B4A9C', 'S'),
 (3, 'ACTIVO',      'Compro dentro de los ultimos 60 dias', 30, '#16704F', 'S'),
 (4, 'EN RIESGO',   'Lleva entre 61 y 120 dias sin comprar', 40, '#8A6200', 'S'),
 (5, 'DORMIDO',     'Lleva mas de 120 dias sin comprar', 50, '#41495A', 'S');

INSERT IGNORE INTO crm.segmento_regla (idregla, idsegmento, campo, operador, valor) VALUES
 (1, 1, 'pedidos',          '=',  '0'),
 (2, 2, 'pedidos',          '<=', '1'),
 (3, 2, 'dias_sin_comprar', '<=', '60'),
 (4, 3, 'dias_sin_comprar', '<=', '60'),
 (5, 4, 'dias_sin_comprar', '<=', '120'),
 (6, 5, 'dias_sin_comprar', '>=', '121');

-- Como quedo
SELECT d.orden, d.nombre, d.activo,
       GROUP_CONCAT(CONCAT(r.campo,' ',r.operador,' ',r.valor) ORDER BY r.idregla SEPARATOR '  Y  ') AS reglas
  FROM crm.segmento_definicion d
  LEFT JOIN crm.segmento_regla r ON r.idsegmento = d.idsegmento
 GROUP BY d.idsegmento, d.orden, d.nombre, d.activo
 ORDER BY d.orden;
