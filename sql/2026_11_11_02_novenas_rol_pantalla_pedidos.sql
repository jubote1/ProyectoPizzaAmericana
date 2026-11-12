-- ===========================================================================
-- Script: 2026_11_11_02_novenas_rol_pantalla_pedidos.sql
-- Propósito: Configurar el acceso a la pantalla GestionVentaEmpresarialNovenas.html
-- en ProyectoPizzaAmericana para los roles operativos de Contact Center:
--   1. Rol Operario (Asesor de Contact Center - idrol = 1)
--   2. Rol Líder Administrativo (Líder de Contact Center - idrol = 2)
--   3. Rol Administrador PQRS / Contact Center (idrol = 3)
--   6. Rol Superadministrador (idrol = 6)
--
-- Ubicación en Menú: Módulo 1 (Pedidos), para que esté disponible directamente
-- para los asesores operativos sin requerir permisos especiales ni acceso a CRM.
-- ===========================================================================

-- 1. Actualización en pizza_prueba (desarrollo local)
UPDATE pizza_prueba.pantalla 
SET idmodulo = 1, orden = 15, activo = 'S'
WHERE url_html = 'GestionVentaEmpresarialNovenas.html';

INSERT IGNORE INTO pizza_prueba.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizza_prueba.rol r
CROSS JOIN pizza_prueba.pantalla p
WHERE p.url_html = 'GestionVentaEmpresarialNovenas.html'
  AND r.idrol IN (1, 2, 3, 6);

-- 2. Actualización en pizzaamericana (producción 172.19.0.25 / localhost)
-- Asegurar tablas de seguridad si aplican en el esquema de producción:
CREATE TABLE IF NOT EXISTS pizzaamericana.rol LIKE pizza_prueba.rol;
INSERT IGNORE INTO pizzaamericana.rol SELECT * FROM pizza_prueba.rol;

CREATE TABLE IF NOT EXISTS pizzaamericana.menu_modulo LIKE pizza_prueba.menu_modulo;
INSERT IGNORE INTO pizzaamericana.menu_modulo SELECT * FROM pizza_prueba.menu_modulo;

CREATE TABLE IF NOT EXISTS pizzaamericana.pantalla LIKE pizza_prueba.pantalla;
INSERT IGNORE INTO pizzaamericana.pantalla SELECT * FROM pizza_prueba.pantalla;

CREATE TABLE IF NOT EXISTS pizzaamericana.rol_pantalla LIKE pizza_prueba.rol_pantalla;
INSERT IGNORE INTO pizzaamericana.rol_pantalla SELECT * FROM pizza_prueba.rol_pantalla;

CREATE TABLE IF NOT EXISTS pizzaamericana.usuario_rol LIKE pizza_prueba.usuario_rol;
INSERT IGNORE INTO pizzaamericana.usuario_rol SELECT * FROM pizza_prueba.usuario_rol;

-- Mover a módulo Pedidos (idmodulo = 1) y asignar a roles 1, 2, 3, 6
UPDATE pizzaamericana.pantalla 
SET idmodulo = 1, orden = 15, activo = 'S'
WHERE url_html = 'GestionVentaEmpresarialNovenas.html';

INSERT IGNORE INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizzaamericana.rol r
CROSS JOIN pizzaamericana.pantalla p
WHERE p.url_html = 'GestionVentaEmpresarialNovenas.html'
  AND r.idrol IN (1, 2, 3, 6);
