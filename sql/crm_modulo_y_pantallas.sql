-- ---------------------------------------------------------------------------
-- EL CRM COMO AREA APARTE
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Crea el modulo,
-- registra las dos pantallas nuevas, mueve al modulo las de clientes que ya
-- existian, y agrega dos indices al maestro de personas.
--
-- POR QUE UN MODULO APARTE
--
-- Lo de clientes estaba repartido en los tres modulos existentes:
--
--   Administracion de Clientes        -> Funciones
--   Administracion de Clientes Full   -> Parametrizacion
--   Consulta Puntos Cliente           -> Monitoreo
--   Segmentacion Cliente              -> Monitoreo
--
-- Quien atiende a un cliente tenia que saberse en cual de los tres estaba cada
-- cosa. Son la misma tarea vista desde tres menus distintos.
--
-- ESTO NO CAMBIA NADA HOY
--
-- El menu que se ve en produccion es el estatico -MenuAdm.html y Menu.html-, no
-- esta tabla: MenuDinamico.html existe pero todavia nadie lo usa. Mover una
-- pantalla de modulo aqui no se la quita a nadie. Deja el catalogo ordenado
-- para cuando el menu dinamico se prenda.
--
-- En el menu estatico las pantallas SIGUEN donde estaban, y ademas aparece la
-- opcion CRM. Nadie tiene que reaprender donde queda lo que ya usa.
--
-- QUE NO SE MUEVE, Y ES DECISION SUYA
--
--   Consulta Pedidos CRM BOT  se deja en Funciones: es una consulta de pedidos,
--                             aunque el bot sea un canal de cliente.
--   Administrar Ofertas       se deja en Parametrizacion: es parametrizacion de
--                             verdad, y sacarla de ahi si cambia la costumbre
--                             de quien la usa.
--
-- Las dos quedan enlazadas desde el menu del CRM igual, porque una pantalla
-- puede estar en dos menus. Si se quieren mover de verdad, es una linea.
--
-- ES IDEMPOTENTE: INSERT IGNORE sobre url_html, que es UNIQUE, y los UPDATE
-- dejan un valor fijo.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. EL MODULO
-- ===========================================================================

INSERT IGNORE INTO pizzaamericana.menu_modulo (idmodulo, nombre, orden, idmodulo_padre, activo)
VALUES (4, 'CRM', 40, NULL, 'S');

-- ===========================================================================
-- 2. LAS PANTALLAS NUEVAS
-- ===========================================================================

INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('CRM - Inicio', 4, 'CRM.html', 10, 'S');

INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('Vista 360 de una persona', 4, 'Persona360.html', 20, 'S');

-- ===========================================================================
-- 3. LAS QUE YA EXISTIAN, AL MODULO DE CRM
--
-- Se mueven por url_html y no por idpantalla: el id depende de en que orden se
-- creo cada una y no es el mismo en otra instalacion.
-- ===========================================================================

UPDATE pizzaamericana.pantalla SET idmodulo = 4, orden = 30
 WHERE url_html = 'CRMClientesFull.html';

UPDATE pizzaamericana.pantalla SET idmodulo = 4, orden = 40
 WHERE url_html = 'CRMClientes.html';

UPDATE pizzaamericana.pantalla SET idmodulo = 4, orden = 50
 WHERE url_html = 'ConsultaPuntosCliente.html';

UPDATE pizzaamericana.pantalla SET idmodulo = 4, orden = 60
 WHERE url_html = 'segmentacionCliente.html';

-- ===========================================================================
-- 4. LOS INDICES PARA BUSCAR PERSONAS
--
-- crm.persona solo tenia indice por celular. La pantalla tambien busca por
-- nombre y por correo, y sin indice cada busqueda recorre las 457 mil filas.
--
-- Por nombre la busqueda es por el COMIENZO -LIKE 'juan%'-, que es lo unico
-- que puede usar un indice. Buscar por el medio obligaria a recorrer la tabla
-- entera en cada tecla, y por eso la pantalla dice que busca por el comienzo
-- en vez de prometer algo que no puede sostener.
-- ===========================================================================

SET @existe := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA='crm' AND TABLE_NAME='persona'
                   AND INDEX_NAME='idx_persona_nombre');
SET @sql := IF(@existe = 0,
  'ALTER TABLE crm.persona ADD INDEX idx_persona_nombre (nombre)',
  'SELECT ''persona ya tiene indice por nombre'' AS paso4a');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA='crm' AND TABLE_NAME='persona'
                   AND INDEX_NAME='idx_persona_apellido');
SET @sql := IF(@existe = 0,
  'ALTER TABLE crm.persona ADD INDEX idx_persona_apellido (apellido)',
  'SELECT ''persona ya tiene indice por apellido'' AS paso4b');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA='crm' AND TABLE_NAME='persona'
                   AND INDEX_NAME='idx_persona_email');
SET @sql := IF(@existe = 0,
  'ALTER TABLE crm.persona ADD INDEX idx_persona_email (email)',
  'SELECT ''persona ya tiene indice por correo'' AS paso4c');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT m.idmodulo, m.nombre AS modulo, p.orden, p.nombre AS pantalla, p.url_html
  FROM pizzaamericana.pantalla p
  JOIN pizzaamericana.menu_modulo m ON m.idmodulo = p.idmodulo
 WHERE p.idmodulo = 4
 ORDER BY p.orden;

-- Los permisos NO se tocan: quien ve que pantalla se decide desde
-- "Asignar Pantallas a Rol", como con las demas pantallas nuevas.
