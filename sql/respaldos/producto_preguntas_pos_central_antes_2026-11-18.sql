-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 172.19.0.25    Database: pos_central
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.5

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
-- Dumping data for table `producto`
--
-- WHERE:  idproducto IN (2,469,470)

LOCK TABLES `producto` WRITE;
/*!40000 ALTER TABLE `producto` DISABLE KEYS */;
INSERT INTO `producto` VALUES (2,'Lasagña Mixta','LASAGNA','Lasagña Mixta','Lasagña Mixta','',9,11,0,0,0,0,0,0,0,0,23000,0,0,0,0,0,0,0,0,0,0,'N','','O','LASAG MIXTA',0,0,NULL,_binary '0','N','S',0,0,'N',_binary '0',_binary '1',_binary '0',0,'N',8,'N',0);
INSERT INTO `producto` VALUES (469,'Pizza MD Estofada','Pizza MD Estofada','Pizza MD Estofada','Pizza MD Estofada','',43,8,10,0,0,0,0,0,0,0,48000,0,0,0,0,0,0,0,0,0,0,'N','','P','MD',2,0,NULL,_binary '0','N','N',0,0,'N',_binary '0',_binary '1',_binary '0',0,'N',12,'N',0);
INSERT INTO `producto` VALUES (470,'Pizza GD Estofada','Pizza GD Estofada','Pizza GD Estofada','Pizza GD Estofada','',44,3,10,0,0,0,0,0,0,0,55000,0,0,0,0,0,0,0,0,0,0,'N','','P','GD',2,0,NULL,_binary '0','N','N',0,0,'N',_binary '0',_binary '1',_binary '0',0,'N',13,'N',0);
/*!40000 ALTER TABLE `producto` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-18 10:25:42
