CREATE DATABASE IF NOT EXISTS `reciclame_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `reciclame_db`;
-- MySQL dump 10.13  Distrib 8.0.28, for Win64 (x86_64)
--
-- Host: localhost    Database: reciclame_db
-- ------------------------------------------------------
-- Server version	8.0.28

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */
;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */
;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */
;
/*!50503 SET NAMES utf8 */
;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */
;
/*!40103 SET TIME_ZONE='+00:00' */
;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */
;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */
;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */
;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */
;

--
-- Table structure for table `auditoria`
--

DROP TABLE IF EXISTS `auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `auditoria` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint DEFAULT NULL,
    `usuario_nombre` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `accion` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `modulo` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `descripcion` text COLLATE utf8mb4_unicode_ci NOT NULL,
    `datos_anteriores` json DEFAULT NULL,
    `datos_nuevos` json DEFAULT NULL,
    `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `estado` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EXITOSO',
    `mensaje_error` text COLLATE utf8mb4_unicode_ci,
    `fecha_accion` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    KEY `idx_usuario` (`usuario_id`),
    KEY `idx_accion` (`accion`),
    KEY `idx_modulo` (`modulo`),
    KEY `idx_estado` (`estado`),
    KEY `idx_fecha` (`fecha_accion`)
) ENGINE = InnoDB AUTO_INCREMENT = 7 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `auditoria`
--

LOCK TABLES `auditoria` WRITE;
/*!40000 ALTER TABLE `auditoria` DISABLE KEYS */
;
INSERT INTO
    `auditoria`
VALUES (
        1,
        NULL,
        'Admin',
        'EDITAR_USUARIO',
        'USUARIOS',
        'Editar usuario: harold (ID: 6)',
        NULL,
        NULL,
        '0:0:0:0:0:0:0:1',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',
        'EXITOSO',
        NULL,
        '2026-04-26 04:51:55',
        1
    ),
    (
        2,
        1,
        'Admin Genera',
        'EDITAR_USUARIO',
        'USUARIOS',
        'Editar usuario: Adso (ID: 1)',
        NULL,
        NULL,
        '0:0:0:0:0:0:0:1',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',
        'EXITOSO',
        NULL,
        '2026-04-26 04:56:53',
        1
    ),
    (
        3,
        1,
        'Adso',
        'EDITAR_USUARIO',
        'USUARIOS',
        'Editar usuario: Ad (ID: 1)',
        NULL,
        NULL,
        '0:0:0:0:0:0:0:1',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',
        'EXITOSO',
        NULL,
        '2026-04-26 05:02:02',
        1
    ),
    (
        4,
        1,
        'Ad',
        'EDITAR_USUARIO',
        'USUARIOS',
        'Editar usuario: Visualiza (ID: 4)',
        NULL,
        NULL,
        '0:0:0:0:0:0:0:1',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',
        'EXITOSO',
        NULL,
        '2026-04-26 05:07:35',
        1
    ),
    (
        5,
        1,
        'Ad',
        'EDITAR_USUARIO',
        'USUARIOS',
        'Editar usuario: esddgfhbj (ID: 7)',
        NULL,
        NULL,
        '0:0:0:0:0:0:0:1',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',
        'EXITOSO',
        NULL,
        '2026-04-26 05:13:52',
        1
    ),
    (
        6,
        1,
        'Ad',
        'EDITAR_USUARIO',
        'USUARIOS',
        'Editar usuario: harold_decadencia (ID: 6)',
        NULL,
        NULL,
        '0:0:0:0:0:0:0:1',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36',
        'EXITOSO',
        NULL,
        '2026-04-26 05:21:39',
        1
    );
/*!40000 ALTER TABLE `auditoria` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `cambios_documento`
--

DROP TABLE IF EXISTS `cambios_documento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `cambios_documento` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `documento_tipo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'NORMATIVA, ESTUDIO',
    `documento_id` bigint NOT NULL,
    `modificado_por_id` bigint NOT NULL,
    `tipo_cambio` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `campos_antes` json DEFAULT NULL,
    `campos_despues` json DEFAULT NULL,
    `motivo` text COLLATE utf8mb4_unicode_ci,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_cambios_documento` (
        `documento_tipo`,
        `documento_id`
    ),
    KEY `idx_cambios_usuario` (`modificado_por_id`),
    KEY `idx_cambios_fecha` (`created_at`),
    CONSTRAINT `fk_cambios_usuario` FOREIGN KEY (`modificado_por_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Historial versionado de cambios';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `cambios_documento`
--

LOCK TABLES `cambios_documento` WRITE;
/*!40000 ALTER TABLE `cambios_documento` DISABLE KEYS */
;
/*!40000 ALTER TABLE `cambios_documento` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `conversaciones_chatbot`
--

DROP TABLE IF EXISTS `conversaciones_chatbot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `conversaciones_chatbot` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint NOT NULL,
    `idioma` varchar(5) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'es',
    `resumen` text COLLATE utf8mb4_unicode_ci,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversaciones_usuario` (`usuario_id`),
    KEY `idx_conversaciones_fecha` (`created_at`),
    CONSTRAINT `fk_conversaciones_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `conversaciones_chatbot`
--

LOCK TABLES `conversaciones_chatbot` WRITE;
/*!40000 ALTER TABLE `conversaciones_chatbot` DISABLE KEYS */
;
/*!40000 ALTER TABLE `conversaciones_chatbot` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `documentos_recientes`
--

DROP TABLE IF EXISTS `documentos_recientes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `documentos_recientes` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint NOT NULL,
    `documento_tipo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'NORMATIVA, ESTUDIO',
    `documento_id` bigint NOT NULL,
    `ultima_vista` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_recientes_usuario_doc` (
        `usuario_id`,
        `documento_tipo`,
        `documento_id`
    ),
    KEY `idx_recientes_usuario` (`usuario_id`),
    KEY `idx_recientes_fecha` (`ultima_vista`),
    CONSTRAINT `fk_recientes_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Acceso rapido a documentos vistos recientemente';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `documentos_recientes`
--

LOCK TABLES `documentos_recientes` WRITE;
/*!40000 ALTER TABLE `documentos_recientes` DISABLE KEYS */
;
/*!40000 ALTER TABLE `documentos_recientes` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `empresas`
--

DROP TABLE IF EXISTS `empresas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `empresas` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `razon_social` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
    `ruc` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `sector` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ruc` (`ruc`),
    KEY `idx_empresas_razon_social` (`razon_social`)
) ENGINE = InnoDB AUTO_INCREMENT = 5 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Empresas socias de ReciclamE';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `empresas`
--

LOCK TABLES `empresas` WRITE;
/*!40000 ALTER TABLE `empresas` DISABLE KEYS */
;
INSERT INTO
    `empresas`
VALUES (
        1,
        'ReciclamE S.A.C.',
        '20512345678',
        'Gestion ambiental',
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        2,
        'Plasticos del Peru S.A.',
        '20498765432',
        'Plasticos PET',
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        3,
        'Envases Tetra Peru',
        '20587654321',
        'Envases Tetra',
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        4,
        'EcoIndustrias Peru',
        '20611223344',
        'Reciclaje industrial',
        1,
        '2026-04-22 00:47:45',
        '2026-04-22 00:47:45'
    );
/*!40000 ALTER TABLE `empresas` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `estados_normativa`
--

DROP TABLE IF EXISTS `estados_normativa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `estados_normativa` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `nombre` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `nombre` (`nombre`)
) ENGINE = InnoDB AUTO_INCREMENT = 6 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `estados_normativa`
--

LOCK TABLES `estados_normativa` WRITE;
/*!40000 ALTER TABLE `estados_normativa` DISABLE KEYS */
;
INSERT INTO
    `estados_normativa`
VALUES (
        1,
        'Vigente',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        2,
        'Proyecto publicado',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        3,
        'Proyecto / borrador en proceso',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        4,
        'Consulta publica',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        5,
        'Derogada',
        1,
        '2026-04-22 00:47:40'
    );
/*!40000 ALTER TABLE `estados_normativa` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `estudios`
--

DROP TABLE IF EXISTS `estudios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `estudios` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `titulo` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
    `descripcion_breve` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `resumen` text COLLATE utf8mb4_unicode_ci,
    `tematica_id` bigint NOT NULL,
    `autor` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `fecha_estudio` date DEFAULT NULL,
    `anio` int DEFAULT NULL,
    `archivo_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
    `archivo_tipo_mime` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `vt_sha256` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `vt_verdict` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `vt_malicious_engines` int DEFAULT NULL,
    `vt_suspicious_engines` int DEFAULT NULL,
    `vt_total_engines` int DEFAULT NULL,
    `vt_skipped` tinyint(1) DEFAULT NULL,
    `vt_scanned_at` timestamp NULL DEFAULT NULL,
    `normativa_relacionada_id` bigint DEFAULT NULL,
    `subida_por_id` bigint NOT NULL,
    `validado_por_id` bigint DEFAULT NULL,
    `es_publico` tinyint(1) NOT NULL DEFAULT '0',
    `visualizaciones_count` int NOT NULL DEFAULT '0',
    `descargas_count` int NOT NULL DEFAULT '0',
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_estudios_normativa` (`normativa_relacionada_id`),
    KEY `fk_estudios_subidor` (`subida_por_id`),
    KEY `fk_estudios_validador` (`validado_por_id`),
    KEY `idx_estudios_tematica` (`tematica_id`),
    KEY `idx_estudios_anio` (`anio`),
    KEY `idx_estudios_publico` (`es_publico`),
    KEY `idx_estudios_vt_sha256` (`vt_sha256`),
    CONSTRAINT `fk_estudios_normativa` FOREIGN KEY (`normativa_relacionada_id`) REFERENCES `normativas` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_estudios_subidor` FOREIGN KEY (`subida_por_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_estudios_tematica` FOREIGN KEY (`tematica_id`) REFERENCES `tematicas` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_estudios_validador` FOREIGN KEY (`validado_por_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB AUTO_INCREMENT = 3 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Estudios e investigaciones';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `estudios`
--

LOCK TABLES `estudios` WRITE;
/*!40000 ALTER TABLE `estudios` DISABLE KEYS */;
/*!40000 ALTER TABLE `estudios` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estudios_etiquetas`
--

DROP TABLE IF EXISTS `estudios_etiquetas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `estudios_etiquetas` (
    `estudio_id` bigint NOT NULL,
    `etiqueta_id` bigint NOT NULL,
    PRIMARY KEY (`estudio_id`, `etiqueta_id`),
    KEY `fk_est_etiq_etiqueta` (`etiqueta_id`),
    CONSTRAINT `fk_est_etiq_estudio` FOREIGN KEY (`estudio_id`) REFERENCES `estudios` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_est_etiq_etiqueta` FOREIGN KEY (`etiqueta_id`) REFERENCES `etiquetas` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `estudios_etiquetas`
--

LOCK TABLES `estudios_etiquetas` WRITE;
/*!40000 ALTER TABLE `estudios_etiquetas` DISABLE KEYS */
;
/*!40000 ALTER TABLE `estudios_etiquetas` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `etiquetas`
--

DROP TABLE IF EXISTS `etiquetas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `etiquetas` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `nombre` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `nombre` (`nombre`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Etiquetas libres para clasificar';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `etiquetas`
--

LOCK TABLES `etiquetas` WRITE;
/*!40000 ALTER TABLE `etiquetas` DISABLE KEYS */
;
/*!40000 ALTER TABLE `etiquetas` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `logs_actividad`
--

DROP TABLE IF EXISTS `logs_actividad`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `logs_actividad` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint NOT NULL,
    `tipo_accion` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'LOGIN, LOGOUT, DESCARGAR, VISUALIZAR, CREAR, EDITAR, ELIMINAR',
    `recurso_tipo` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'NORMATIVA, ESTUDIO, USUARIO, SISTEMA',
    `recurso_id` bigint DEFAULT NULL,
    `descripcion_detalle` text COLLATE utf8mb4_unicode_ci,
    `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_logs_usuario` (`usuario_id`),
    KEY `idx_logs_tipo_accion` (`tipo_accion`),
    KEY `idx_logs_fecha` (`created_at`),
    KEY `idx_logs_reporte_usuario_fecha` (
        `usuario_id`,
        `created_at`,
        `tipo_accion`
    ),
    KEY `idx_logs_reporte_descarga` (
        `tipo_accion`,
        `recurso_tipo`,
        `recurso_id`,
        `created_at`
    ),
    CONSTRAINT `fk_logs_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Registro de actividad del sistema';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `logs_actividad`
--

LOCK TABLES `logs_actividad` WRITE;
/*!40000 ALTER TABLE `logs_actividad` DISABLE KEYS */
;
/*!40000 ALTER TABLE `logs_actividad` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `mensajes_chatbot`
--

DROP TABLE IF EXISTS `mensajes_chatbot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `mensajes_chatbot` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `conversacion_id` bigint NOT NULL,
    `rol` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'USER, BOT',
    `contenido` text COLLATE utf8mb4_unicode_ci NOT NULL,
    `intent_detectado` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `confianza` decimal(5, 4) DEFAULT NULL,
    `derivado_soporte` tinyint(1) NOT NULL DEFAULT '0',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_mensajes_conversacion` (`conversacion_id`),
    KEY `idx_mensajes_fecha` (`created_at`),
    CONSTRAINT `fk_mensajes_conversacion` FOREIGN KEY (`conversacion_id`) REFERENCES `conversaciones_chatbot` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `mensajes_chatbot`
--

LOCK TABLES `mensajes_chatbot` WRITE;
/*!40000 ALTER TABLE `mensajes_chatbot` DISABLE KEYS */
;
/*!40000 ALTER TABLE `mensajes_chatbot` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `normativas`
--

DROP TABLE IF EXISTS `normativas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `normativas` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `titulo` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
    `descripcion_breve` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `contenido_resumido` text COLLATE utf8mb4_unicode_ci,
    `tematica_id` bigint NOT NULL,
    `tipo_normativa_id` bigint NOT NULL,
    `estado_normativa_id` bigint NOT NULL,
    `ambito` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NACIONAL' COMMENT 'NACIONAL, INTERNACIONAL',
    `clasificacion` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VINCULANTE' COMMENT 'VINCULANTE, NO_VINCULANTE',
    `anio` int NOT NULL,
    `fecha_promulgacion` date DEFAULT NULL,
    `fecha_entrada_vigencia` date DEFAULT NULL,
    `fecha_derogacion` date DEFAULT NULL,
    `archivo_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
    `archivo_tipo_mime` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `vt_sha256` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `vt_verdict` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `vt_malicious_engines` int DEFAULT NULL,
    `vt_suspicious_engines` int DEFAULT NULL,
    `vt_total_engines` int DEFAULT NULL,
    `vt_skipped` tinyint(1) DEFAULT NULL,
    `vt_scanned_at` timestamp NULL DEFAULT NULL,
    `creada_por_id` bigint NOT NULL,
    `validada_por_id` bigint DEFAULT NULL,
    `es_publica` tinyint(1) NOT NULL DEFAULT '0',
    `visualizaciones_count` int NOT NULL DEFAULT '0',
    `descargas_count` int NOT NULL DEFAULT '0',
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_normativas_tipo` (`tipo_normativa_id`),
    KEY `fk_normativas_creador` (`creada_por_id`),
    KEY `fk_normativas_validador` (`validada_por_id`),
    KEY `idx_normativas_tematica` (`tematica_id`),
    KEY `idx_normativas_estado` (`estado_normativa_id`),
    KEY `idx_normativas_anio` (`anio`),
    KEY `idx_normativas_publica` (`es_publica`),
    KEY `idx_normativas_clasificacion` (`clasificacion`),
    KEY `idx_normativas_vt_sha256` (`vt_sha256`),
    KEY `idx_normativas_filtro` (
        `ambito`,
        `clasificacion`,
        `tematica_id`,
        `estado_normativa_id`,
        `tipo_normativa_id`,
        `anio`
    ),
    CONSTRAINT `fk_normativas_creador` FOREIGN KEY (`creada_por_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_normativas_estado` FOREIGN KEY (`estado_normativa_id`) REFERENCES `estados_normativa` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_normativas_tematica` FOREIGN KEY (`tematica_id`) REFERENCES `tematicas` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_normativas_tipo` FOREIGN KEY (`tipo_normativa_id`) REFERENCES `tipos_normativa` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_normativas_validador` FOREIGN KEY (`validada_por_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_normativas_ambito` CHECK (
        (
            `ambito` in (
                _utf8mb4 'NACIONAL',
                _utf8mb4 'INTERNACIONAL'
            )
        )
    ),
    CONSTRAINT `chk_normativas_clasificacion` CHECK (
        (
            `clasificacion` in (
                _utf8mb4 'VINCULANTE',
                _utf8mb4 'NO_VINCULANTE'
            )
        )
    )
) ENGINE = InnoDB AUTO_INCREMENT = 3 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Repositorio de normativas legales';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `normativas`
--

LOCK TABLES `normativas` WRITE;
/*!40000 ALTER TABLE `normativas` DISABLE KEYS */
;
INSERT INTO
    `normativas`
VALUES (
        1,
        'estudio_2',
        NULL,
        NULL,
        2,
        5,
        5,
        'NACIONAL',
        'VINCULANTE',
        2026,
        NULL,
        NULL,
        NULL,
        'http://localhost:9000/reciclame-docs/1777348492547_LAB2_H791_G2.pdf',
        'application/pdf',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        1,
        NULL,
        1,
        NULL,
        1,
        0,
        0,
        1,
        '2026-04-28 08:54:53',
        '2026-04-28 08:56:13'
    ),
    (
        2,
        'normativa_3',
        NULL,
        NULL,
        1,
        1,
        4,
        'INTERNACIONAL',
        'NO_VINCULANTE',
        2026,
        '2026-04-28',
        '2026-04-29',
        '2026-04-28',
        'http://localhost:9000/reciclame-docs/1777348540077_Invoice-ITSKHBXP-0001.pdf',
        'application/pdf',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        1,
        NULL,
        1,
        NULL,
        1,
        0,
        0,
        1,
        '2026-04-28 08:55:40',
        '2026-04-28 08:55:55'
    );
/*!40000 ALTER TABLE `normativas` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `normativas_etiquetas`
--

DROP TABLE IF EXISTS `normativas_etiquetas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `normativas_etiquetas` (
    `normativa_id` bigint NOT NULL,
    `etiqueta_id` bigint NOT NULL,
    PRIMARY KEY (`normativa_id`, `etiqueta_id`),
    KEY `fk_norm_etiq_etiqueta` (`etiqueta_id`),
    CONSTRAINT `fk_norm_etiq_etiqueta` FOREIGN KEY (`etiqueta_id`) REFERENCES `etiquetas` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_norm_etiq_normativa` FOREIGN KEY (`normativa_id`) REFERENCES `normativas` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `normativas_etiquetas`
--

LOCK TABLES `normativas_etiquetas` WRITE;
/*!40000 ALTER TABLE `normativas_etiquetas` DISABLE KEYS */
;
/*!40000 ALTER TABLE `normativas_etiquetas` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `notificaciones`
--

DROP TABLE IF EXISTS `notificaciones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `notificaciones` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint NOT NULL,
    `tipo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'NUEVA_NORMATIVA, NUEVO_ESTUDIO, DOCUMENTO_VALIDADO, MENSAJE_ADMIN, SISTEMA',
    `titulo` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `mensaje` text COLLATE utf8mb4_unicode_ci NOT NULL,
    `enlace_recurso` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `leida` tinyint(1) NOT NULL DEFAULT '0',
    `fecha_lectura` timestamp NULL DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notificaciones_usuario` (`usuario_id`),
    KEY `idx_notificaciones_leida` (`leida`),
    KEY `idx_notificaciones_fecha` (`created_at`),
    CONSTRAINT `fk_notificaciones_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `notificaciones`
--

LOCK TABLES `notificaciones` WRITE;
/*!40000 ALTER TABLE `notificaciones` DISABLE KEYS */
;
/*!40000 ALTER TABLE `notificaciones` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `parametros_sistema`
--

DROP TABLE IF EXISTS `parametros_sistema`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `parametros_sistema` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `clave` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `valor` text COLLATE utf8mb4_unicode_ci NOT NULL,
    `descripcion` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `tipo_dato` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STRING' COMMENT 'STRING, INTEGER, BOOLEAN, JSON',
    `modificado_por_id` bigint DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `clave` (`clave`),
    KEY `fk_parametros_modificador` (`modificado_por_id`),
    KEY `idx_parametros_clave` (`clave`),
    CONSTRAINT `fk_parametros_modificador` FOREIGN KEY (`modificado_por_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB AUTO_INCREMENT = 8 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Parametros globales configurables sin tocar codigo';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `parametros_sistema`
--

LOCK TABLES `parametros_sistema` WRITE;
/*!40000 ALTER TABLE `parametros_sistema` DISABLE KEYS */
;
INSERT INTO
    `parametros_sistema`
VALUES (
        1,
        'max_intentos_login',
        '5',
        'Intentos fallidos antes de bloquear',
        'INTEGER',
        NULL,
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        2,
        'tiempo_bloqueo_minutos',
        '30',
        'Minutos que dura el bloqueo',
        'INTEGER',
        NULL,
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        3,
        'magic_link_expiracion_min',
        '10',
        'Minutos de vida de un magic link',
        'INTEGER',
        NULL,
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        4,
        'recovery_expiracion_min',
        '30',
        'Minutos para recuperar contrasena',
        'INTEGER',
        NULL,
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        5,
        'filtro_max_anios_estudio',
        '1',
        'Rango maximo de anios en busqueda',
        'INTEGER',
        NULL,
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        6,
        'chatbot_habilitado',
        'true',
        'Activar o desactivar chatbot global',
        'BOOLEAN',
        NULL,
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    ),
    (
        7,
        'idioma_predeterminado',
        'es',
        'Idioma por defecto del sistema',
        'STRING',
        NULL,
        1,
        '2026-04-22 00:47:40',
        '2026-04-22 00:47:40'
    );
/*!40000 ALTER TABLE `parametros_sistema` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `preferencias_notificacion`
--

DROP TABLE IF EXISTS `preferencias_notificacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `preferencias_notificacion` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint NOT NULL,
    `email_habilitado` tinyint(1) NOT NULL DEFAULT '1',
    `push_habilitado` tinyint(1) NOT NULL DEFAULT '0',
    `sms_habilitado` tinyint(1) NOT NULL DEFAULT '0',
    `notif_nuevas_normativas` tinyint(1) NOT NULL DEFAULT '1',
    `notif_nuevos_estudios` tinyint(1) NOT NULL DEFAULT '1',
    `notif_cambios_seguidos` tinyint(1) NOT NULL DEFAULT '1',
    `notif_mensajes_admin` tinyint(1) NOT NULL DEFAULT '1',
    `notif_digesto_semanal` tinyint(1) NOT NULL DEFAULT '0',
    `frecuencia_email` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DIARIA' COMMENT 'INMEDIATA, DIARIA, SEMANAL, NUNCA',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `usuario_id` (`usuario_id`),
    KEY `idx_preferencias_usuario` (`usuario_id`),
    CONSTRAINT `fk_preferencias_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `preferencias_notificacion`
--

LOCK TABLES `preferencias_notificacion` WRITE;
/*!40000 ALTER TABLE `preferencias_notificacion` DISABLE KEYS */
;
/*!40000 ALTER TABLE `preferencias_notificacion` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `roles` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `nombre` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SUPERADMIN, ADMINISTRADOR, SOCIO, VISUALIZADOR',
    `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `nombre` (`nombre`)
) ENGINE = InnoDB AUTO_INCREMENT = 5 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Catalogo de roles del sistema';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */
;
INSERT INTO
    `roles`
VALUES (
        1,
        'SUPERADMIN',
        'Administrador supremo PUCP - auditoria y gestion global',
        1,
        '2026-04-22 00:47:39'
    ),
    (
        2,
        'ADMINISTRADOR',
        'Gestion de usuarios, documentos y reportes',
        1,
        '2026-04-22 00:47:39'
    ),
    (
        3,
        'SOCIO',
        'Acceso completo al repositorio - lectura y descarga',
        1,
        '2026-04-22 00:47:39'
    ),
    (
        4,
        'VISUALIZADOR',
        'Acceso limitado - solo lectura de documentos publicos',
        1,
        '2026-04-22 00:47:39'
    );
/*!40000 ALTER TABLE `roles` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `sesiones_impersonacion`
--

DROP TABLE IF EXISTS `sesiones_impersonacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `sesiones_impersonacion` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `superadmin_id` bigint NOT NULL,
    `usuario_objetivo_id` bigint NOT NULL,
    `motivo` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
    `fecha_inicio` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `fecha_fin` timestamp NULL DEFAULT NULL,
    `activa` tinyint(1) NOT NULL DEFAULT '1',
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_impersonacion_sa` (`superadmin_id`),
    KEY `idx_impersonacion_objetivo` (`usuario_objetivo_id`),
    KEY `idx_impersonacion_activa` (`activa`),
    CONSTRAINT `fk_impersonacion_sa` FOREIGN KEY (`superadmin_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_impersonacion_user` FOREIGN KEY (`usuario_objetivo_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Auditoria de impersonaciones del SuperAdmin';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `sesiones_impersonacion`
--

LOCK TABLES `sesiones_impersonacion` WRITE;
/*!40000 ALTER TABLE `sesiones_impersonacion` DISABLE KEYS */
;
/*!40000 ALTER TABLE `sesiones_impersonacion` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `sesiones_usuario`
--

DROP TABLE IF EXISTS `sesiones_usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `sesiones_usuario` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint NOT NULL,
    `token_sesion` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `activa` tinyint(1) NOT NULL DEFAULT '1',
    `fecha_expiracion` timestamp NOT NULL,
    `ultimo_acceso` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `token_sesion` (`token_sesion`),
    KEY `idx_sesiones_usuario` (`usuario_id`),
    KEY `idx_sesiones_activa` (`activa`),
    CONSTRAINT `fk_sesiones_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Sesiones activas del usuario';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `sesiones_usuario`
--

LOCK TABLES `sesiones_usuario` WRITE;
/*!40000 ALTER TABLE `sesiones_usuario` DISABLE KEYS */
;
/*!40000 ALTER TABLE `sesiones_usuario` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `solicitudes_registro`
--

DROP TABLE IF EXISTS `solicitudes_registro`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `solicitudes_registro` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `nombre_completo` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
    `dni` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
    `correo_corporativo` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
    `empresa_nombre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
    `estado` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, APROBADA, RECHAZADA',
    `revisada_por_id` bigint DEFAULT NULL,
    `motivo_rechazo` text COLLATE utf8mb4_unicode_ci,
    `fecha_resolucion` timestamp NULL DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_solicitudes_revisor` (`revisada_por_id`),
    KEY `idx_solicitudes_estado` (`estado`),
    KEY `idx_solicitudes_correo` (`correo_corporativo`),
    CONSTRAINT `fk_solicitudes_revisor` FOREIGN KEY (`revisada_por_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Cola de aprobacion para registro de nuevos usuarios';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `solicitudes_registro`
--

LOCK TABLES `solicitudes_registro` WRITE;
/*!40000 ALTER TABLE `solicitudes_registro` DISABLE KEYS */
;
/*!40000 ALTER TABLE `solicitudes_registro` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `solicitudes_respaldo`
--

DROP TABLE IF EXISTS `solicitudes_respaldo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `solicitudes_respaldo` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `solicitado_por_id` bigint NOT NULL,
    `tipo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMPLETO' COMMENT 'COMPLETO, PARCIAL_USUARIOS, PARCIAL_DOCUMENTOS, PARCIAL_LOGS',
    `estado` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, EN_PROCESO, COMPLETADO, FALLIDO',
    `storage_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `tamanio_bytes` bigint DEFAULT NULL,
    `notas` text COLLATE utf8mb4_unicode_ci,
    `fecha_completado` timestamp NULL DEFAULT NULL,
    `es_backup_pre_restauracion` tinyint(1) NOT NULL DEFAULT '0',
    `restaurado_por_id` bigint DEFAULT NULL,
    `fecha_restauracion` timestamp NULL DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_respaldos_estado` (`estado`),
    KEY `idx_respaldos_solicitante` (`solicitado_por_id`),
    KEY `idx_respaldos_fecha` (`created_at`),
    CONSTRAINT `fk_respaldo_solicitante` FOREIGN KEY (`solicitado_por_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_respaldo_restaurado_por` FOREIGN KEY (`restaurado_por_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Solicitudes de respaldo para auditoria de backups';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `solicitudes_respaldo`
--

LOCK TABLES `solicitudes_respaldo` WRITE;
/*!40000 ALTER TABLE `solicitudes_respaldo` DISABLE KEYS */
;
/*!40000 ALTER TABLE `solicitudes_respaldo` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `programaciones_backup`
--

DROP TABLE IF EXISTS `programaciones_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `programaciones_backup` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tipo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMPLETO' COMMENT 'COMPLETO, PARCIAL_USUARIOS, PARCIAL_DOCUMENTOS, PARCIAL_LOGS',
    `frecuencia` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'DIARIA, SEMANAL, MENSUAL',
    `hora_ejecucion` time NOT NULL DEFAULT '02:00:00',
    `dia_semana` tinyint DEFAULT NULL COMMENT '0=Domingo, 1=Lunes... (SEMANAL)',
    `dia_mes` tinyint DEFAULT NULL COMMENT '1-31 (MENSUAL)',
    `habilitado` tinyint(1) NOT NULL DEFAULT '1',
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Programaciones de backups automaticos';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `programaciones_backup`
--

LOCK TABLES `programaciones_backup` WRITE;
/*!40000 ALTER TABLE `programaciones_backup` DISABLE KEYS */;
/*!40000 ALTER TABLE `programaciones_backup` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tematicas`
--

DROP TABLE IF EXISTS `tematicas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `tematicas` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `codigo` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'EC, GR, EE, REP, OTRO',
    `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `color_hex` varchar(7) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#95A5A6' COMMENT 'Color distintivo para la UI',
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `codigo` (`codigo`)
) ENGINE = InnoDB AUTO_INCREMENT = 6 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tematicas con color distintivo';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `tematicas`
--

LOCK TABLES `tematicas` WRITE;
/*!40000 ALTER TABLE `tematicas` DISABLE KEYS */
;
INSERT INTO
    `tematicas`
VALUES (
        1,
        'EC',
        'Economia Circular',
        '#27AE60',
        1,
        '2026-04-22 00:47:39'
    ),
    (
        2,
        'GR',
        'Gestion de Residuos',
        '#3498DB',
        1,
        '2026-04-22 00:47:39'
    ),
    (
        3,
        'EE',
        'Envases y Embalajes',
        '#F39C12',
        1,
        '2026-04-22 00:47:39'
    ),
    (
        4,
        'REP',
        'Responsabilidad Extendida del Productor',
        '#E74C3C',
        1,
        '2026-04-22 00:47:39'
    ),
    (
        5,
        'OTRO',
        'Otros',
        '#95A5A6',
        1,
        '2026-04-22 00:47:39'
    );
/*!40000 ALTER TABLE `tematicas` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `tipos_normativa`
--

DROP TABLE IF EXISTS `tipos_normativa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `tipos_normativa` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `nombre` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `nombre` (`nombre`)
) ENGINE = InnoDB AUTO_INCREMENT = 7 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `tipos_normativa`
--

LOCK TABLES `tipos_normativa` WRITE;
/*!40000 ALTER TABLE `tipos_normativa` DISABLE KEYS */
;
INSERT INTO
    `tipos_normativa`
VALUES (
        1,
        'Ley',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        2,
        'Reglamento',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        3,
        'NTP / Norma tecnica',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        4,
        'Directiva',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        5,
        'Hoja de Ruta',
        1,
        '2026-04-22 00:47:40'
    ),
    (
        6,
        'Resolucion',
        1,
        '2026-04-22 00:47:40'
    );
/*!40000 ALTER TABLE `tipos_normativa` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `tokens_acceso`
--

DROP TABLE IF EXISTS `tokens_acceso`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `tokens_acceso` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint DEFAULT NULL,
    `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `tipo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MAGIC_LINK, RECOVERY, VERIFICACION',
    `usado` tinyint(1) NOT NULL DEFAULT '0',
    `fecha_expiracion` timestamp NOT NULL,
    `ip_solicitante` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `token` (`token`),
    KEY `idx_tokens_usuario` (`usuario_id`),
    KEY `idx_tokens_expiracion` (`fecha_expiracion`),
    CONSTRAINT `fk_tokens_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tokens de corta vida para magic links y recuperacion';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `tokens_acceso`
--

LOCK TABLES `tokens_acceso` WRITE;
/*!40000 ALTER TABLE `tokens_acceso` DISABLE KEYS */
;
/*!40000 ALTER TABLE `tokens_acceso` ENABLE KEYS */
;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */
;
/*!50503 SET character_set_client = utf8mb4 */
;
CREATE TABLE `usuarios` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `nombre_completo` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
    `dni` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
    `correo_corporativo` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
    `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `cargo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `foto_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `rol_id` bigint NOT NULL,
    `empresa_id` bigint DEFAULT NULL,
    `intentos_fallidos` tinyint NOT NULL DEFAULT '0',
    `cuenta_bloqueada` tinyint(1) NOT NULL DEFAULT '0',
    `fecha_bloqueo` timestamp NULL DEFAULT NULL,
    `ip_bloqueada` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `tfa_activo` tinyint(1) NOT NULL DEFAULT '0',
    `tfa_secret` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `estado_cuenta` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, ACTIVO, INACTIVO, BLOQUEADO',
    `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
    `ultimo_login` timestamp NULL DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `dni` (`dni`),
    UNIQUE KEY `correo_corporativo` (`correo_corporativo`),
    KEY `idx_usuarios_correo` (`correo_corporativo`),
    KEY `idx_usuarios_dni` (`dni`),
    KEY `idx_usuarios_rol` (`rol_id`),
    KEY `idx_usuarios_empresa` (`empresa_id`),
    KEY `idx_usuarios_estado` (`estado_cuenta`),
    CONSTRAINT `fk_usuarios_empresa` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_usuarios_rol` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Usuarios del sistema';
/*!40101 SET character_set_client = @saved_cs_client */
;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */
;
-- Contraseñas cifradas con BCrypt (rounds=10):
-- admin@reciclame.com        → admin123
-- superadmin@reciclame.com   → super123
-- socio@reciclame.com        → socio123
-- visualizador@reciclame.com → vis123
-- pendiente@reciclame.com    → password
-- a20202132 / sergiomeneses893 → 910e0084
-- sergiomeneses3 / jairo     → 123456
INSERT INTO
    `usuarios`
VALUES (
        1,
        'Ad',
        '71234567',
        'admin@reciclame.com',
        '$2b$10$DcAuL30y3G9uCzGpCgCKle3rC.HIymnCb1f7ovjnB3n5daZ8ME9i6',
        'Administrador del Sistema',
        NULL,
        2,
        1,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-22 00:47:45',
        '2026-04-26 05:02:02'
    ),
    (
        2,
        'Super',
        '71234568',
        'superadmin@reciclame.com',
        '$2b$10$Z.Lpe/Y4VlVwYtr6yofN.uHqthFdVRCjIrOiPjvrM2TgfZN2qVvwS',
        'SuperAdmin',
        NULL,
        1,
        1,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-22 00:47:45',
        '2026-04-24 06:40:50'
    ),
    (
        3,
        'Socio Demo',
        '71234569',
        'socio@reciclame.com',
        '$2b$10$iwDe8HO.ZuKkW6HFUIAhe.2Q2mHEZSXYRWNesfOQh0gFHVG1l4.qq',
        'Socio Empresarial',
        NULL,
        3,
        1,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-22 00:47:45',
        '2026-04-22 00:47:45'
    ),
    (
        4,
        'Visualiza',
        '71234570',
        'visualizador@reciclame.com',
        '$2b$10$fwCz/8MjgF52Kyxl/RGKCekZ4KKcgw59MEQ0FUHPuwL9pX2DqMX7G',
        'Visualizador',
        NULL,
        4,
        NULL,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-22 00:47:45',
        '2026-04-26 05:07:35'
    ),
    (
        5,
        'Usuario Pendiente',
        '71234571',
        'pendiente@reciclame.com',
        '$2b$10$Vdk3J3.eHUOpQVK.HXqwWe36aOP9C/KOuGlEvAkL1OxHufVUpSfXq',
        'Nuevo Usuario',
        NULL,
        3,
        2,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-22 00:47:45',
        '2026-04-22 19:08:43'
    ),
    (
        6,
        'harold_decadencia',
        '21421434',
        'a20202132@pucp.edu.pe',
        '$2b$10$esXYyXRukydU//HhrNSdOeDl84CjDhQQ9rkMJOX5ZpmRriKs5yRdm',
        'desarrollador frontend',
        NULL,
        3,
        2,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-22 19:08:11',
        '2026-04-26 05:21:39'
    ),
    (
        7,
        'esddgfhbj',
        '23242442',
        'sergiomeneses893@gmail.com',
        '$2b$10$esXYyXRukydU//HhrNSdOeDl84CjDhQQ9rkMJOX5ZpmRriKs5yRdm',
        'desarrollador frontend',
        NULL,
        4,
        4,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-22 19:17:38',
        '2026-04-26 05:13:52'
    ),
    (
        8,
        'sergio',
        '21421432',
        'sergiomeneses3@gmail.com',
        '$2b$10$LS2TBRF5aKvUcdYQP3Vk6OoP8tmSwK6j5S8tMq3X0udamR3zFAnGu',
        'desarolador',
        NULL,
        2,
        4,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-24 06:35:21',
        '2026-04-24 06:35:21'
    ),
    (
        9,
        'jairitocuadros',
        '25254254',
        'jairo@pucp.edu.pe',
        '$2b$10$LS2TBRF5aKvUcdYQP3Vk6OoP8tmSwK6j5S8tMq3X0udamR3zFAnGu',
        'IT',
        NULL,
        1,
        1,
        0,
        0,
        NULL,
        0,
        NULL,
        'ACTIVO',
        1,
        NULL,
        '2026-04-24 06:53:32',
        '2026-04-24 06:53:32'
    );
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */
;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */
;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */
;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */
;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */
;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */
;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */
;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */
;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */
;

--
-- Mi Unidad (carpetas e ítems por socio) — ejecutar si aún no existen
--
-- ============================================================
-- UNIDAD DE USUARIO (My Documents personal)
-- ============================================================

-- Tabla de carpetas personales del usuario
CREATE TABLE IF NOT EXISTS `unidad_carpetas` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `usuario_id` bigint NOT NULL,
    `parent_id` bigint DEFAULT NULL,
    `nombre` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `sort_order` int NOT NULL DEFAULT '0',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_unidad_carpeta_usuario` (`usuario_id`),
    KEY `idx_unidad_carpeta_parent` (`parent_id`),
    CONSTRAINT `fk_unidad_carpeta_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_unidad_carpeta_parent` FOREIGN KEY (`parent_id`) REFERENCES `unidad_carpetas` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tabla de items (documentos guardados en carpetas personales)
CREATE TABLE IF NOT EXISTS unidad_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    carpeta_id BIGINT DEFAULT NULL,
    documento_tipo VARCHAR(20) NOT NULL COMMENT 'NORMATIVA, ESTUDIO',
    documento_id BIGINT NOT NULL,
    titulo_presentacion VARCHAR(500) DEFAULT NULL COMMENT 'Texto mostrado al usuario (ej. desde la tabla normativo/estudios)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_unidad_item_usuario (usuario_id),
    KEY idx_unidad_item_carpeta (carpeta_id),
    KEY idx_unidad_item_doc (documento_tipo, documento_id),
    CONSTRAINT fk_unidad_item_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_unidad_item_carpeta FOREIGN KEY (carpeta_id) REFERENCES unidad_carpetas (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    usado TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_prt_token (token),
    INDEX idx_prt_usuario (usuario_id),
    INDEX idx_prt_expires (expires_at),
    CONSTRAINT fk_prt_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

SET SQL_SAFE_UPDATES = 0;

-- ============================================================
-- Migración: reemplazar URLs de GCP Storage por URLs de MinIO
-- Ejecutar UNA SOLA VEZ contra la BD live cuando MinIO sea el
-- almacenamiento activo y los archivos ya existan en el bucket.
-- ============================================================

-- IMPORTANTE: los archivos referenciados deben existir en el
-- bucket MinIO (reciclame-docs) antes de ejecutar este script.
-- Si los archivos no fueron migrados de GCP a MinIO, el visor
-- mostrará error al intentar abrirlos.

UPDATE normativas
SET
    archivo_url =
REPLACE (
        archivo_url,
        'https://storage.googleapis.com/reciclame-docs/',
        'http://localhost:9000/reciclame-docs/'
    )
WHERE
    archivo_url LIKE 'https://storage.googleapis.com/reciclame-docs/%';

-- Verificación: mostrar registros actualizados
SELECT 'estudios' AS tabla, id, titulo, archivo_url
FROM estudios
WHERE
    archivo_url LIKE 'http://localhost:9000/reciclame-docs/%'
UNION ALL
SELECT 'normativas', id, titulo, archivo_url
FROM normativas
WHERE
    archivo_url LIKE 'http://localhost:9000/reciclame-docs/%';

-- ============================================================
-- TABLAS CREADAS EN SESIÓN: Configuración de vistas por rol
-- Fecha: 2026-05-13
-- ============================================================

-- Tabla: configuracion_ui_rol
-- Almacena qué columnas y módulos del sidebar son visibles
-- para cada rol (SOCIO, VISUALIZADOR), configurado por Admin/SuperAdmin.
--
-- Claves posibles por rol:
--   columnas_normativas  → columnas visibles en Repositorio Normativo
--   columnas_estudios    → columnas visibles en Estudios Recíclame
--   modulos_sidebar      → módulos del sidebar visibles
--
-- El campo 'valor' guarda los nombres separados por coma.
-- Ej: "nombre,codigo,anio,tipo,acciones"
--
-- Si no existe fila para un rol+clave, el servicio devuelve
-- los valores por defecto definidos en ConfiguracionUiRolService.

CREATE TABLE IF NOT EXISTS `reciclame_db`.`configuracion_ui_rol` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `rol`            VARCHAR(50)   NOT NULL COMMENT 'Rol afectado: SOCIO | VISUALIZADOR',
  `clave`          VARCHAR(100)  NOT NULL COMMENT 'columnas_normativas | columnas_estudios | modulos_sidebar',
  `valor`          TEXT          NULL     COMMENT 'Valores separados por coma',
  `actualizado_at` DATETIME(6)   NULL     COMMENT 'Última modificación',
  PRIMARY KEY (`id`),
  CONSTRAINT `uq_ui_rol_clave` UNIQUE (`rol`, `clave`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Configuración de columnas y módulos visibles por rol de usuario';

SET SQL_SAFE_UPDATES = 1;

-- ============================================================
-- CHATBOT ANALYTICS — Reciclame
-- Fecha: 2026-06-18
-- Persistencia y métricas del chatbot n8n "La Buena Cajita"
-- ============================================================

-- 1) Extender conversaciones_chatbot con métricas agregadas
ALTER TABLE `conversaciones_chatbot`
    ADD COLUMN `titulo`              VARCHAR(200)  NULL          COMMENT 'Titulo corto generado para la conversacion',
    ADD COLUMN `total_mensajes`      INT           NOT NULL DEFAULT 0 COMMENT 'Cache de total de mensajes para listados rapidos',
    ADD COLUMN `duracion_total_ms`   BIGINT        NOT NULL DEFAULT 0 COMMENT 'Suma de latencias USER->BOT de la conversacion',
    ADD COLUMN `ultimo_mensaje_at`   TIMESTAMP     NULL          COMMENT 'Fecha del ultimo mensaje',
    ADD COLUMN `ip_address`          VARCHAR(45)   NULL          COMMENT 'IP del usuario al iniciar la conversacion',
    ADD COLUMN `user_agent`          VARCHAR(500)  NULL          COMMENT 'User-Agent del navegador',
    ADD COLUMN `cerrada_por_usuario` TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '1 = el usuario cerro la conversacion manualmente',
    ADD INDEX `idx_conversaciones_ultimo` (`ultimo_mensaje_at`);

-- 2) Extender mensajes_chatbot con métricas y trazabilidad
ALTER TABLE `mensajes_chatbot`
    ADD COLUMN `duracion_ms`           BIGINT        NULL          COMMENT 'Latencia entre pregunta del usuario y respuesta del bot (ms)',
    ADD COLUMN `tokens_estimados`      INT           NULL          COMMENT 'Tokens aprox. consumidos (entrada+salida)',
    ADD COLUMN `modelo`                VARCHAR(60)   NULL          COMMENT 'Modelo LLM usado (ej: minimax, gpt-4o-mini)',
    ADD COLUMN `es_error`              TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '1 = hubo error al generar la respuesta',
    ADD COLUMN `mensaje_error`         VARCHAR(500)  NULL,
    ADD COLUMN `sql_ejecutado`         TEXT          NULL          COMMENT 'SQL ejecutado por el agente (truncado a 4000 chars)',
    ADD COLUMN `filas_devueltas`       INT           NULL          COMMENT 'Cantidad de filas devueltas por la tool SQL',
    ADD COLUMN `metadata`              JSON          NULL          COMMENT 'Datos extra del callback (intents, scores, etc.)',
    ADD COLUMN `ip_address`            VARCHAR(45)   NULL,
    ADD INDEX `idx_mensajes_rol_fecha` (`rol`, `created_at`),
    ADD INDEX `idx_mensajes_error`     (`es_error`, `created_at`);

-- 3) Parametros de sistema nuevos para el chatbot
INSERT IGNORE INTO `parametros_sistema`
    (`clave`, `valor`, `descripcion`, `tipo_dato`, `estado_activo`)
VALUES
    ('chatbot_callback_enabled', 'true',
     'Habilita el endpoint de callback para registrar conversaciones', 'BOOLEAN', 1),
    ('chatbot_max_mensajes_por_sesion', '100',
     'Limite de mensajes por conversacion antes de forzar cierre', 'INTEGER', 1),
    ('chatbot_retencion_dias', '365',
     'Dias de retencion del historial de conversaciones', 'INTEGER', 1),
    ('chatbot_tiempo_promedio_objetivo_ms', '8000',
     'SLA objetivo de latencia del bot (ms)', 'INTEGER', 1);

-- 4) Feedback opcional del usuario (calificacion de la respuesta)
CREATE TABLE IF NOT EXISTS `chatbot_feedback` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `mensaje_id`      BIGINT        NOT NULL,
    `usuario_id`      BIGINT        NOT NULL,
    `calificacion`    TINYINT       NOT NULL             COMMENT '1=util, -1=no util',
    `comentario`      VARCHAR(500)  NULL,
    `created_at`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_feedback_mensaje_usuario` (`mensaje_id`, `usuario_id`),
    KEY `idx_feedback_usuario` (`usuario_id`),
    KEY `idx_feedback_calificacion` (`calificacion`),
    CONSTRAINT `fk_feedback_mensaje` FOREIGN KEY (`mensaje_id`)
        REFERENCES `mensajes_chatbot` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_feedback_usuario` FOREIGN KEY (`usuario_id`)
        REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Feedback (👍 / 👎) sobre respuestas del chatbot';

-- Migración: ampliar archivo_tipo_mime en normativas de varchar(50) a varchar(255)
-- Necesario para soportar tipos MIME de archivos Word (.docx)
ALTER TABLE normativas
    MODIFY COLUMN archivo_tipo_mime VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL;

-- ============================================================
-- CHAT INTERNO EN TIEMPO REAL (Centrifugo)
-- Fecha: 2026-06-20
-- Chat 1:1 entre Socios y Administradores. SUPERADMIN no participa.
-- ============================================================

DROP TABLE IF EXISTS `chat_mensajes`;
DROP TABLE IF EXISTS `chat_conversaciones`;

CREATE TABLE `chat_conversaciones` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `usuario_a_id`      BIGINT       NOT NULL,
    `usuario_b_id`      BIGINT       NOT NULL,
    `ultimo_mensaje`    TEXT         NULL,
    `ultimo_mensaje_at` TIMESTAMP    NULL,
    `created_at`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_par` (`usuario_a_id`, `usuario_b_id`),
    KEY `idx_chat_a` (`usuario_a_id`),
    KEY `idx_chat_b` (`usuario_b_id`),
    CONSTRAINT `fk_chat_usra` FOREIGN KEY (`usuario_a_id`)
        REFERENCES `usuarios` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_usrb` FOREIGN KEY (`usuario_b_id`)
        REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Conversaciones 1:1 del chat interno (Centrifugo)';

CREATE TABLE `chat_mensajes` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `conversacion_id` BIGINT       NOT NULL,
    `remitente_id`    BIGINT       NOT NULL,
    `contenido`       TEXT         NOT NULL,
    `leido`           TINYINT(1)   NOT NULL DEFAULT 0,
    `leido_at`        TIMESTAMP    NULL,
    `created_at`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_msg_conv` (`conversacion_id`, `created_at`),
    KEY `idx_msg_remitente` (`remitente_id`),
    CONSTRAINT `fk_msg_conv` FOREIGN KEY (`conversacion_id`)
        REFERENCES `chat_conversaciones` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_msg_remitente` FOREIGN KEY (`remitente_id`)
        REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Mensajes del chat interno';


-- ─────────────────────────────────────────────────────────────
-- Migración: agregar columnas de auditoría VirusTotal a las
-- tablas `normativas` y `estudios`.
--
-- Aplicar contra RDS una sola vez antes del próximo deploy:
--   mysql -h reciclame-db.cfowgekco09o.us-east-1.rds.amazonaws.com \
--         -u admin -p reciclame_db < db/reciclame_db.sql
--
-- Es idempotente: las columnas sólo se agregan si no existen.
-- ─────────────────────────────────────────────────────────────

-- 1) normativas
SET @tbl := 'normativas';
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name   = @tbl
        AND column_name  = 'vt_sha256') = 0,
    'ALTER TABLE normativas
        ADD COLUMN vt_sha256           VARCHAR(64)  NULL AFTER archivo_tipo_mime,
        ADD COLUMN vt_verdict          VARCHAR(20)  NULL AFTER vt_sha256,
        ADD COLUMN vt_malicious_engines INT         NULL AFTER vt_verdict,
        ADD COLUMN vt_suspicious_engines INT        NULL AFTER vt_malicious_engines,
        ADD COLUMN vt_total_engines    INT          NULL AFTER vt_suspicious_engines,
        ADD COLUMN vt_skipped          TINYINT(1)   NULL AFTER vt_total_engines,
        ADD COLUMN vt_scanned_at       TIMESTAMP    NULL AFTER vt_skipped',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Índice auxiliar: permite filtrar/listar rápidamente las normativas con
-- un SHA-256 conocido (útil para deduplicación futura).
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name   = @tbl
        AND index_name   = 'idx_normativas_vt_sha256') = 0,
    'CREATE INDEX idx_normativas_vt_sha256 ON normativas (vt_sha256)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) estudios
SET @tbl := 'estudios';
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name   = @tbl
        AND column_name  = 'vt_sha256') = 0,
    'ALTER TABLE estudios
        ADD COLUMN vt_sha256           VARCHAR(64)  NULL AFTER archivo_tipo_mime,
        ADD COLUMN vt_verdict          VARCHAR(20)  NULL AFTER vt_sha256,
        ADD COLUMN vt_malicious_engines INT         NULL AFTER vt_verdict,
        ADD COLUMN vt_suspicious_engines INT        NULL AFTER vt_malicious_engines,
        ADD COLUMN vt_total_engines    INT          NULL AFTER vt_suspicious_engines,
        ADD COLUMN vt_skipped          TINYINT(1)   NULL AFTER vt_total_engines,
        ADD COLUMN vt_scanned_at       TIMESTAMP    NULL AFTER vt_skipped',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name   = @tbl
        AND index_name   = 'idx_estudios_vt_sha256') = 0,
    'CREATE INDEX idx_estudios_vt_sha256 ON estudios (vt_sha256)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;