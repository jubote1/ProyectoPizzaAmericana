-- ---------------------------------------------------------------------------
-- La redencion manual de puntos entra al catalogo de pantallas
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- NO CREA NI CAMBIA NINGUNA TABLA
--
-- fidelizacion_redencion ya tiene todo lo que hace falta -idtienda, usuario,
-- origen, estado, motivo-, asi que la redencion manual no necesita columnas
-- nuevas. Se marca con origen = 'ADM', al lado de los 'POS' y 'CC' que ya
-- existen. La columna es char(3) y por eso la abreviatura.
--
-- LO UNICO QUE SE AGREGA ES LA FILA DEL CATALOGO DE SEGURIDAD
--
-- Hoy la pantalla se protege exigiendo sesion y que el usuario sea
-- administrador -son 13 de los 127 activos-, que es como se separa hoy el menu
-- administrativo. Se registra igual en pantalla para que el dia que se pase de
-- administrador a roles de verdad, esta ya este en la lista y no haya que
-- acordarse de agregarla. Ver seguridad_roles_catalogo.sql.
--
-- No se le da a ningun rol todavia: mientras el control sea el de
-- administrador, darle la pantalla a un rol no cambiaria nada y crearia la
-- ilusion de que si.
-- ---------------------------------------------------------------------------

INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Redencion Manual de Puntos', m.idmodulo, 'RedencionManualPuntos.html', 55, 'S'
  FROM pizzaamericana.menu_modulo m
 WHERE m.nombre = 'CRM'
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla ya
                    WHERE ya.url_html = 'RedencionManualPuntos.html');

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT p.idpantalla, p.nombre, p.url_html, m.nombre AS modulo, p.orden, p.activo
  FROM pizzaamericana.pantalla p
  JOIN pizzaamericana.menu_modulo m ON m.idmodulo = p.idmodulo
 WHERE p.url_html IN ('ConsultaPuntosCliente.html', 'RedencionManualPuntos.html')
 ORDER BY p.orden;

-- Las redenciones por origen. Despues de la primera redencion manual, 'ADM'
-- tiene que aparecer aqui.
SELECT IFNULL(NULLIF(origen, ''), '(sin origen)') AS origen, COUNT(*) AS redenciones
  FROM pizzaamericana.fidelizacion_redencion
 GROUP BY IFNULL(NULLIF(origen, ''), '(sin origen)')
 ORDER BY redenciones DESC;
