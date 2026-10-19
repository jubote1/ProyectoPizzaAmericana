-- =====================================================================
-- DATAMART: FACTURAS ELECTRONICAS Y NOTAS CREDITO DE CADA TIENDA
-- Base de datos: datamart  (CENTRAL, 172.19.0.25)  -- se corre UNA vez
--
-- Para que sirve
--   El reporte mensual de facturacion del central (FacturacionElectronica.html) lee de aqui cuanto se
--   facturo y cuantas notas credito hubo por tienda. La tabla de cada tienda la lleva aqui la replica
--   diaria (ServicioReplicaPedidos), asi que el reporte no depende de que las tiendas esten encendidas.
--
--   Los ids se repiten de una tienda a otra (cada tienda numera sus facturas desde cero), por eso la llave
--   siempre lleva idtienda.
--
--   No se copian qr_url ni qr_data de la factura: ocupan mucho y el reporte no los usa.
--
-- Requiere que cada tienda haya corrido 2026_10_06_01_facturacion_valores_y_notas_credito.sql
-- (POS). Idempotente: se puede correr mas de una vez.
-- =====================================================================

USE datamart;

CREATE TABLE IF NOT EXISTS factura_electronica_generada (
  idtienda             INT NOT NULL,
  idsolicitud          INT NOT NULL,
  idpedidotienda       INT NOT NULL DEFAULT 0,
  prefijo              VARCHAR(10) NOT NULL DEFAULT '',
  numerodocumento      INT NOT NULL DEFAULT 0,
  cufe                 VARCHAR(100) NOT NULL DEFAULT '',
  validacion_dian      TINYINT(1) NULL,
  fecha                VARCHAR(20) NULL,
  hora                 VARCHAR(20) NULL,
  valor_sin_impuestos  DECIMAL(14,2) NULL,
  valor_impuesto       DECIMAL(14,2) NULL,
  valor_total          DECIMAL(14,2) NULL,
  PRIMARY KEY (idtienda, idsolicitud),
  KEY ix_fact_fecha (fecha, idtienda),
  KEY ix_fact_cufe (cufe),
  KEY ix_fact_pedido (idtienda, idpedidotienda)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS nota_credito_electronica_generada (
  idtienda             INT NOT NULL,
  idnota               INT NOT NULL,
  idpedidotienda       INT NOT NULL DEFAULT 0,
  documento_factura    VARCHAR(40) NOT NULL DEFAULT '',
  cufe_factura         VARCHAR(100) NOT NULL DEFAULT '',
  documento_nota       VARCHAR(40) NOT NULL DEFAULT '',
  cufe_nota            VARCHAR(100) NOT NULL DEFAULT '',
  valor_sin_impuestos  DECIMAL(14,2) NULL,
  valor_impuesto       DECIMAL(14,2) NULL,
  valor_total          DECIMAL(14,2) NULL,
  idmotivo             INT NOT NULL DEFAULT 0,
  motivo               VARCHAR(200) NOT NULL DEFAULT '',
  validacion_dian      TINYINT(1) NULL,
  fecha                VARCHAR(20) NULL,
  hora                 VARCHAR(20) NULL,
  PRIMARY KEY (idtienda, idnota),
  KEY ix_nc_fecha (fecha, idtienda),
  KEY ix_nc_cufe_factura (cufe_factura)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verificacion
SELECT table_name FROM information_schema.tables
 WHERE table_schema = 'datamart'
   AND table_name IN ('factura_electronica_generada', 'nota_credito_electronica_generada');
