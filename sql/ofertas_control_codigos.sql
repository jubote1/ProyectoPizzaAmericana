-- ---------------------------------------------------------------------------
-- CONTROL DE OFERTAS Y CODIGOS PROMOCIONALES
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Es aditivo e idempotente:
-- solo agrega columnas con valor por defecto y tablas nuevas; el codigo actual sigue
-- funcionando igual mientras no se despliegue el nuevo.
--
-- QUE RESUELVE
--
-- 1. El descuento por codigo en la tienda no estaba unido al codigo: el valor se
--    digitaba y el "viene del codigo X" era texto libre. Ahora el codigo se RESERVA al
--    validarlo, el servidor calcula el descuento, y se CONFIRMA cuando el pedido se
--    finaliza (o se libera si se cancela). Necesita las columnas de reserva.
-- 2. log_redencion_codigo no tenia fecha, ni pedido, ni tienda: de 834 registros, 418
--    tenian descuento en cero y 395 eran del usuario "caja". No se podia auditar.
-- 3. La oferta solo sabia de % o valor, horas y fechas. Ahora puede exigir monto
--    minimo, tener tope de descuento, limitarse a unas tiendas, a mostrador o a
--    domicilio, y a un maximo de usos por cliente y de codigos emitidos.
-- 4. Un envio de publicidad puede llevar una oferta: cada destinatario recibe su propio
--    codigo, ligado al envio (oferta_cliente.idenvio) para poder medirlo.
--
-- IMPORTANTE (leccion de otra migracion): las columnas de fecha nuevas van SIN valor por
-- defecto. Un ADD COLUMN ... DEFAULT CURRENT_TIMESTAMP estampa la hora del ALTER en TODAS
-- las filas viejas, y el historial diria que todo paso el dia de la migracion.
-- ---------------------------------------------------------------------------

USE pizzaamericana;

DROP PROCEDURE IF EXISTS tmp_agregar_columna;
DELIMITER $$
CREATE PROCEDURE tmp_agregar_columna(IN tabla VARCHAR(64), IN columna VARCHAR(64), IN definicion VARCHAR(400))
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = 'pizzaamericana' AND table_name = tabla AND column_name = columna) = 0 THEN
    SET @ddl = CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN ', columna, ' ', definicion);
    PREPARE paso FROM @ddl; EXECUTE paso; DEALLOCATE PREPARE paso;
  END IF;
END$$
DELIMITER ;

-- 1. Reglas de la oferta (todas opcionales: 0 o vacio = sin restriccion)
CALL tmp_agregar_columna('oferta', 'monto_minimo',     'DOUBLE NOT NULL DEFAULT 0 COMMENT ''Total minimo del pedido para poder usarla''');
CALL tmp_agregar_columna('oferta', 'tope_descuento',   'DOUBLE NOT NULL DEFAULT 0 COMMENT ''Maximo descuento en pesos por uso''');
CALL tmp_agregar_columna('oferta', 'tiendas',          'VARCHAR(200) NULL COMMENT ''idtienda separados por coma; vacio = todas''');
CALL tmp_agregar_columna('oferta', 'aplica_a',         'CHAR(1) NOT NULL DEFAULT ''T'' COMMENT ''T todos, M solo mostrador, D solo domicilio''');
CALL tmp_agregar_columna('oferta', 'max_usos_cliente', 'INT NOT NULL DEFAULT 0 COMMENT ''Veces que un mismo cliente puede redimirla; 0 = sin limite''');
CALL tmp_agregar_columna('oferta', 'max_emision',      'INT NOT NULL DEFAULT 0 COMMENT ''Codigos que se pueden emitir en total; 0 = sin limite''');

-- 2. Reserva del codigo y liga con el envio de publicidad
CALL tmp_agregar_columna('oferta_cliente', 'reservada_hasta',  'DATETIME NULL');
CALL tmp_agregar_columna('oferta_cliente', 'reserva_token',    'VARCHAR(40) NULL');
CALL tmp_agregar_columna('oferta_cliente', 'reserva_tienda',   'INT NULL');
CALL tmp_agregar_columna('oferta_cliente', 'reserva_usuario',  'VARCHAR(50) NULL');
CALL tmp_agregar_columna('oferta_cliente', 'reserva_descuento','DOUBLE NULL COMMENT ''Descuento que el servidor autorizo al reservar''');
CALL tmp_agregar_columna('oferta_cliente', 'idenvio',          'BIGINT NULL COMMENT ''Envio de publicidad que emitio este codigo''');
CALL tmp_agregar_columna('oferta_cliente', 'anulada',          'CHAR(1) NOT NULL DEFAULT ''N'' COMMENT ''S = anulada a mano, ya no sirve''');

-- 3. El log de redenciones, que ahora tiene que poder auditarse (NULL en lo historico)
CALL tmp_agregar_columna('log_redencion_codigo', 'fecha_real', 'DATETIME NULL');
CALL tmp_agregar_columna('log_redencion_codigo', 'idpedido',   'INT NULL');
CALL tmp_agregar_columna('log_redencion_codigo', 'idtienda',   'INT NULL');
CALL tmp_agregar_columna('log_redencion_codigo', 'origen',     'VARCHAR(10) NULL COMMENT ''POS, WEB o CENTRAL''');
CALL tmp_agregar_columna('log_redencion_codigo', 'estado',     'VARCHAR(12) NULL COMMENT ''OK, EXCESO (descuento mayor al autorizado) o CONFLICTO''');
CALL tmp_agregar_columna('log_redencion_codigo', 'idoferta',   'INT NULL');
CALL tmp_agregar_columna('log_redencion_codigo', 'token_reserva', 'VARCHAR(40) NULL');

DROP PROCEDURE tmp_agregar_columna;

-- 4. Indices para lo que ahora se consulta
SET @sql = (SELECT IF(COUNT(*) = 0,
  'CREATE INDEX idx_ofcli_envio ON oferta_cliente (idenvio)', 'SELECT ''idx_ofcli_envio ya existe'' AS nota')
  FROM information_schema.statistics WHERE table_schema = 'pizzaamericana' AND table_name = 'oferta_cliente' AND index_name = 'idx_ofcli_envio');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'CREATE INDEX idx_ofcli_reserva ON oferta_cliente (reserva_token)', 'SELECT ''idx_ofcli_reserva ya existe'' AS nota')
  FROM information_schema.statistics WHERE table_schema = 'pizzaamericana' AND table_name = 'oferta_cliente' AND index_name = 'idx_ofcli_reserva');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'CREATE INDEX idx_logred_oferta ON log_redencion_codigo (idofertacliente, fecha_real)', 'SELECT ''idx_logred_oferta ya existe'' AS nota')
  FROM information_schema.statistics WHERE table_schema = 'pizzaamericana' AND table_name = 'log_redencion_codigo' AND index_name = 'idx_logred_oferta');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- 5. Bitacora de cambios a las ofertas: quien cambio que y cuando
CREATE TABLE IF NOT EXISTS oferta_bitacora (
  idbitacora INT NOT NULL AUTO_INCREMENT,
  idoferta INT NOT NULL,
  fecha_real DATETIME NOT NULL,
  usuario VARCHAR(50) DEFAULT NULL,
  accion VARCHAR(20) NOT NULL COMMENT 'CREAR, EDITAR, ELIMINAR, REGLAS, ANULAR_CODIGO, ANULAR_ENVIO',
  detalle VARCHAR(2000) DEFAULT NULL,
  PRIMARY KEY (idbitacora),
  KEY ix_bitacora_oferta (idoferta, fecha_real)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Quien creo, edito o elimino una oferta, y que cambio.';

-- Verificacion
SELECT table_name, column_name, column_type
  FROM information_schema.columns
 WHERE table_schema = 'pizzaamericana'
   AND ((table_name = 'oferta' AND column_name IN ('monto_minimo','tope_descuento','tiendas','aplica_a','max_usos_cliente','max_emision'))
     OR (table_name = 'oferta_cliente' AND column_name IN ('reservada_hasta','reserva_token','reserva_tienda','reserva_usuario','reserva_descuento','idenvio','anulada'))
     OR (table_name = 'log_redencion_codigo' AND column_name IN ('fecha_real','idpedido','idtienda','origen','estado','idoferta','token_reserva')))
 ORDER BY table_name, column_name;
