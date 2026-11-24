-- ============================================================================
-- 2026_11_24_01_correos_dominio_fidelizacion.sql
--
-- Corrige los dominios mal digitados en las tablas de FIDELIZACION.
--
-- La correccion del 2026-11-12 (2026_11_12_01_correos_dominio_typo.sql) arreglo
-- crm.persona, pero NO estas tablas, que son de donde sale el correo nocturno de
-- "tus puntos vencen". Por eso siguen rebotando contra la cuenta de Gmail todas
-- las noches: el rebote que Juan David mostro el 2026-11-24 -un "outloock.com",
-- con dos o- es uno de esos. Autorizado por el ese mismo dia.
--
-- ALCANCE MEDIDO: 1.010 filas en cinco tablas.
--   cliente_fidelizacion             341 cuentas -> 339 cuentas buenas
--   fidelizacion_transaccion         639
--   fidelizacion_redencion_detalle    18
--   codigo_redencion_puntos           10
--   fidelizacion_redencion             2
--   cliente_no_fidelizacion            0
--   fidelizacion_ajuste_manual         0
-- Son 23.279 puntos que hoy estan colgados de un correo que rebota.
--
-- ---------------------------------------------------------------------------
-- POR QUE TODO SE CONSOLIDA Y NADA SE "RENOMBRA"
-- ---------------------------------------------------------------------------
-- cliente_fidelizacion tiene el correo como LLAVE PRIMARIA, asi que renombrar
-- choca cuando el correo corregido ya existe. La primera version de este script
-- separaba "renombrar" (273) de "fusionar" (68) y tenia DOS defectos, los dos
-- encontrados al medir antes de correrlo:
--
--   1. Hay DOS personas con el mismo correo bueno escrito mal de dos maneras
--      distintas (por ejemplo "hotamail" y "hotamil", que corrigen las dos a
--      hotmail.com). Un UPDATE con JOIN por correo_nuevo aplica UNA sola de las
--      dos filas y la otra se pierde en silencio: a una de esas personas se le
--      habrian borrado 266 o 768 puntos sin que nadie se enterara.
--   2. En uno de esos dos casos el correo bueno NO existe, asi que las dos
--      filas se habrian querido renombrar al mismo destino: llave duplicada y la
--      migracion muerta a media marcha.
--
-- Por eso ahora hay un solo camino: se suman los puntos POR CORREO BUENO, se
-- crea la cuenta buena si no existe, y se borran todas las del typo. El caso de
-- uno a uno y el de dos a uno se resuelven con el mismo codigo.
--
-- EN LAS OTRAS CUATRO TABLAS NO HAY CHOQUE POSIBLE: fidelizacion_transaccion se
-- verifico (ninguna de las 639 cae sobre una (correo, idtienda, idpedidotienda)
-- existente) y las otras tres tienen llave sustituta propia.
--
-- COMO SE HACE SIN BLOQUEAR NADA: primero se materializan las llaves en tablas
-- de trabajo y despues se actualiza POR LLAVE. Nunca un UPDATE con
-- SUBSTRING_INDEX en el JOIN sobre una tabla viva: eso fue lo que dejo la tabla
-- cliente bloqueada seis minutos el 2026-09-15. fidelizacion_transaccion tiene
-- 57.472 filas y se escribe cada noche.
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Respaldo de todo lo que se va a tocar
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm.respaldo_fidelizacion_dominio_20261124 (
    tabla         VARCHAR(40)  NOT NULL,
    llave         VARCHAR(120) NOT NULL,
    correo_antes  VARCHAR(100) NOT NULL,
    correo_nuevo  VARCHAR(100) NOT NULL,
    extra         VARCHAR(160) DEFAULT NULL,
    guardado_en   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resp_tabla (tabla),
    INDEX idx_resp_correo (correo_antes)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 2. El mapa de trabajo: correo malo -> correo bueno
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS crm.tmp_correo_fidelizacion;
CREATE TABLE crm.tmp_correo_fidelizacion (
    correo_viejo  VARCHAR(100) NOT NULL,
    correo_nuevo  VARCHAR(100) NOT NULL,
    puntos_viejos DOUBLE       NOT NULL DEFAULT 0,
    fecha_vieja   TIMESTAMP    NULL DEFAULT NULL,
    activo_viejo  CHAR(1)      DEFAULT NULL,
    PRIMARY KEY (correo_viejo),
    INDEX idx_tmp_nuevo (correo_nuevo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- El mapa se arma desde las cinco tablas, no solo del censo: hay correos que
-- aparecen en transacciones o en redenciones y no en cliente_fidelizacion.
INSERT IGNORE INTO crm.tmp_correo_fidelizacion (correo_viejo, correo_nuevo)
SELECT cf.correo, CONCAT(SUBSTRING_INDEX(cf.correo,'@',1), '@', d.bueno)
  FROM pizzaamericana.cliente_fidelizacion cf
  JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(cf.correo,'@',-1);

INSERT IGNORE INTO crm.tmp_correo_fidelizacion (correo_viejo, correo_nuevo)
SELECT t.correo, CONCAT(SUBSTRING_INDEX(t.correo,'@',1), '@', d.bueno)
  FROM pizzaamericana.fidelizacion_transaccion t
  JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(t.correo,'@',-1)
 GROUP BY t.correo, d.bueno;

INSERT IGNORE INTO crm.tmp_correo_fidelizacion (correo_viejo, correo_nuevo)
SELECT r.correo, CONCAT(SUBSTRING_INDEX(r.correo,'@',1), '@', d.bueno)
  FROM pizzaamericana.fidelizacion_redencion r
  JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(r.correo,'@',-1)
 GROUP BY r.correo, d.bueno;

INSERT IGNORE INTO crm.tmp_correo_fidelizacion (correo_viejo, correo_nuevo)
SELECT dt.correo, CONCAT(SUBSTRING_INDEX(dt.correo,'@',1), '@', d.bueno)
  FROM pizzaamericana.fidelizacion_redencion_detalle dt
  JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(dt.correo,'@',-1)
 GROUP BY dt.correo, d.bueno;

INSERT IGNORE INTO crm.tmp_correo_fidelizacion (correo_viejo, correo_nuevo)
SELECT c.correo, CONCAT(SUBSTRING_INDEX(c.correo,'@',1), '@', d.bueno)
  FROM pizzaamericana.codigo_redencion_puntos c
  JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(c.correo,'@',-1)
 GROUP BY c.correo, d.bueno;

-- Lo que trae cada cuenta del typo, materializado para no hacer auto-JOIN.
UPDATE crm.tmp_correo_fidelizacion m
  JOIN pizzaamericana.cliente_fidelizacion a ON a.correo = m.correo_viejo
   SET m.puntos_viejos = a.puntos_vigentes,
       m.fecha_vieja   = a.fecha_vinculacion,
       m.activo_viejo  = a.activo;

-- Y el acumulado POR CORREO BUENO. Esta es la tabla que evita el defecto 1:
-- si dos typos caen en el mismo correo, aqui sus puntos se suman en una fila.
DROP TABLE IF EXISTS crm.tmp_destino_fidelizacion;
CREATE TABLE crm.tmp_destino_fidelizacion (
    correo_nuevo   VARCHAR(100) NOT NULL,
    puntos_sumados DOUBLE       NOT NULL DEFAULT 0,
    fecha_antigua  TIMESTAMP    NULL DEFAULT NULL,
    algun_activo   CHAR(1)      NOT NULL DEFAULT 'N',
    cuantos_typos  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (correo_nuevo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO crm.tmp_destino_fidelizacion (correo_nuevo, puntos_sumados, fecha_antigua, algun_activo, cuantos_typos)
SELECT m.correo_nuevo, SUM(m.puntos_viejos), MIN(m.fecha_vieja),
       IF(SUM(m.activo_viejo = 'S') > 0, 'S', 'N'), COUNT(*)
  FROM crm.tmp_correo_fidelizacion m
 GROUP BY m.correo_nuevo;

-- Constancia de todo, antes de tocar una sola fila.
INSERT INTO crm.respaldo_fidelizacion_dominio_20261124 (tabla, llave, correo_antes, correo_nuevo, extra)
SELECT 'cliente_fidelizacion', cf.correo, cf.correo, m.correo_nuevo,
       CONCAT('puntos=', cf.puntos_vigentes, ' activo=', cf.activo, ' vinculado=', cf.fecha_vinculacion)
  FROM pizzaamericana.cliente_fidelizacion cf
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = cf.correo
 WHERE NOT EXISTS (SELECT 1 FROM crm.respaldo_fidelizacion_dominio_20261124 r
                    WHERE r.tabla='cliente_fidelizacion' AND r.llave = cf.correo);

INSERT INTO crm.respaldo_fidelizacion_dominio_20261124 (tabla, llave, correo_antes, correo_nuevo, extra)
SELECT 'fidelizacion_transaccion',
       CONCAT(t.correo,'|',t.idtienda,'|',t.idpedidotienda), t.correo, m.correo_nuevo,
       CONCAT('puntos=', t.puntos)
  FROM pizzaamericana.fidelizacion_transaccion t
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = t.correo
 WHERE NOT EXISTS (SELECT 1 FROM crm.respaldo_fidelizacion_dominio_20261124 r
                    WHERE r.tabla='fidelizacion_transaccion'
                      AND r.llave = CONCAT(t.correo,'|',t.idtienda,'|',t.idpedidotienda));

INSERT INTO crm.respaldo_fidelizacion_dominio_20261124 (tabla, llave, correo_antes, correo_nuevo)
SELECT 'fidelizacion_redencion', r.idredencion, r.correo, m.correo_nuevo
  FROM pizzaamericana.fidelizacion_redencion r
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = r.correo
 WHERE NOT EXISTS (SELECT 1 FROM crm.respaldo_fidelizacion_dominio_20261124 z
                    WHERE z.tabla='fidelizacion_redencion' AND z.llave = r.idredencion);

INSERT INTO crm.respaldo_fidelizacion_dominio_20261124 (tabla, llave, correo_antes, correo_nuevo)
SELECT 'fidelizacion_redencion_detalle', dt.iddetalle, dt.correo, m.correo_nuevo
  FROM pizzaamericana.fidelizacion_redencion_detalle dt
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = dt.correo
 WHERE NOT EXISTS (SELECT 1 FROM crm.respaldo_fidelizacion_dominio_20261124 z
                    WHERE z.tabla='fidelizacion_redencion_detalle' AND z.llave = dt.iddetalle);

INSERT INTO crm.respaldo_fidelizacion_dominio_20261124 (tabla, llave, correo_antes, correo_nuevo)
SELECT 'codigo_redencion_puntos', c.idcodigo, c.correo, m.correo_nuevo
  FROM pizzaamericana.codigo_redencion_puntos c
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = c.correo
 WHERE NOT EXISTS (SELECT 1 FROM crm.respaldo_fidelizacion_dominio_20261124 z
                    WHERE z.tabla='codigo_redencion_puntos' AND z.llave = c.idcodigo);

-- ---------------------------------------------------------------------------
-- 3. Las tablas con llave sustituta: renombrar y ya
-- ---------------------------------------------------------------------------
UPDATE pizzaamericana.fidelizacion_redencion r
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = r.correo
   SET r.correo = m.correo_nuevo;

UPDATE pizzaamericana.fidelizacion_redencion_detalle dt
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = dt.correo
   SET dt.correo = m.correo_nuevo;

UPDATE pizzaamericana.codigo_redencion_puntos c
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = c.correo
   SET c.correo = m.correo_nuevo;

-- ---------------------------------------------------------------------------
-- 4. Las transacciones, que son las que de verdad mandan los puntos
-- ---------------------------------------------------------------------------
-- Van ANTES de tocar el censo: asi, pase lo que pase despues, los puntos ya
-- quedaron colgados del correo bueno. El calculo de puntos por vencer sale de
-- aqui, no de cliente_fidelizacion.puntos_vigentes, que es solo una cache.
UPDATE pizzaamericana.fidelizacion_transaccion t
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = t.correo
   SET t.correo = m.correo_nuevo;

-- ---------------------------------------------------------------------------
-- 5. El censo de cuentas: crear la buena, sumarle, y borrar las del typo
-- ---------------------------------------------------------------------------
-- 5a. La cuenta buena que todavia no existe se crea en cero: el UPDATE de 5b le
--     suma. Un solo camino para los dos casos.
INSERT IGNORE INTO pizzaamericana.cliente_fidelizacion
       (correo, fecha_vinculacion, activo, puntos_vigentes, canal)
SELECT dnew.correo_nuevo, IFNULL(dnew.fecha_antigua, NOW()), dnew.algun_activo, 0, ''
  FROM crm.tmp_destino_fidelizacion dnew;

-- 5b. Sumar los puntos del typo a la cuenta buena.
UPDATE pizzaamericana.cliente_fidelizacion b
  JOIN crm.tmp_destino_fidelizacion dnew ON dnew.correo_nuevo = b.correo
   SET b.puntos_vigentes   = b.puntos_vigentes + dnew.puntos_sumados,
       b.fecha_vinculacion = LEAST(b.fecha_vinculacion, IFNULL(dnew.fecha_antigua, b.fecha_vinculacion)),
       b.activo            = IF(b.activo = 'S' OR dnew.algun_activo = 'S', 'S', b.activo);

-- 5c. Fuera las cuentas del typo.
DELETE a FROM pizzaamericana.cliente_fidelizacion a
  JOIN crm.tmp_correo_fidelizacion m ON m.correo_viejo = a.correo;

-- ---------------------------------------------------------------------------
-- 6. Comprobacion
-- ---------------------------------------------------------------------------
SELECT 'cliente_fidelizacion' AS tabla, COUNT(*) AS quedan_con_dominio_malo
  FROM pizzaamericana.cliente_fidelizacion a JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(a.correo,'@',-1)
UNION ALL SELECT 'fidelizacion_transaccion', COUNT(*)
  FROM pizzaamericana.fidelizacion_transaccion a JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(a.correo,'@',-1)
UNION ALL SELECT 'fidelizacion_redencion', COUNT(*)
  FROM pizzaamericana.fidelizacion_redencion a JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(a.correo,'@',-1)
UNION ALL SELECT 'fidelizacion_redencion_detalle', COUNT(*)
  FROM pizzaamericana.fidelizacion_redencion_detalle a JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(a.correo,'@',-1)
UNION ALL SELECT 'codigo_redencion_puntos', COUNT(*)
  FROM pizzaamericana.codigo_redencion_puntos a JOIN crm.dominio_correccion d ON d.malo = SUBSTRING_INDEX(a.correo,'@',-1);

-- Ni un punto se puede haber perdido: lo que se sumo debe ser lo que habia.
SELECT ROUND(SUM(puntos_sumados),1) AS puntos_consolidados,
       COUNT(*)                     AS cuentas_buenas,
       SUM(cuantos_typos)           AS cuentas_del_typo_absorbidas,
       SUM(cuantos_typos > 1)       AS casos_de_dos_typos_al_mismo_correo
  FROM crm.tmp_destino_fidelizacion;

SELECT COUNT(*) AS filas_respaldadas FROM crm.respaldo_fidelizacion_dominio_20261124;

-- Que no haya quedado una transaccion colgando de una cuenta que no existe.
SELECT COUNT(DISTINCT t.correo) AS transacciones_sin_cuenta
  FROM pizzaamericana.fidelizacion_transaccion t
  JOIN crm.tmp_destino_fidelizacion dnew ON dnew.correo_nuevo = t.correo
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.cliente_fidelizacion cf WHERE cf.correo = t.correo);

-- Los casos de dos typos al mismo correo, para mirarlos a ojo. No se escriben
-- correos de clientes en el repositorio: salen de la tabla de trabajo.
SELECT b.puntos_vigentes, b.activo, b.fecha_vinculacion, dnew.cuantos_typos
  FROM crm.tmp_destino_fidelizacion dnew
  JOIN pizzaamericana.cliente_fidelizacion b ON b.correo = dnew.correo_nuevo
 WHERE dnew.cuantos_typos > 1;
