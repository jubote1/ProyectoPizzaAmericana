-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 172.19.0.25    Database: pizzaamericana
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.4

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `rol`
--

DROP TABLE IF EXISTS `rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol` (
  `idrol` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `descripcion` varchar(200) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `activo` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'S',
  PRIMARY KEY (`idrol`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rol`
--

LOCK TABLES `rol` WRITE;
/*!40000 ALTER TABLE `rol` DISABLE KEYS */;
INSERT INTO `rol` VALUES (1,'Operario','Perfil operativo de punto de venta / contact center, migrado desde administrador=N','S'),(2,'Lider Administrativo','Perfil administrativo completo, migrado desde administrador=S','S'),(3,'Administrador PQRS/Contact Center','Perfil de gestion de PQRS, migrado desde administrador=P. Partir en roles mas finos si el negocio lo requiere','S'),(4,'Administrador de Tienda','Ve el mapa de ubicacion de domiciliarios y el desempeno de domiciliarios de su tienda.','S'),(5,'Gerencia','Acceso al menu Gerencia: calendario de semanas, gastos por tienda, estructura y nomina agregada','S');
/*!40000 ALTER TABLE `rol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menu_modulo`
--

DROP TABLE IF EXISTS `menu_modulo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu_modulo` (
  `idmodulo` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `orden` int NOT NULL DEFAULT '0',
  `idmodulo_padre` int DEFAULT NULL,
  `activo` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'S',
  PRIMARY KEY (`idmodulo`),
  UNIQUE KEY `nombre_padre` (`nombre`,`idmodulo_padre`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu_modulo`
--

LOCK TABLES `menu_modulo` WRITE;
/*!40000 ALTER TABLE `menu_modulo` DISABLE KEYS */;
INSERT INTO `menu_modulo` VALUES (1,'Funciones',10,NULL,'S'),(2,'Parametrizacion',20,NULL,'S'),(3,'Monitoreo',30,NULL,'S'),(4,'CRM',40,NULL,'S'),(5,'Gerencia',50,NULL,'S');
/*!40000 ALTER TABLE `menu_modulo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pantalla`
--

DROP TABLE IF EXISTS `pantalla`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pantalla` (
  `idpantalla` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_general_ci NOT NULL,
  `idmodulo` int NOT NULL,
  `url_html` varchar(150) COLLATE utf8mb4_general_ci NOT NULL,
  `orden` int NOT NULL DEFAULT '0',
  `activo` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'S',
  PRIMARY KEY (`idpantalla`),
  UNIQUE KEY `url_html` (`url_html`)
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pantalla`
--

LOCK TABLES `pantalla` WRITE;
/*!40000 ALTER TABLE `pantalla` DISABLE KEYS */;
INSERT INTO `pantalla` VALUES (1,'Nuevo Pedido - Clasico',1,'PedidosClasico.html',10,'S'),(2,'Nuevo Pedido',1,'Pedidos.html',20,'S'),(3,'Consulta Pedidos Registrados',1,'ConsultaPedidos.html',30,'S'),(4,'Consultar Estados Pedido Tienda',1,'EstadosPedidosTienda.html',40,'S'),(5,'Consultar Pedidos En Curso',1,'ConsultaPedidosEnCurso.html',50,'S'),(6,'Consultar Ped TIENDA VIRTUAL',1,'ConsultaPedidosNuevaTienda.html',60,'S'),(7,'Consultar Pedidos APP',1,'ConsultaPedidosAPP.html',70,'S'),(8,'Consulta Plataforma Domicilios',1,'ConsultaPedidosDomicom.html',80,'S'),(9,'Consulta Pedidos CRM BOT',1,'ConsultaPedidosCRMBOT.html',90,'S'),(10,'RAPPI CARGO',1,'ConsultaCARGO.html',100,'S'),(11,'Registrar PQRS',1,'PQRS.html',110,'S'),(12,'Consulta PQRS',1,'ConsultaPQRS.html',120,'S'),(13,'Modificar PQRS',1,'ModificarPQRS.html',130,'S'),(14,'Consulta Tiempos Tiendas',1,'ConsultaTiemposPedidos.html',140,'S'),(15,'Administracion de Clientes',4,'CRMClientes.html',40,'S'),(16,'Validar Codigo Promocional',1,'ValidarCodigo.html',160,'S'),(17,'Consulta Direccion Fuera Zona',1,'ConsultaDireccionFueraZona.html',170,'S'),(18,'Ubicar Pedidos Tomados en Mapa-Zona',1,'ConsultaPedidosMapaEvolutivo.html',180,'S'),(19,'Resultado Encuesta Mejor Empleado',1,'ResultadoEncuesta.html',190,'S'),(20,'Encuestas de Mercadeo (POS)',1,'ConsultaEncuestaMercadeo.html',200,'S'),(21,'Dispersion Premios Ruleta',1,'DispersionPremiosRuleta.html',210,'S'),(22,'Novedades de Biometria',1,'NovedadesBiometria.html',220,'S'),(23,'Revision Devolucion de Puntos',1,'RevisionReversaRedencion.html',230,'S'),(24,'Historial de Estado de un Pedido',1,'EstadosHistorialPedido.html',240,'S'),(25,'Consultar Solicitudes Fact Electronicas',1,'ConsultaSolicitudFacturaElectronica.html',250,'S'),(26,'Consultar Ventas Empresariales',1,'ConsultaVentaEmpresarial.html',260,'S'),(27,'Consultar Estados Datafonos',1,'ConsultarEstadoDatafonos.html',270,'S'),(28,'Registrar Pedido Cumpleanos',1,'RegistroPedidoCumple.html',280,'S'),(29,'Consultar Registro Pedido Cumpleanos',1,'ConsultarRegistroPedidoCumple.html',290,'S'),(30,'Especialidades',2,'Especialidad.html',10,'S'),(31,'Excepciones de Precios',2,'ExcepcionPrecio.html',20,'S'),(32,'Administrar Ofertas',2,'Oferta.html',30,'S'),(33,'Estado Pedido',2,'EstadoPedido.html',40,'S'),(34,'Productos',2,'Producto.html',50,'S'),(35,'Sabor por Tipo de Liquido',2,'SaborTipoLiquido.html',60,'S'),(36,'Tiendas',2,'Tienda.html',70,'S'),(37,'Tiendas Bloqueadas',2,'TiendaBloqueada.html',80,'S'),(38,'Tipo Liquido',2,'TipoLiquido.html',90,'S'),(39,'Productos No Existentes',2,'MarcarProductoNoExistente.html',100,'S'),(40,'Asignar Tiempos Pedido Tiendas',2,'AsignarTiemposPedidos.html',110,'S'),(41,'Administracion de Clientes Full',4,'CRMClientesFull.html',30,'S'),(42,'Consultar Ubicacion Domiciliario',2,'mapa.jsp',130,'S'),(43,'Modificacion General Pedido',2,'ModificarGeneralPedido.html',140,'S'),(44,'Roles de Seguridad',2,'Rol.html',150,'S'),(45,'Asignar Pantallas a Rol',2,'AsignarPantallasRol.html',160,'S'),(46,'Asignar Rol a Usuario',2,'AsignarRolUsuario.html',170,'S'),(47,'Monitoreo Plataformas Domicilios',3,'MonitoreoPlataformas.html',10,'S'),(48,'Monitoreo Pagos Virtuales',3,'MonitoreoPagosVirtuales.html',20,'S'),(49,'Cancelaciones Plataformas',3,'CancelacionesPlataformas.html',30,'S'),(50,'Consulta Puntos Cliente',4,'ConsultaPuntosCliente.html',50,'S'),(51,'Segmentacion Cliente',4,'segmentacionCliente.html',60,'S'),(52,'Ingresar Solicitud Conciliacion',3,'ConciliacionQRDATAFONO.html',60,'S'),(53,'Consultar Solicitudes Conciliacion',3,'ConsultarConciliacionQRDATAFONO.html',70,'S'),(54,'Consulta Ventas Asesor',3,'ConsultaVentasAsesor.html',80,'S'),(55,'Venta Integral',3,'VentaIntegral.html',90,'S'),(56,'Venta Integral - Categorias',3,'VentaIntegralCategorias.html',100,'S'),(57,'Desempeno de Domiciliarios',3,'DesempenoDomiciliario.html',110,'S'),(58,'CRM - Inicio',4,'CRM.html',10,'S'),(59,'Vista 360 de una persona',4,'Persona360.html',20,'S'),(60,'Segmentacion de Personas',4,'SegmentacionPersona.html',55,'S'),(61,'Definicion de Segmentos',4,'DefinicionSegmentos.html',56,'S'),(62,'Tablero de Segmentos',4,'TableroSegmentos.html',54,'S'),(63,'Envio de Publicidad',4,'EnvioPublicidad.html',25,'S'),(64,'Resultado Preguntas a Empleados',3,'ResultadoPreguntasEmpleado.html',111,'S'),(65,'Calendario de Semanas',5,'CalendarioSemanas.html',10,'S'),(66,'Gastos por Tienda',5,'GastosTienda.html',20,'S'),(67,'Estructura y Nomina',5,'EstructuraGerencia.html',30,'S'),(68,'Redencion Manual de Puntos',4,'RedencionManualPuntos.html',55,'S'),(69,'Nomina por Empleado',5,'NominaEmpleado.html',40,'S'),(70,'Promociones del Reporte Diario',1,'PromocionesReporte.html',240,'S'),(71,'Tablero de Rentabilidad',5,'Rentabilidad.html',50,'S'),(72,'Calculo de Nomina',5,'NominaCalculo.html',45,'S'),(73,'Facturacion Electronica',3,'FacturacionElectronica.html',120,'S'),(74,'Compensaciones por Tiempo',4,'CompensacionesTiempo.html',56,'S'),(75,'Cierre de Gerencia',5,'CierreGerencia.html',60,'S'),(76,'Marcas de Cliente',4,'MarcasCliente.html',56,'S');
/*!40000 ALTER TABLE `pantalla` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rol_pantalla`
--

DROP TABLE IF EXISTS `rol_pantalla`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol_pantalla` (
  `idrol` int NOT NULL,
  `idpantalla` int NOT NULL,
  PRIMARY KEY (`idrol`,`idpantalla`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rol_pantalla`
--

LOCK TABLES `rol_pantalla` WRITE;
/*!40000 ALTER TABLE `rol_pantalla` DISABLE KEYS */;
INSERT INTO `rol_pantalla` VALUES (4,42),(4,57),(5,65),(5,66),(5,67),(5,69),(5,71),(5,72),(5,75);
/*!40000 ALTER TABLE `rol_pantalla` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario_rol`
--

DROP TABLE IF EXISTS `usuario_rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario_rol` (
  `idusuario` int NOT NULL,
  `idrol` int NOT NULL,
  PRIMARY KEY (`idusuario`,`idrol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario_rol`
--

LOCK TABLES `usuario_rol` WRITE;
/*!40000 ALTER TABLE `usuario_rol` DISABLE KEYS */;
INSERT INTO `usuario_rol` VALUES (1,5),(19,2),(27,1),(63,2),(101,1),(115,1),(118,2),(119,1),(121,1),(126,1),(128,1),(129,1);
/*!40000 ALTER TABLE `usuario_rol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pantalla_servlet`
--

DROP TABLE IF EXISTS `pantalla_servlet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pantalla_servlet` (
  `idpantalla` int NOT NULL,
  `patron_url` varchar(150) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`idpantalla`,`patron_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pantalla_servlet`
--

LOCK TABLES `pantalla_servlet` WRITE;
/*!40000 ALTER TABLE `pantalla_servlet` DISABLE KEYS */;
/*!40000 ALTER TABLE `pantalla_servlet` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-28  8:32:24
