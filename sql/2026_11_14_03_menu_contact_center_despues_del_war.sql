-- ============================================================================
-- 2026_11_14_03_menu_contact_center_despues_del_war.sql
--
-- *** CORRER SOLO DESPUES DE DESPLEGAR EL WAR DEL CENTRAL ***
--
-- Arma el menu propio del Contact Center en pos_central.configuracion_menu.
--
-- POR QUE EL ORDEN IMPORTA. El MotorCatalogo anterior lee configuracion_menu
-- SIN filtrar por canal. Si estas filas existen y el WAR viejo sigue arriba, la
-- pantalla devuelve los dos menus pegados: se corrio por error el 2026-11-14 y
-- el servicio contesto 167 items con 78 productos repetidos. Se revirtio en el
-- momento. El MotorCatalogo nuevo filtra `cm.canal = ?` y pide CONTACT por
-- defecto.
--
-- Comprobacion rapida despues de correrlo, que debe dar 80 items y 0 repetidos:
--   curl "http://172.19.0.25:8080/ProyectoPizzaAmericana/api/motor/catalogo?accion=menu"
--
-- Requiere que 2026_11_14_01 ya este corrido (es el que crea la columna canal).
-- Idempotente: borra y vuelve a armar el menu CONTACT en cada corrida.
-- ============================================================================

-- El menu del Contact Center arranca como una copia del de las tiendas, menos
-- los botones que pidieron quitar en la Certificacion #1.
--
-- EL PRECIO EMPLEADO (472-478) SE QUEDA. El documento pedia esconderlo, pero
-- Juan David lo corrigio el 2026-11-14: es justo una de las cosas que quieren
-- poder hacer desde el Contact Center. Lo que si sale es la Porcion Pizza
-- Empleado (420), que es otro producto.
DELETE FROM pos_central.configuracion_menu WHERE canal = 'CONTACT';

INSERT INTO pos_central.configuracion_menu (canal, multimenu, menu, fila, columna, idproducto)
SELECT 'CONTACT', multimenu, menu, fila, columna, idproducto
  FROM pos_central.configuracion_menu
 WHERE canal = 'TIENDA'
   AND idproducto NOT IN (
       -- Bebidas que el Contact Center no vende
       531,  -- Agua Brisa Limon P280
       532,  -- Agua Brisa Manzana P280
       533,  -- Agua Brisa Maracuya P280
       615,  -- Soda Miche Limon
       506,  -- Soda Saborizada
       530,  -- Agua Manantial P600
       -- Porciones, que no se despachan por domicilio
       420,  -- Porcion Pizza Empleado
       414,  -- Porcion Pizza
       416,  -- Porcion 4000 - Emp Temporal
       -- Pendiente de que Mercadeo confirme si siguen vigentes. Por ahora solo
       -- salen del Contact Center; en las tiendas siguen.
       584,  -- Pizza MD 50% Hagamos Vaca
       574   -- Pizza MD Crunch
   );

-- ---------------------------------------------------------------------------
-- Comprobacion
-- ---------------------------------------------------------------------------
SELECT canal, COUNT(*) AS casillas FROM pos_central.configuracion_menu GROUP BY canal ORDER BY canal;

SELECT nm.nombre AS seccion,
       SUM(cm.canal = 'TIENDA')  AS en_tienda,
       SUM(cm.canal = 'CONTACT') AS en_contact
  FROM pos_central.configuracion_menu cm
  JOIN pos_central.nombre_multimenu nm ON nm.idmultimenu = cm.multimenu
 GROUP BY nm.nombre ORDER BY nm.nombre;

SELECT p.idproducto, p.descripcion AS fuera_del_contact
  FROM pos_central.producto p
 WHERE p.idproducto IN (531,532,533,615,506,530,420,414,416,584,574)
 ORDER BY p.idproducto;
