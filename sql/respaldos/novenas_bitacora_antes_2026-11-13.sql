-- Respaldo (2026-11-13) de como estaban la tabla y la vista de la bitacora de Novenas ANTES de adaptarlas al codigo.
-- La tabla tenia 0 filas. Para devolverlas: DROP VIEW pizzaamericana.campana_novenas_bitacora; DROP TABLE tiendaamericana.campana_novenas_bitacora; y correr lo de abajo.
USE tiendaamericana;
CREATE TABLE `campana_novenas_bitacora` (;
  `idbitacora` int NOT NULL AUTO_INCREMENT,;
  `idlead` int NOT NULL,;
  `idusuario` int NOT NULL,;
  `nombre_usuario` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,;
  `estado_gestion` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,;
  `fecha_tentativa_evento` date DEFAULT NULL,;
  `pizzas_estimadas` int DEFAULT '0',;
  `observacion` text COLLATE utf8mb4_unicode_ci,;
  `creado_en` timestamp NULL DEFAULT CURRENT_TIMESTAMP,;
  PRIMARY KEY (`idbitacora`),;
  KEY `idx_idlead` (`idlead`),;
  KEY `idx_idusuario` (`idusuario`),;
  KEY `idx_fecha` (`creado_en`);
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
USE pizzaamericana;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `pizzaamericana`.`campana_novenas_bitacora` AS select `tiendaamericana`.`campana_novenas_bitacora`.`idbitacora` AS `idbitacora`,`tiendaamericana`.`campana_novenas_bitacora`.`idlead` AS `idlead`,`tiendaamericana`.`campana_novenas_bitacora`.`idusuario` AS `idusuario`,`tiendaamericana`.`campana_novenas_bitacora`.`nombre_usuario` AS `nombre_usuario`,`tiendaamericana`.`campana_novenas_bitacora`.`estado_gestion` AS `estado_gestion`,`tiendaamericana`.`campana_novenas_bitacora`.`fecha_tentativa_evento` AS `fecha_tentativa_evento`,`tiendaamericana`.`campana_novenas_bitacora`.`pizzas_estimadas` AS `pizzas_estimadas`,`tiendaamericana`.`campana_novenas_bitacora`.`observacion` AS `observacion`,`tiendaamericana`.`campana_novenas_bitacora`.`creado_en` AS `creado_en` from `tiendaamericana`.`campana_novenas_bitacora`;
