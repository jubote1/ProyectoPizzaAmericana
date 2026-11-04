-- =====================================================================
-- PLANTILLAS DEL CORREO NOCTURNO DE PUNTOS, PARAMETRIZADAS
-- Base de datos: general (la CENTRAL, 172.19.0.25). Idempotente.
--
-- Hasta hoy los siete templateId de Brevo estaban QUEMADOS en el codigo, y
-- ademas repetidos: siete bloques identicos en ServicioReplicaProgramaFidelizacion
-- y otros siete en ServicioReplicaProgramaFidelizacionRep, el de reproceso.
-- Cambiar una plantilla obligaba a tocar catorce sitios, compilar y desplegar.
-- Ahora salen de esta tabla y se cambian con un UPDATE.
--
-- EL HUECO QUE TENIAN LOS RANGOS VIEJOS
--
-- Los cortes del codigo viejo eran:
--
--   < 50            -> 2
--   >= 50  y < 99   -> 17     el 99 no entraba en NINGUNO
--   >= 100 y < 149  -> 18     el 149 tampoco
--   >= 150 y < 199  -> 19     ni el 199
--   >= 200 y < 249  -> 20     ni el 249
--   >= 250 y < 299  -> 21     ni el 299
--   >= 300          -> 22
--
-- Quien quedara exactamente en 99, 149, 199, 249 o 299 puntos NO recibia
-- correo esa noche. Medido el 2026-11-04 sobre cliente_fidelizacion activos:
-- 106 clientes estaban parados justo en esos valores (99:32, 149:21, 199:26,
-- 249:16, 299:11).
--
-- Aqui los rangos van con puntos_hasta INCLUSIVO y pegados uno con otro, asi
-- que no puede volver a quedar un hueco. Quien lo administre debe mantener esa
-- regla: el desde de una fila es el hasta de la anterior mas uno.
--
-- LOS RANGOS NUEVOS (V2, definidos por Mercadeo el 2026-11-04)
--
-- Son SEIS, no siete: la V2 unio en una sola los dos primeros rangos viejos
-- (menos de 50 y de 50 a 99) en "0 A 99 PUNTOS".
-- =====================================================================

CREATE TABLE IF NOT EXISTS general.plantilla_correo_puntos (
  idplantilla_correo_puntos INT NOT NULL AUTO_INCREMENT,
  idplantilla_brevo         INT NOT NULL COMMENT 'El templateId de Brevo',
  puntos_desde              INT NOT NULL DEFAULT 0 COMMENT 'Inclusivo',
  puntos_hasta              INT NULL COMMENT 'Inclusivo. NULL = sin tope, es el ultimo rango',
  asunto                    VARCHAR(200) NOT NULL DEFAULT '',
  descripcion               VARCHAR(200) NOT NULL DEFAULT '',
  activo                    CHAR(1) NOT NULL DEFAULT 'S',
  PRIMARY KEY (idplantilla_correo_puntos),
  KEY idx_pcp_rango (activo, puntos_desde)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Que plantilla de Brevo le toca al correo nocturno de puntos segun el total acumulado del cliente';

-- Se borra y se vuelve a sembrar SOLO si la tabla quedo vacia, para no pisar
-- un ajuste que Mercadeo haya hecho a mano despues.
INSERT INTO general.plantilla_correo_puntos
      (idplantilla_brevo, puntos_desde, puntos_hasta, asunto, descripcion)
SELECT * FROM (
  SELECT 77 AS a,   0 AS b,   99 AS c, 'Gracias por tu compra' AS d, 'V2 PRIMERA PORCION 0 A 99 PUNTOS'    AS e UNION ALL
  SELECT 79,      100,       149,      'Gracias por tu compra',      'V2 SEGUNDA PORCION 100 A 149 PUNTOS'    UNION ALL
  SELECT 80,      150,       199,      'Gracias por tu compra',      'V2 TERCERA PORCION 150 A 199 PUNTOS'    UNION ALL
  SELECT 81,      200,       249,      'Gracias por tu compra',      'V2 CUARTA PORCION 200 A 249 PUNTOS'     UNION ALL
  SELECT 82,      250,       299,      'Gracias por tu compra',      'V2 QUINTA PORCION 250 A 299 PUNTOS'     UNION ALL
  SELECT 83,      300,       NULL,     'Gracias por tu compra',      'V2 SEXTA PORCION +300 PUNTOS'
) AS v
WHERE NOT EXISTS (SELECT 1 FROM general.plantilla_correo_puntos);

-- ---------------------------------------------------------------------
-- LA BIENVENIDA
--
-- No es un rango, es un correo suelto que sale cuando alguien se matricula al
-- programa. Estaba quemada como templateId 14 en
-- capaControladorCC.FidelizacionCtrl.enviarCorreoBienvenida. La V2 es la 78.
-- ---------------------------------------------------------------------
-- OJO: general.parametros NO tiene columna descripcion. Son cuatro columnas:
-- valorparametro, valornumerico, valortexto y valornumericod.
INSERT INTO general.parametros (valorparametro, valornumerico, valortexto)
SELECT 'PLANTILLABIENVENIDAFIDELIZACION', 78, ''
WHERE NOT EXISTS (SELECT 1 FROM general.parametros
                  WHERE valorparametro = 'PLANTILLABIENVENIDAFIDELIZACION');

-- ---------------------------------------------------------------------
-- COMPROBACION
-- ---------------------------------------------------------------------
SELECT idplantilla_brevo, puntos_desde,
       IFNULL(puntos_hasta, 'sin tope') AS puntos_hasta, activo, descripcion
FROM general.plantilla_correo_puntos
ORDER BY puntos_desde;

-- Que no haya huecos ni solapes: cada desde debe ser el hasta anterior + 1.
SELECT 'HUECO O SOLAPE' AS alerta, p.idplantilla_brevo, p.puntos_desde,
       (SELECT MAX(q.puntos_hasta) FROM general.plantilla_correo_puntos q
         WHERE q.activo = 'S' AND q.puntos_hasta < p.puntos_desde) AS hasta_anterior
FROM general.plantilla_correo_puntos p
WHERE p.activo = 'S'
  AND p.puntos_desde > 0
  AND p.puntos_desde <> 1 + IFNULL((SELECT MAX(q.puntos_hasta)
                                      FROM general.plantilla_correo_puntos q
                                     WHERE q.activo = 'S'
                                       AND q.puntos_hasta < p.puntos_desde), -1);

SELECT valorparametro, valornumerico FROM general.parametros
WHERE valorparametro = 'PLANTILLABIENVENIDAFIDELIZACION';
