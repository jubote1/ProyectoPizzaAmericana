-- ---------------------------------------------------------------------------
-- GUSTOS DE CLIENTE (HINCHA NACIONAL / HINCHA MEDELLIN) Y CALENDARIO DE PARTIDOS
--
-- Idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
--
-- QUE HACE Y POR QUE
--
-- Juan David quiere dirigir la publicidad del Combo Futbolero segun de que equipo es
-- hincha el cliente. La forma de saberlo: si compro Futbolero un dia en que SOLO jugaba
-- Nacional, es hincha de Nacional; si solo jugaba Medellin, de Medellin. En clasico no
-- se marca a nadie, porque no se sabe.
--
-- 1. UNA MARCA QUE NO ES UNA ALERTA
--
-- Las marcas de cliente que ya existen (No desea Rappi Cargo, Posible fraude) le salen
-- al cajero como AVISO en el POS. Un gusto de futbol no puede salir ahi: no es una
-- advertencia y solo distraeria a quien esta tomando el pedido. Por eso se agrega la
-- columna es_gusto: las marcas con es_gusto = 'S' viven solo en el CRM y en la
-- segmentacion de publicidad, y el POS no las muestra.
--
-- 2. EL CALENDARIO DE PARTIDOS
--
-- Va en una tabla y no en el codigo porque hay partidos cada semana: mercadeo la
-- mantiene al dia sin que nadie tenga que desplegar nada.
--
-- UNA FILA POR EQUIPO Y POR FECHA. Un clasico son DOS filas con la misma fecha, una de
-- cada equipo; asi el proceso que marca no necesita saber que es un clasico: si una
-- fecha tiene los dos equipos, no marca a nadie. Es mas dificil equivocarse.
--
-- La columna "confirmado" existe porque el calendario se puede ir llenando con fechas
-- que todavia no estan verificadas: el proceso de marcado SOLO usa las confirmadas.
-- Marcar mal a un cliente -decirle hincha del Medellin a un hincha del Nacional- es
-- peor que no marcarlo.
--
-- PARA DEVOLVERLO
--
--   DELETE FROM pizzaamericana.cliente_marca WHERE idtipo IN ('HINCHA_NACIONAL','HINCHA_MEDELLIN');
--   DELETE FROM pizzaamericana.cliente_marca_tipo WHERE idtipo IN ('HINCHA_NACIONAL','HINCHA_MEDELLIN');
--   ALTER TABLE pizzaamericana.cliente_marca_tipo DROP COLUMN es_gusto;
--   DROP TABLE pizzaamericana.partido_futbol;
-- ---------------------------------------------------------------------------

USE pizzaamericana;

-- 1. La columna que separa un gusto de una alerta.
SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'cliente_marca_tipo'
                  AND COLUMN_NAME = 'es_gusto');
SET @sql = IF(@existe = 0,
        'ALTER TABLE cliente_marca_tipo ADD COLUMN es_gusto CHAR(1) NOT NULL DEFAULT ''N'' COMMENT ''S = es un gusto del cliente, no una alerta: no se muestra en el POS''',
        'SELECT ''la columna es_gusto ya existia''');
PREPARE p FROM @sql; EXECUTE p; DEALLOCATE PREPARE p;

-- 2. Las dos marcas de hinchada.
INSERT INTO cliente_marca_tipo (idtipo, nombre, descripcion, severidad, bloquea_cargo, activo, orden, es_gusto)
SELECT 'HINCHA_NACIONAL', 'Hincha de Nacional',
       'Compro el Combo Futbolero un dia en que jugaba unicamente Atletico Nacional. Sirve para dirigirle la publicidad del combo. No es una alerta y no se muestra en el POS.',
       'I', 'N', 'S', 50, 'S'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cliente_marca_tipo WHERE idtipo = 'HINCHA_NACIONAL');

INSERT INTO cliente_marca_tipo (idtipo, nombre, descripcion, severidad, bloquea_cargo, activo, orden, es_gusto)
SELECT 'HINCHA_MEDELLIN', 'Hincha del Medellin',
       'Compro el Combo Futbolero un dia en que jugaba unicamente Independiente Medellin. Sirve para dirigirle la publicidad del combo. No es una alerta y no se muestra en el POS.',
       'I', 'N', 'S', 51, 'S'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cliente_marca_tipo WHERE idtipo = 'HINCHA_MEDELLIN');

-- 3. El calendario. Una fila por equipo y fecha; un clasico son dos filas.
CREATE TABLE IF NOT EXISTS partido_futbol (
  idpartido   INT AUTO_INCREMENT PRIMARY KEY,
  fecha       DATE         NOT NULL,
  equipo      VARCHAR(20)  NOT NULL COMMENT 'NACIONAL o MEDELLIN',
  rival       VARCHAR(60)  NOT NULL DEFAULT '',
  torneo      VARCHAR(60)  NOT NULL DEFAULT '',
  confirmado  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT 'S = fecha verificada; el marcado solo usa las confirmadas',
  fuente      VARCHAR(120) NOT NULL DEFAULT '' COMMENT 'de donde salio la fecha',
  usuario     VARCHAR(50)  NOT NULL DEFAULT '',
  fecha_registro DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_fecha_equipo (fecha, equipo),
  KEY ix_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Dias en que jugo Nacional o Medellin, para deducir de que equipo es hincha un cliente';

-- Comprobacion.
SELECT idtipo, nombre, es_gusto, activo FROM cliente_marca_tipo ORDER BY orden;
SELECT COUNT(*) AS partidos_cargados FROM partido_futbol;
