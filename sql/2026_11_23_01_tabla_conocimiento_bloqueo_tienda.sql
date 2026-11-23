-- =============================================================================
-- Migracion: Tabla de Conocimiento Semanal de Bloqueos de Tienda
-- Proyecto: ProyectoPizzaAmericana / Servicios
-- Fecha: 2026-11-23
-- Proposito: Almacenar la analitica operacional y predictiva de cada bloqueo de tienda:
--            volumen de pedidos (pixeles), domiciliarios activos, ratio de presion,
--            patrones detectados y sugerencias para prevencion predictiva.
-- =============================================================================

CREATE TABLE IF NOT EXISTS pizzaamericana.conocimiento_bloqueo_tienda (
    idconocimiento INT AUTO_INCREMENT PRIMARY KEY,
    idlogbloqueo INT DEFAULT 0,
    idtienda INT NOT NULL,
    nombre_tienda VARCHAR(100) NOT NULL,
    periodo_semana VARCHAR(50) NOT NULL,
    fecha_bloqueo DATETIME NOT NULL,
    fecha_desbloqueo DATETIME NULL,
    minutos_bloqueo INT NOT NULL DEFAULT 0,
    horas_bloqueo DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    motivo VARCHAR(255) NULL,
    observacion TEXT NULL,
    debloqueo_en VARCHAR(50) NULL,
    dia_semana VARCHAR(30) NULL,
    es_fin_semana TINYINT(1) DEFAULT 0,
    franja_horaria VARCHAR(50) NULL,
    hora_del_dia INT DEFAULT 0,
    pedidos_activos_pixel INT DEFAULT 0,
    pizzas_en_proceso INT DEFAULT 0,
    domiciliarios_activos INT DEFAULT 0,
    ratio_pedidos_domiciliario DECIMAL(6,2) DEFAULT 0.00,
    pedidos_domicilio_activos INT DEFAULT 0,
    pedidos_mostrador_activos INT DEFAULT 0,
    venta_hora_promedio DECIMAL(12,2) DEFAULT 0.00,
    venta_estimada_perdida DECIMAL(12,2) DEFAULT 0.00,
    patron_detectado VARCHAR(100) NULL,
    nivel_criticidad VARCHAR(20) DEFAULT 'MEDIA',
    sugerencia_predictiva TEXT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_periodo (periodo_semana),
    KEY idx_tienda_fecha (idtienda, fecha_bloqueo),
    KEY idx_patron (patron_detectado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
