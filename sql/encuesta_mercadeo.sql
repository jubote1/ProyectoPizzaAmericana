-- =====================================================================
-- ENCUESTAS DE MERCADEO EN FINALIZACIÓN DE PEDIDOS (POS)
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Sigue el patrón ya existente en esta base: pregunta_XXX (configuración)
-- + encuesta_XXX (respuestas planas, una fila por respuesta), como
-- pregunta_servicio / encuesta_servicio.
--
-- Agrega lo que ese patrón no tiene y el requerimiento sí pide:
-- vigencia por fechas, tipo de pregunta, opciones normalizadas y orden.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Configuración de preguntas
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pregunta_mercadeo` (
  `idpregunta`       int NOT NULL AUTO_INCREMENT,
  `titulo`           varchar(50)  NOT NULL COMMENT 'Nombre corto para reportes',
  `descripcion`      varchar(200) NOT NULL COMMENT 'Texto que se le lee al cliente',
  `tipo`             char(1)      NOT NULL DEFAULT 'B' COMMENT 'B=Si/No, O=Opciones, A=Abierta',
  `fecha_inicio`     date         NOT NULL,
  `fecha_fin`        date         NOT NULL,
  `orden`            int          NOT NULL DEFAULT '1',
  `obligatoria`      char(1)      NOT NULL DEFAULT 'N',
  `aplica_pv`        char(1)      NOT NULL DEFAULT 'S' COMMENT 'Aplica en punto de venta',
  `aplica_domicilio` char(1)      NOT NULL DEFAULT 'N' COMMENT 'Reservado, hoy solo PV',
  `activo`           char(1)      NOT NULL DEFAULT 'S',
  `usuario_creacion` varchar(50)      NULL,
  `fecha_creacion`   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idpregunta`),
  UNIQUE KEY `titulo` (`titulo`),
  KEY `vigencia` (`activo`,`fecha_inicio`,`fecha_fin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Configuración de preguntas de encuesta de mercadeo aplicadas en el POS';

-- ---------------------------------------------------------------------
-- 2. Opciones (solo para preguntas tipo 'O')
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `opcion_mercadeo` (
  `idopcion`    int NOT NULL AUTO_INCREMENT,
  `idpregunta`  int NOT NULL,
  `descripcion` varchar(100) NOT NULL,
  `orden`       int NOT NULL DEFAULT '1',
  `activo`      char(1) NOT NULL DEFAULT 'S',
  PRIMARY KEY (`idopcion`),
  KEY `idpregunta` (`idpregunta`),
  CONSTRAINT `fk_opcion_mercadeo_pregunta`
    FOREIGN KEY (`idpregunta`) REFERENCES `pregunta_mercadeo` (`idpregunta`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Opciones de respuesta para preguntas de mercadeo tipo O';

-- ---------------------------------------------------------------------
-- 3. Respuestas — una fila por respuesta, como encuesta_servicio
--
--    OJO: idpedidotienda es el consecutivo de la TIENDA
--    (tiendaamericana.pedido.idpedidotienda), NO pizzaamericana.pedido.idpedido:
--    son secuencias distintas. El par (idpedidotienda, idtienda) identifica
--    el pedido, igual que numero_pedido+idtienda en log_encuesta_servicio.
--    idcliente también es el de la tienda.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `encuesta_mercadeo` (
  `idencuesta`      int NOT NULL AUTO_INCREMENT,
  `idpregunta`      int NOT NULL,
  `respuesta`       varchar(300) NOT NULL COMMENT 'S/N, texto de la opción, o texto libre',
  `idopcion`        int NOT NULL DEFAULT '0' COMMENT '0 si no aplica; evita parsear texto al agrupar',
  `idpedidotienda`  int NOT NULL DEFAULT '0',
  `idtienda`        int NOT NULL DEFAULT '0',
  `idcliente`       int NOT NULL DEFAULT '0',
  `idusuario`       int NOT NULL DEFAULT '0' COMMENT 'Quien operó el POS',
  `fecha_creacion`  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idencuesta`),
  KEY `pedido_tienda` (`idpedidotienda`,`idtienda`),
  KEY `pregunta_fecha` (`idpregunta`,`fecha_creacion`),
  KEY `cliente` (`idcliente`,`idtienda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Respuestas de encuestas de mercadeo tomadas en el POS';
