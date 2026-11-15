-- ============================================================================
-- 2026_11_14_01_menu_por_canal_contact_center.sql
--
-- EL MENU DEJA DE SER UNO SOLO: se separa por CANAL.
--
-- Hasta hoy pos_central.configuracion_menu y la configuracion_menu de cada
-- tienda tenian las MISMAS 92 casillas -se comparo y el diff salia vacio por los
-- dos lados-, y ninguna de las dos tenia columna de canal ni de tienda. Por eso
-- "quitarlo solo del Contact Center" era imposible: borrar la fila se lo quitaba
-- tambien a las once tiendas.
--
-- Decidido por Juan David el 2026-11-14: manejar un canal en configuracion_menu
-- y armarle su propio menu al Contact Center. Las tablas excepcion_tienda y
-- producto_horario_canal se dejan quietas: esas son para diferenciar un
-- PRODUCTO por canal, que es otro problema.
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente: el menu CONTACT se borra y
-- se vuelve a armar en cada corrida.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. La columna de canal
-- ---------------------------------------------------------------------------
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='pos_central' AND TABLE_NAME='configuracion_menu'
                   AND COLUMN_NAME='canal');
SET @sql := IF(@existe = 0,
  'ALTER TABLE pos_central.configuracion_menu
     ADD COLUMN canal VARCHAR(20) NOT NULL DEFAULT ''TIENDA''
     COMMENT ''TIENDA = el menu de las tiendas fisicas. CONTACT = el del Contact Center.''
     AFTER idconfiguracionmenu',
  'SELECT ''la columna canal ya existe'' AS nota');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Lo que ya habia es el menu de las tiendas.
UPDATE pos_central.configuracion_menu SET canal = 'TIENDA' WHERE canal IS NULL OR canal = '';

-- ---------------------------------------------------------------------------
-- 2. La casilla 13, que tenia DOS productos encima
-- ---------------------------------------------------------------------------
-- La Lasagna Mixta (2) y la Pizza MD (8) estaban las dos en multimenu 1,
-- casilla 13 (fila 2, columna 0). No es solo un duplicado cosmetico: el POS de
-- la tienda guarda la cuadricula en confMenu[fila][columna], asi que una de las
-- dos NO SE PINTA -gana la que llegue de ultima, y la consulta no trae ORDER BY,
-- o sea que ni siquiera es estable-. Ademas la Lasagna estaba repetida en la
-- casilla 24.
--
-- Se quita la Lasagna de la casilla 13: la Pizza MD recupera su casilla y la
-- Lasagna queda una sola vez, en la 24. Decidido con Juan David el 2026-11-14.
DELETE FROM pos_central.configuracion_menu
 WHERE canal = 'TIENDA' AND multimenu = 1 AND menu = 13 AND idproducto = 2;

-- El numero de casilla tiene que ser fila*6 + columna + 1, como dice el
-- comentario de PanelMenuPOS. Tres filas lo tenian corrido en 6 -una fila
-- arriba-: Soda Miche Limon (23 en vez de 29), Soda Saborizada (29 en vez de
-- 35) y Deditos Cumple (1 en vez de 5). No se nota en pantalla porque el POS
-- dibuja con confMenu[fila][columna] y no con `menu`, pero dejaba a Deditos
-- Cumple con el mismo numero de casilla que el XL Combo Futbolero.
UPDATE pos_central.configuracion_menu
   SET menu = (fila * 6 + columna + 1)
 WHERE menu <> (fila * 6 + columna + 1);

-- Que no vuelva a pasar: una casilla, un producto, por canal. La llave va por
-- (fila, columna), que es lo que de verdad identifica la casilla en el POS.
SET @tieneLlave := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA='pos_central' AND TABLE_NAME='configuracion_menu'
                       AND INDEX_NAME='uk_canal_casilla');
SET @sql := IF(@tieneLlave = 0,
  'ALTER TABLE pos_central.configuracion_menu
     ADD UNIQUE KEY uk_canal_casilla (canal, multimenu, fila, columna)',
  'SELECT ''la llave unica ya existe'' AS nota');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ---------------------------------------------------------------------------
-- 3. El menu del Contact Center va APARTE, en 2026_11_14_03
-- ---------------------------------------------------------------------------
-- No se arma aqui a proposito. El MotorCatalogo que esta desplegado hoy lee la
-- tabla SIN filtrar por canal, asi que en cuanto existen las filas CONTACT la
-- pantalla devuelve los dos menus pegados: se probo y salieron 167 items con 78
-- productos repetidos. Primero se despliega el WAR, despues se arma el menu.
-- Ver sql/2026_11_14_03_menu_contact_center_despues_del_war.sql.

-- ---------------------------------------------------------------------------
-- 4. Comprobacion
-- ---------------------------------------------------------------------------
SELECT canal, COUNT(*) AS casillas FROM pos_central.configuracion_menu GROUP BY canal ORDER BY canal;

SELECT 'casillas con mas de un producto' AS revision, COUNT(*) AS cuantas FROM (
  SELECT canal, multimenu, menu FROM pos_central.configuracion_menu
   GROUP BY canal, multimenu, menu HAVING COUNT(*) > 1) x;

SELECT 'productos repetidos en el mismo canal' AS revision, COUNT(*) AS cuantos FROM (
  SELECT canal, idproducto FROM pos_central.configuracion_menu
   GROUP BY canal, idproducto HAVING COUNT(*) > 1) y;

SELECT p.idproducto, p.descripcion, 'sigue en TIENDA, fuera del Contact' AS nota
  FROM pos_central.producto p
 WHERE p.idproducto IN (531,532,533,615,506,530,420,414,416,584,574)
 ORDER BY p.idproducto;
