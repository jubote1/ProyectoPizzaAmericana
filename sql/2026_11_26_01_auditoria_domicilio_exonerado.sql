-- Creación de tabla para auditoría de cobro de domicilio exonerado
-- Fecha: 2026-11-26
-- Permite registrar cada vez que un asesor exonera el cobro de flete de domicilio ($5.000)
-- con su respectivo motivo, justificación, usuario y fecha/hora.

CREATE TABLE IF NOT EXISTS pizzaamericana.auditoria_domicilio_exonerado (
  idauditoria INT AUTO_INCREMENT PRIMARY KEY,
  idpedido INT NOT NULL,
  idtienda INT NOT NULL,
  usuario VARCHAR(50) NOT NULL,
  motivo VARCHAR(100) NOT NULL,
  justificacion TEXT NULL,
  valor_exonerado DOUBLE NOT NULL DEFAULT 5000,
  fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_pedido (idpedido),
  KEY idx_fecha (fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
