-- ============================================================================
-- 2026_11_13_03_eventos_especiales_planificador.sql
--
-- EVENTOS ESPECIALES: menu propio en el central, y el planificador de eventos.
--
-- 1) La tabla donde se planilla cada evento (pizzaamericana.evento_especial).
--    Va en pizzaamericana y NO en tiendaamericana, porque es ahi donde apunta
--    ConexionBaseDatos.obtenerConexionBDPrincipal(), que es la conexion que usan
--    los DAO del central. La campana de novenas quedo en tiendaamericana con una
--    vista encima justamente porque su tabla ya existia; una tabla nueva no tiene
--    por que repetir ese rodeo.
--
-- 2) El modulo "Eventos Especiales" del menu dinamico, con sus tres pantallas:
--    las novenas (que estaba suelta en Clientes y CRM), el planificador nuevo y
--    la ayuda del contact center. Se le asigna a TODOS los roles: es informacion
--    que la operacion entera necesita ver.
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. El planillado de eventos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pizzaamericana.evento_especial (
    idevento          INT AUTO_INCREMENT PRIMARY KEY,
    tipo_evento       VARCHAR(40)  NOT NULL DEFAULT 'NOVENA',
    nombre_evento     VARCHAR(150) DEFAULT NULL,
    nombre_cliente    VARCHAR(150) NOT NULL,
    empresa           VARCHAR(150) DEFAULT NULL,
    telefono          VARCHAR(50)  NOT NULL,
    email             VARCHAR(100) DEFAULT NULL,
    idcliente         INT          DEFAULT NULL,
    idlead            INT          DEFAULT NULL,
    fecha_evento      DATE         NOT NULL,
    hora_evento       TIME         NOT NULL DEFAULT '19:00:00',
    cantidad_pizzas   INT          NOT NULL DEFAULT 0,
    detalle_pedido    TEXT         DEFAULT NULL,
    idtienda          INT          DEFAULT NULL,
    direccion_entrega VARCHAR(250) DEFAULT NULL,
    valor_estimado    DOUBLE       NOT NULL DEFAULT 0,
    anticipo          DOUBLE       NOT NULL DEFAULT 0,
    estado            VARCHAR(30)  NOT NULL DEFAULT 'TENTATIVO',
    observacion       TEXT         DEFAULT NULL,
    usuario_registra  VARCHAR(50)  NOT NULL,
    creado_en         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_modifica  VARCHAR(50)  DEFAULT NULL,
    modificado_en     DATETIME     DEFAULT NULL,
    INDEX idx_ev_fecha  (fecha_evento),
    INDEX idx_ev_estado (estado),
    INDEX idx_ev_tel    (telefono),
    INDEX idx_ev_tienda (idtienda),
    INDEX idx_ev_lead   (idlead)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Planificador de eventos especiales: novenas, empresariales y celebraciones. Una fila por evento planillado.';

-- Estados: TENTATIVO (hablado, sin plata) -> APARTADO (pago el 20%) ->
-- CONFIRMADO (cerrado con la tienda) -> ENTREGADO, o CANCELADO.
-- Tipos: NOVENA, EMPRESARIAL, CUMPLEANOS, INSTITUCIONAL, OTRO.

-- ---------------------------------------------------------------------------
-- 2. El modulo del menu y sus pantallas
-- ---------------------------------------------------------------------------
-- Los modulos 10, 11 y 12 estaban libres (hay 1-9 y 13-15 en uso).
INSERT INTO pizzaamericana.menu_modulo (idmodulo, nombre, orden, idmodulo_padre, activo)
VALUES (10, 'Eventos Especiales', 15, NULL, 'S')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), orden = VALUES(orden), activo = 'S';

-- La de novenas ya existe (idpantalla 95, estaba en Clientes y CRM): se muda.
UPDATE pizzaamericana.pantalla
   SET idmodulo = 10, nombre = 'Novenas de Fin de Ano y 25 Anos', orden = 10, activo = 'S'
 WHERE url_html = 'GestionVentaEmpresarialNovenas.html';

INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Planificador de Eventos', 10, 'PlanificadorEventos.html', 20, 'S'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'PlanificadorEventos.html');

INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Ayuda: como se manejan los eventos', 10, 'AyudaEventosEspeciales.html', 30, 'S'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'AyudaEventosEspeciales.html');

UPDATE pizzaamericana.pantalla
   SET idmodulo = 10, orden = 20, activo = 'S'
 WHERE url_html = 'PlanificadorEventos.html';

UPDATE pizzaamericana.pantalla
   SET idmodulo = 10, orden = 30, activo = 'S'
 WHERE url_html = 'AyudaEventosEspeciales.html';

-- A todos los roles: el menu arma el arbol desde rol_pantalla, asi que el modulo
-- aparece solo en cuanto el rol tiene una pantalla adentro.
INSERT IGNORE INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
  FROM pizzaamericana.rol r
 CROSS JOIN pizzaamericana.pantalla p
 WHERE p.url_html IN ('GestionVentaEmpresarialNovenas.html',
                      'PlanificadorEventos.html',
                      'AyudaEventosEspeciales.html');

-- ---------------------------------------------------------------------------
-- 3. Comprobacion
-- ---------------------------------------------------------------------------
SELECT m.nombre AS modulo, p.idpantalla, p.nombre AS pantalla, p.url_html, p.orden,
       (SELECT COUNT(*) FROM pizzaamericana.rol_pantalla rp WHERE rp.idpantalla = p.idpantalla) AS roles
  FROM pizzaamericana.pantalla p
  JOIN pizzaamericana.menu_modulo m ON m.idmodulo = p.idmodulo
 WHERE p.idmodulo = 10
 ORDER BY p.orden;

SELECT COUNT(*) AS columnas_evento_especial
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'evento_especial';
