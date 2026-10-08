-- ---------------------------------------------------------------------------
-- Las sodas entran a Venta Integral
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente.
--
-- No hace falta tocar codigo: el catalogo de Venta Integral es parametrizable
-- y una categoria nueva es una fila con sus items.
--
-- QUE SE CUENTA: 506 Y 615, Y NADA MAS
--
-- En la tienda hay seis productos con "Soda" en el nombre, pero solo dos se
-- venden:
--
--     506  Soda Saborizada        $10.000   63 lineas la semana del 21 al 27
--     615  Soda Miche Limon        $6.000   45 lineas
--     507  Soda Saborizada Segunda 50%      sin movimiento
--     508  Soda Saborizada Pizza Entera     sin movimiento
--     595  Nueva Soda Frutos Rojos          sin movimiento
--     596  Nueva Soda Frutos Amarillos      sin movimiento
--
-- Y SOBRE TODO: LOS SABORES NO SE CUENTAN
--
-- El sabor no es un producto vendido, es la respuesta a una pregunta forzada
-- (506 tiene idpreguntaforzada1 = 49). En detalle_pedido el sabor queda como
-- una linea aparte que CUELGA de la soda -iddetalle_pedido_master apunta a la
-- linea de la soda- y con valortotal = 0:
--
--     linea 1596700  master 0        506 Soda Saborizada   $10.000
--     linea 1596701  master 1596700  514 Maracuya               $0
--     linea 1596702  master 1596700  511 Borde Michelado        $0
--
-- Si se metieran 505 Cereza, 511 Borde Michelado o 514 Maracuya a la
-- categoria, una sola soda contaria como tres. Por eso van SOLO 506 y 615.
--
-- Verificado contra las once tiendas, semana del 2026-09-21 al 2026-09-27:
-- 506 y 615 aparecen SIEMPRE como linea propia (master = 0) y NUNCA colgadas
-- de otra. No hay doble conteo entre las dos.
--
-- SOLO AMBITO TIENDA
--
-- Por contact center no se venden sodas: los productos 506 y 615 ni siquiera
-- existen en el catalogo del central. Se deja sin items de CONTACTCENTER, y el
-- proceso lo maneja bien -VentaIntegralResumenDAO devuelve 0 cuando la lista
-- viene vacia, no arma un "in ()" invalido-.
--
-- SUMA_CANTIDAD Y NO CONTEO_FILAS
--
-- Las categorias de producto que ya existian cuentan FILAS. Para una bebida
-- eso subcuenta: dos sodas en una misma linea son dos sodas. Medido esa
-- semana, San Antonio tuvo 10 lineas y 12 unidades de la 506. Se cuentan
-- unidades.
--
-- EXCLUYE ANULADOS, A DIFERENCIA DE LAS OTRAS
--
-- Estofadas, Adiciones y Deditos NO excluyen anulados: es una inconsistencia
-- heredada de las consultas viejas que se preservo a proposito para no cambiar
-- cifras historicas en silencio. Esta categoria nace hoy y no tiene historia
-- que respetar, asi que se hace lo correcto: una soda anulada no es una venta.
-- Si se prefiere que quede igual a las otras, se cambia desde la pantalla de
-- Categorias sin tocar la base.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LA CATEGORIA
-- ===========================================================================

INSERT INTO pizzaamericana.venta_integral_categoria
       (nombre, abreviatura, tipodato, medicion_tienda, excluye_anulados_tienda,
        filtro_estacion_tienda, medicion_cc, activo, orden, fechacreacion)
SELECT 'Sodas', 'SODA', 'PRODUCTO', 'SUMA_CANTIDAD', 'S',
       '%serv%', 'CONTEO_PRODUCTO', 'S', 80, NOW()
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.venta_integral_categoria
                    WHERE nombre = 'Sodas');

-- ===========================================================================
-- 2. LOS DOS PRODUCTOS, SOLO EN TIENDA
-- ===========================================================================

INSERT INTO pizzaamericana.venta_integral_categoria_item (idcategoria, ambito, idvalor, activo)
SELECT c.idcategoria, 'TIENDA', 506, 'S'
  FROM pizzaamericana.venta_integral_categoria c
 WHERE c.nombre = 'Sodas'
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.venta_integral_categoria_item i
                    WHERE i.idcategoria = c.idcategoria AND i.ambito = 'TIENDA' AND i.idvalor = 506);

INSERT INTO pizzaamericana.venta_integral_categoria_item (idcategoria, ambito, idvalor, activo)
SELECT c.idcategoria, 'TIENDA', 615, 'S'
  FROM pizzaamericana.venta_integral_categoria c
 WHERE c.nombre = 'Sodas'
   AND NOT EXISTS (SELECT 1 FROM pizzaamericana.venta_integral_categoria_item i
                    WHERE i.idcategoria = c.idcategoria AND i.ambito = 'TIENDA' AND i.idvalor = 615);

-- ===========================================================================
-- 3. COMO QUEDO
-- ===========================================================================

SELECT c.orden, c.nombre, c.abreviatura, c.tipodato, c.medicion_tienda,
       c.excluye_anulados_tienda AS excl_anul, c.filtro_estacion_tienda AS estacion,
       c.medicion_cc, c.activo,
       GROUP_CONCAT(CONCAT(i.ambito, ':', i.idvalor) ORDER BY i.ambito, i.idvalor) AS items
  FROM pizzaamericana.venta_integral_categoria c
  LEFT JOIN pizzaamericana.venta_integral_categoria_item i
         ON i.idcategoria = c.idcategoria AND i.activo = 'S'
 GROUP BY c.idcategoria, c.orden, c.nombre, c.abreviatura, c.tipodato, c.medicion_tienda,
          c.excluye_anulados_tienda, c.filtro_estacion_tienda, c.medicion_cc, c.activo
 ORDER BY c.orden;
