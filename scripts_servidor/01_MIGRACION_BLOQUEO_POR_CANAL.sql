-- =====================================================================
-- Script de Migración: Soporte para Bloqueo de Tiendas por Canal
-- Proyecto: Pizza Americana
-- Fecha: 2026-11-23
-- Descripción:
--   Permite que los bloqueos de tiendas puedan ser generales ('TODOS')
--   o segmentados por uno o varios canales específicos:
--   - 'TODOS': Bloqueo general en todos los canales y POS
--   - 'TIENDA_VIRTUAL': Bloquea solo la página web / e-commerce
--   - 'BOT': Bloquea solo el bot de WhatsApp (Kommo CRM)
--   - 'CALL_CENTER': Bloquea toma de pedidos telefónica
--   - 'POS': Bloquea solo pedidos en mostrador / salón
-- =====================================================================

-- 1. Modificar tabla tienda_bloqueada
-- Agregamos la columna canal con 'TODOS' para conservar compatibilidad con datos existentes
ALTER TABLE tienda_bloqueada 
ADD COLUMN canal VARCHAR(30) NOT NULL DEFAULT 'TODOS' AFTER idtienda;

-- 2. Modificar la clave primaria a compuesta (idtienda, canal)
-- Permite que una tienda pueda tener múltiples canales bloqueados de forma independiente
ALTER TABLE tienda_bloqueada 
DROP PRIMARY KEY,
ADD PRIMARY KEY (idtienda, canal);

-- 3. Modificar tabla log_bloqueo_tienda para auditar el canal afectado
ALTER TABLE log_bloqueo_tienda 
ADD COLUMN canal VARCHAR(30) NOT NULL DEFAULT 'TODOS' AFTER idtienda;

-- 4. Índice para optimizar la consulta rápida de disponibilidad por canal
CREATE INDEX idx_tienda_bloqueada_canal ON tienda_bloqueada (idtienda, canal);

-- 5. (Opcional - Futuro / IA) Modificar tabla conocimiento_bloqueo_tienda si se desea registrar canal analizado
-- Nota: La tabla conocimiento_bloqueo_tienda la alimenta un proceso externo/batch analítico.
-- Al agregar esta columna con DEFAULT 'TODOS', no afecta los registros actuales.
-- ALTER TABLE conocimiento_bloqueo_tienda 
-- ADD COLUMN canal VARCHAR(30) NOT NULL DEFAULT 'TODOS' AFTER idtienda;
