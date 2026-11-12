-- ==============================================================================
-- MIGRACIÓN SQL: CAMPAÑA VENTA EMPRESARIAL Y NOVENAS NAVIDEÑAS (25 AÑOS)
-- Fecha: 2026-11-09
-- Descripción:
--   Crea la estructura de tablas para la precarga de clientes objetivo (leads con >= 3 pizzas),
--   la bitácora de llamadas y seguimiento para el Contact Center, y la regla de atribución de
--   ventas corporativas para pedidos que contengan 3 o más pizzas.
-- ==============================================================================

USE tiendaamericana;

-- 1. Tabla de clientes precargados de la campaña (Leads)
CREATE TABLE IF NOT EXISTS campana_novenas_lead (
    idlead INT AUTO_INCREMENT PRIMARY KEY,
    idcliente INT NOT NULL,
    nombre_cliente VARCHAR(150) NOT NULL,
    empresa VARCHAR(150) DEFAULT NULL,
    telefono VARCHAR(50) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    direccion VARCHAR(200) DEFAULT NULL,
    observacion_cliente TEXT DEFAULT NULL,
    clasificacion VARCHAR(80) NOT NULL,
    criterio_clasificacion VARCHAR(255) DEFAULT NULL,
    total_pizzas_historico INT DEFAULT 0,
    total_pedidos_historico INT DEFAULT 0,
    total_comprado_historico DOUBLE DEFAULT 0,
    ultima_fecha_compra DATE DEFAULT NULL,
    factura_electronica VARCHAR(2) DEFAULT 'NO',
    nit_cedula VARCHAR(50) DEFAULT NULL,
    razon_social VARCHAR(150) DEFAULT NULL,
    pizzas_preferidas TEXT DEFAULT NULL,
    estado_gestion VARCHAR(50) DEFAULT 'PENDIENTE',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cliente (idcliente),
    INDEX idx_telefono (telefono),
    INDEX idx_email (email),
    INDEX idx_estado (estado_gestion),
    INDEX idx_clasif (clasificacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabla de bitácora y contactos del Contact Center
CREATE TABLE IF NOT EXISTS campana_novenas_bitacora (
    idbitacora INT AUTO_INCREMENT PRIMARY KEY,
    idlead INT NOT NULL,
    idcliente INT NOT NULL,
    usuario_asesor VARCHAR(50) NOT NULL,
    fecha_contacto DATETIME NOT NULL,
    estado_contacto VARCHAR(50) NOT NULL, -- 'INTERESADO', 'APARTADO_5%', 'VOLVER_A_LLAMAR', 'NO_INTERESADO', 'NO_CONTESTA'
    fecha_tentativa_evento DATE DEFAULT NULL,
    pizzas_estimadas INT DEFAULT 0,
    observacion TEXT NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lead (idlead),
    INDEX idx_cliente (idcliente),
    INDEX idx_asesor (usuario_asesor),
    INDEX idx_fecha (fecha_contacto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Registro de la pantalla en el catálogo de seguridad y menús
USE pizza_prueba;

INSERT INTO pizza_prueba.pantalla (nombre, idmodulo, url_html, orden, activo) 
SELECT 'Campaña Novenas y Venta Empresarial', 7, 'GestionVentaEmpresarialNovenas.html', 45, 'S'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM pizza_prueba.pantalla WHERE url_html = 'GestionVentaEmpresarialNovenas.html'
);

INSERT IGNORE INTO pizza_prueba.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizza_prueba.rol r
CROSS JOIN pizza_prueba.pantalla p 
WHERE p.url_html = 'GestionVentaEmpresarialNovenas.html';

USE tiendaamericana;
