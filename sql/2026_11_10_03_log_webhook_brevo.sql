-- ---------------------------------------------------------------------------
-- LOG DE EVENTOS DEL WEBHOOK DE BREVO (DESUSCRIPCIONES, QUEJAS Y REBOTES)
-- Base de datos: crm (172.19.0.25). Idempotente.
--
-- Registra cada notificacion que Brevo envia cuando un cliente:
--  1. Se da de baja voluntariamente (unsubscribed).
--  2. Marca un correo como spam / queja (spam / complaint).
--  3. Rebota de forma permanente / dura (hard_bounce / invalid_email).
--  4. Rebota de forma temporal / blanda (soft_bounce).
--
-- Permite auditar en cualquier momento que novedades llegaron de Brevo y
-- que accion automatica se aplico sobre cliente, persona y correo_rebotado.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS crm.log_webhook_brevo (
    idlog BIGINT NOT NULL AUTO_INCREMENT,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    evento VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL,
    motivo VARCHAR(300) NULL,
    resultado VARCHAR(100) NOT NULL,
    ip_origen VARCHAR(60) NULL,
    json_evento TEXT NULL,
    PRIMARY KEY (idlog),
    KEY idx_log_brevo_fecha (fecha_hora),
    KEY idx_log_brevo_email (email),
    KEY idx_log_brevo_evento (evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Registro auditable de eventos recibidos por el webhook de Brevo';
