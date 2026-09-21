-- ---------------------------------------------------------------------------
-- Webhook de Bold (SONO QR): registro de cada evento recibido.
-- Base de datos: pizzaamericana (172.19.0.25). Idempotente.
--
-- Bold notifica cada pago del QR (y de los datafonos) a un endpoint nuestro
-- (POST /api/bold/webhook). Por ahora el servicio SOLO guarda lo que llega,
-- para poder mirar los eventos reales antes de decidir que hacer con ellos.
--
-- hash_cuerpo (SHA-256 del cuerpo tal como llego) es unico: Bold reintenta
-- hasta 5 veces si no recibe un 200 a tiempo, y el mismo cuerpo no debe
-- quedar guardado dos veces. Las demas columnas son el mismo JSON abierto para
-- poder consultarlo sin parsear; json_evento conserva el cuerpo completo.
--
-- firma_valida / motivo_firma: el resultado de validar el encabezado
-- x-bold-signature con la llave secreta (integracion_crm, crm = 'SONOQR',
-- columna fresh_token). Un evento con firma no valida se guarda igual, marcado,
-- porque en esta fase lo que importa es ver que llega; cuando se empiece a
-- ACTUAR sobre los eventos solo se usaran los de firma_valida = 1.
--
-- Requiere que ya exista el registro en integracion_crm:
--   crm = 'SONOQR', access_token = llave de identidad, fresh_token = llave secreta.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS log_evento_bold (
  idlog_evento_bold bigint NOT NULL AUTO_INCREMENT,
  hash_cuerpo char(64) NOT NULL,
  id_notificacion varchar(80) DEFAULT NULL,
  tipo_evento varchar(40) DEFAULT NULL,
  payment_id varchar(100) DEFAULT NULL,
  merchant_id varchar(100) DEFAULT NULL,
  payment_method varchar(40) DEFAULT NULL,
  monto_total decimal(16,2) DEFAULT NULL,
  moneda varchar(10) DEFAULT NULL,
  referencia varchar(200) DEFAULT NULL,
  terminal_id varchar(100) DEFAULT NULL,
  fecha_evento varchar(60) DEFAULT NULL,
  firma_valida tinyint(1) NOT NULL DEFAULT 0,
  motivo_firma varchar(40) DEFAULT NULL,
  ip_origen varchar(60) DEFAULT NULL,
  json_evento longtext,
  procesado tinyint(1) NOT NULL DEFAULT 0,
  fecha_recepcion timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (idlog_evento_bold),
  UNIQUE KEY uk_hash_cuerpo (hash_cuerpo),
  KEY idx_payment_id (payment_id),
  KEY idx_fecha_recepcion (fecha_recepcion),
  KEY idx_tipo_evento (tipo_evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Eventos que Bold (SONO QR y datafonos) notifica a nuestro webhook, tal como llegaron.';
