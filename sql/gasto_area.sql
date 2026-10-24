-- =====================================================================
-- LOS GASTOS DE LAS AREAS QUE SE REPARTEN ENTRE LAS TIENDAS
-- Base de datos: pizzaamericana (el CENTRAL). Idempotente.
--
-- Hoy esto vive en el libro GASTOS_FIJOS_TIENDAS_JULIO.xlsx, en las pestanas
-- ADMINISTRATIVA, LOGISTICA Y PRODUCCION, CONTAC CENTER y PUBLICIDAD, y de ahi
-- alguien lo copia A MANO a la pestana PORCENTAJE. Ninguno de los valores de
-- esa pestana tiene formula: estan todos digitados.
--
-- La tabla nace para PUBLICIDAD, que es por donde se empieza, pero sirve igual
-- para las otras tres: las cuatro cuotas son el mismo calculo -costo del area
-- por la participacion de la tienda en la venta- y solo cambia el area.
--
-- DOS CLASES DE GASTO, y es la distincion que pidio Juan David:
--
--   'F' FIJO CON VIGENCIA. Vale lo mismo cada mes mientras este vigente. El
--       valor vive aqui, en valor_mes, y la vigencia dice desde cuando y hasta
--       cuando. Si vigencia_hasta es NULL, sigue corriendo.
--       Ejemplos: Cclick Action, Sebastian Botero.
--
--   'V' VARIABLE. Cambia mes a mes y alguien lo digita. El valor NO vive aqui
--       sino en gasto_area_mes, una fila por mes. valor_mes se queda en 0.
--       Ejemplo: Pautas y temas de publicidad.
--
-- POR QUE EL VARIABLE VA EN OTRA TABLA: si el valor del mes se guardara encima
-- del anterior, al recalcular un periodo viejo saldrian las cifras de hoy y no
-- las de entonces. Un gasto ya repartido entre las tiendas no puede cambiar
-- porque alguien digite el mes siguiente.
-- =====================================================================

CREATE TABLE IF NOT EXISTS gasto_area (
  idgasto        INT          NOT NULL AUTO_INCREMENT,
  -- PUBLICIDAD, ADMINISTRATIVA, LOGISTICA, CONTACT
  area           VARCHAR(20)  NOT NULL,
  concepto       VARCHAR(100) NOT NULL,
  -- 'F' fijo con vigencia, 'V' variable mes a mes
  tipo           CHAR(1)      NOT NULL DEFAULT 'F',
  -- Solo para los fijos. Los variables lo tienen en gasto_area_mes.
  valor_mes      DOUBLE       NOT NULL DEFAULT 0,
  vigencia_desde DATE         NOT NULL,
  -- NULL = sigue vigente, sin fecha de fin
  vigencia_hasta DATE         NULL,
  activo         CHAR(1)      NOT NULL DEFAULT 'S',
  usuario        VARCHAR(50)  NOT NULL DEFAULT '',
  creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (idgasto),
  KEY idx_gasto_area (area, activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gasto_area_mes (
  idgasto        INT         NOT NULL,
  anio           INT         NOT NULL,
  mes            INT         NOT NULL,
  valor          DOUBLE      NOT NULL DEFAULT 0,
  usuario        VARCHAR(50) NOT NULL DEFAULT '',
  actualizado_en DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (idgasto, anio, mes)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- Los tres gastos de publicidad que hoy estan en la pestana PUBLICIDAD.
--
-- OJO CON LAS FECHAS: vigencia_desde queda en el primer dia del mes en curso
-- porque el libro no dice desde cuando corre cada contrato. HAY QUE AJUSTARLO
-- en la pantalla con la fecha real, o los periodos viejos van a salir sin
-- estos gastos.
--
-- "Cclick Action" entra como un solo concepto por 5.287.100, que es lo que vale
-- hoy. En el Excel esa celda es "=2674700+2612400": si son dos contratos
-- distintos, conviene partirlo en dos renglones desde la pantalla.
-- ---------------------------------------------------------------------
INSERT INTO gasto_area (area, concepto, tipo, valor_mes, vigencia_desde, vigencia_hasta, activo, usuario)
SELECT 'PUBLICIDAD', 'Cclick Action', 'F', 5287100, DATE_FORMAT(CURDATE(), '%Y-%m-01'), NULL, 'S', 'migracion'
 WHERE NOT EXISTS (SELECT 1 FROM gasto_area WHERE area='PUBLICIDAD' AND concepto='Cclick Action');

INSERT INTO gasto_area (area, concepto, tipo, valor_mes, vigencia_desde, vigencia_hasta, activo, usuario)
SELECT 'PUBLICIDAD', 'Sebastian Botero', 'F', 1000000, DATE_FORMAT(CURDATE(), '%Y-%m-01'), NULL, 'S', 'migracion'
 WHERE NOT EXISTS (SELECT 1 FROM gasto_area WHERE area='PUBLICIDAD' AND concepto='Sebastian Botero');

INSERT INTO gasto_area (area, concepto, tipo, valor_mes, vigencia_desde, vigencia_hasta, activo, usuario)
SELECT 'PUBLICIDAD', 'Pautas y temas de publicidad', 'V', 0, DATE_FORMAT(CURDATE(), '%Y-%m-01'), NULL, 'S', 'migracion'
 WHERE NOT EXISTS (SELECT 1 FROM gasto_area WHERE area='PUBLICIDAD' AND concepto='Pautas y temas de publicidad');

-- El valor del mes en curso para el variable, como esta en el libro.
INSERT INTO gasto_area_mes (idgasto, anio, mes, valor, usuario)
SELECT g.idgasto, YEAR(CURDATE()), MONTH(CURDATE()), 3300000, 'migracion'
  FROM gasto_area g
 WHERE g.area='PUBLICIDAD' AND g.concepto='Pautas y temas de publicidad'
   AND NOT EXISTS (SELECT 1 FROM gasto_area_mes m
                    WHERE m.idgasto=g.idgasto AND m.anio=YEAR(CURDATE()) AND m.mes=MONTH(CURDATE()));

-- Verificacion: los tres conceptos y el valor del mes en curso.
SELECT g.idgasto, g.area, g.concepto, g.tipo,
       IF(g.tipo='F', g.valor_mes, IFNULL(m.valor, 0)) AS valor_del_mes,
       g.vigencia_desde, IFNULL(g.vigencia_hasta, '(sin fin)') AS vigencia_hasta, g.activo
  FROM gasto_area g
  LEFT JOIN gasto_area_mes m
         ON m.idgasto = g.idgasto AND m.anio = YEAR(CURDATE()) AND m.mes = MONTH(CURDATE())
 WHERE g.area = 'PUBLICIDAD'
 ORDER BY g.idgasto;
