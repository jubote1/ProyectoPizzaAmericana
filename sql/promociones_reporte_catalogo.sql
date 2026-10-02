-- ---------------------------------------------------------------------------
-- EL REPORTE DIARIO DE PROMOCIONES SE VUELVE PARAMETRIZABLE
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- No apaga ni toca el reporte viejo. Siembra el catalogo del nuevo, para que
-- los dos puedan correr en paralelo unos dias y se puedan comparar antes de
-- jubilar el actual.
--
-- ===========================================================================
-- POR QUE HAY QUE CAMBIARLO
-- ===========================================================================
--
-- ReportePromocionesDiarias son 806 lineas con TRECE bloques copiados y
-- pegados, cada uno con su promocion quemada en el codigo. Eso tiene tres
-- consecuencias, y las tres se pueden medir:
--
-- 1. ESTA MIRANDO LAS PROMOCIONES EQUIVOCADAS
--
--    Vigila las excepciones 11, 20, 24, 26, 27, 29, 34, 39, 40 y 41. De esas,
--    SIETE no vendieron una sola linea entre julio y septiembre de 2026. Y las
--    tres mas vendidas de la compania no aparecen:
--
--        XL Insuperable   5.085 lineas    no esta en el reporte
--        GD Insuperable   2.893 lineas    no esta en el reporte
--        MD Insuperable   2.600 lineas    no esta en el reporte
--
--    Son 10.578 lineas en tres meses que nadie ve. Paso porque agregar una
--    promocion exige tocar codigo, y nadie lo hizo.
--
-- 2. CUENTA DIVIDIENDO PLATA ENTRE UN PRECIO QUEMADO
--
--    Doce precios escritos en el codigo -17495, 19990, 21990, 31990, 9900,
--    19900, 9995, 26500, 34900, 49990, 49990, 9900- y la cantidad sale de
--    SUM(valorunitario) / precio. Si una promocion cambia de precio, la
--    cantidad queda mal y nadie se entera.
--
-- 3. CUENTA COSAS QUE NO SON PIZZAS
--
--    Los productos se buscan con descripcion LIKE '%texto%'. El patron
--    '%Rappi%' incluye "Tarifa Servicio RAPPI" y "Adicional RAPPI", y la tabla
--    del correo dice "Cant Pizzas".
--
-- Y de paso: datamart.estadistica_promocion tiene 15.572 filas de historia que
-- se dejaron de escribir el 2025-05-20. El correo manda numeros todos los dias
-- y no guarda ninguno, asi que nunca hay con que comparar.
--
-- ===========================================================================
-- LA DECISION DE DISENO QUE LO SIMPLIFICA TODO
-- ===========================================================================
--
-- El reporte viejo mide por dos lados: los productos en la base de cada tienda
-- y el idexcepcion en la base del central. Eso obliga a mantener dos listas y
-- abre la puerta a contar dos veces.
--
-- No hace falta. SE MIDE SOLO EN LAS TIENDAS, POR idproducto.
--
-- La base de la tienda tiene TODOS los canales. Verificado con el Combo
-- Insuperable XL en Manrique, julio a septiembre: 591 unidades repartidas en
-- servidor1 -el mostrador-, CONTACT-CENTER, CRM, APP, TIENDA VIRTUAL y
-- BOT DOMI-Prop. Las de plataforma aparecen con estacion DIDI DOMI-Plat o
-- RAPPI DOMI-Plat.
--
-- Medir en la tienda ademas RECUPERA EL MOSTRADOR, que es el 41% de los
-- pedidos que el central no ve. El canal sale de pedido.estacion, asi que el
-- reporte puede abrir por canal sin consultar dos bases.
--
-- Y el catalogo de producto es el mismo en todas: se comparo la firma MD5 de
-- los 19 productos sembrados aqui en las once tiendas y diez dieron identico
-- -Manrique Piloto no respondio esa tarde, no es una diferencia de datos-.
--
-- ===========================================================================
-- QUE SE SIEMBRA
-- ===========================================================================
--
-- Las trece promociones que de verdad vendieron entre julio y septiembre de
-- 2026, con sus productos reales. Las que el reporte viejo vigilaba y no
-- venden no se siembran: el catalogo arranca limpio.
--
-- NO se siembran los productos en cero pesos -Deditos Promo, Madurito Promo,
-- las de Puntos-: son acompanantes regalados o redenciones, no venta de
-- promocion. Deditos Promo llevaba 1.303 unidades y 0 pesos en tres meses, y
-- mezclarlo con las demas haria ver la lista encabezada por algo que no
-- factura.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. EL CATALOGO
-- ===========================================================================

CREATE TABLE IF NOT EXISTS pizzaamericana.promocion_reporte (
  idpromo    INT          NOT NULL AUTO_INCREMENT,
  nombre     VARCHAR(80)  NOT NULL,
  plataforma CHAR(1)      NOT NULL DEFAULT 'N' COMMENT 'S cuando la vende DiDi o Rappi',
  activo     CHAR(1)      NOT NULL DEFAULT 'S',
  orden      INT          NOT NULL DEFAULT 0,
  creada_en  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (idpromo),
  UNIQUE KEY uk_promocion_reporte_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Promociones que vigila el reporte diario. Una promocion es una fila';

CREATE TABLE IF NOT EXISTS pizzaamericana.promocion_reporte_item (
  iditem     INT     NOT NULL AUTO_INCREMENT,
  idpromo    INT     NOT NULL,
  idproducto INT     NOT NULL COMMENT 'Producto en la base de CADA TIENDA, no del central',
  activo     CHAR(1) NOT NULL DEFAULT 'S',
  PRIMARY KEY (iditem),
  UNIQUE KEY uk_promocion_item (idpromo, idproducto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Productos de tienda que componen cada promocion';

-- ===========================================================================
-- 2. LA HISTORIA, QUE ES LO QUE PERMITE COMPARAR
--
-- Sin esto el reporte no puede decir si hoy fue bueno o malo. Se guarda por
-- dia, promocion, tienda y canal, con unidades Y plata: una promocion que
-- vende tres de sesenta mil no es igual a una que vende tres de veinte mil.
--
-- La llave unica hace que reprocesar un dia no duplique.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS datamart.promocion_dia (
  fecha     DATE          NOT NULL,
  idpromo   INT           NOT NULL,
  idtienda  INT           NOT NULL,
  canal     VARCHAR(20)   NOT NULL COMMENT 'MOSTRADOR, CONTACT, CRM, APP, VIRTUAL, DIDI, RAPPI, OTRO',
  unidades  DOUBLE        NOT NULL DEFAULT 0,
  valor     DOUBLE        NOT NULL DEFAULT 0,
  grabado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (fecha, idpromo, idtienda, canal),
  KEY idx_promocion_dia_promo (idpromo, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Venta diaria de promociones por tienda y canal';

-- ===========================================================================
-- 3. LAS PROMOCIONES
--
-- Los numeros entre parentesis son las unidades vendidas en Manrique entre el
-- 1 de julio y el 30 de septiembre de 2026, para que se vea de donde salio
-- cada una y por que esta en ese orden.
-- ===========================================================================

INSERT INTO pizzaamericana.promocion_reporte (nombre, plataforma, activo, orden)
SELECT * FROM (
          SELECT 'Combo Insuperable' AS n,            'N' AS p, 'S' AS a,  10 AS o   -- 1.185
UNION ALL SELECT 'XL Combo Futbolero',                'N', 'S',  20              -- 157
UNION ALL SELECT 'Pizza Estofada',                    'N', 'S',  30              -- 196
UNION ALL SELECT 'Mediana Max',                       'N', 'S',  40              -- 121
UNION ALL SELECT 'Grande Max',                        'N', 'S',  50              -- 56
UNION ALL SELECT 'Plan Portero',                      'N', 'S',  60              -- 1
UNION ALL SELECT 'Promo 2 Lasagnas con bebida',       'N', 'S',  70              -- 30
UNION ALL SELECT 'Combo Familiar GD DiDi',            'S', 'S',  80              -- 132
UNION ALL SELECT 'Pizzeta Promo DiDi',                'S', 'S',  90              -- 131
UNION ALL SELECT 'Grande DiDi',                       'S', 'S', 100              -- 63
UNION ALL SELECT 'Pizzeta DiDi',                      'S', 'S', 110              -- 27
UNION ALL SELECT 'Promo 2 Lasagnas con bebida DiDi',  'S', 'S', 120              -- 5
UNION ALL SELECT 'Pizza con gaseosa Rappi',           'S', 'S', 130              -- 18
) nuevas
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.promocion_reporte ya WHERE ya.nombre = nuevas.n);

-- ===========================================================================
-- 4. LOS PRODUCTOS DE CADA UNA
-- ===========================================================================

INSERT INTO pizzaamericana.promocion_reporte_item (idpromo, idproducto, activo)
SELECT c.idpromo, t.idproducto, 'S'
  FROM (
            SELECT 'Combo Insuperable' AS nombre, 480 AS idproducto   -- Combo Insuperable XL
  UNION ALL SELECT 'Combo Insuperable', 481                           -- Combo Insuperable GD
  UNION ALL SELECT 'Combo Insuperable', 482                           -- Combo Insuperable MD
  UNION ALL SELECT 'XL Combo Futbolero', 444
  UNION ALL SELECT 'Pizza Estofada', 469                              -- Pizza MD Estofada
  UNION ALL SELECT 'Pizza Estofada', 470                              -- Pizza GD Estofada
  UNION ALL SELECT 'Pizza Estofada', 502                              -- Pizza MD Estofada Plat
  UNION ALL SELECT 'Pizza Estofada', 503                              -- Pizza GD Estofada Plat
  UNION ALL SELECT 'Mediana Max', 298
  UNION ALL SELECT 'Grande Max', 436
  UNION ALL SELECT 'Plan Portero', 301
  UNION ALL SELECT 'Promo 2 Lasagnas con bebida', 613
  UNION ALL SELECT 'Combo Familiar GD DiDi', 568
  UNION ALL SELECT 'Pizzeta Promo DiDi', 607
  UNION ALL SELECT 'Grande DiDi', 495
  UNION ALL SELECT 'Pizzeta DiDi', 497
  UNION ALL SELECT 'Promo 2 Lasagnas con bebida DiDi', 614
  UNION ALL SELECT 'Pizza con gaseosa Rappi', 567                     -- Hawaiana XG + Gas 1.5l
  UNION ALL SELECT 'Pizza con gaseosa Rappi', 569                     -- Americana GD + Gas 1.5l
  ) t
  JOIN pizzaamericana.promocion_reporte c ON c.nombre = t.nombre
 WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.promocion_reporte_item ya
                    WHERE ya.idpromo = c.idpromo AND ya.idproducto = t.idproducto);

-- Las Estofada de empleado -477 y 478- se dejan FUERA a proposito: son precio
-- de empleado, no venta de promocion. En tres meses fueron 1 unidad.

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT p.idpromo, p.nombre,
       IF(p.plataforma = 'S', 'plataforma', 'propia') AS tipo,
       p.activo, p.orden,
       (SELECT COUNT(*) FROM pizzaamericana.promocion_reporte_item i
         WHERE i.idpromo = p.idpromo) AS productos,
       (SELECT GROUP_CONCAT(i.idproducto ORDER BY i.idproducto)
          FROM pizzaamericana.promocion_reporte_item i WHERE i.idpromo = p.idpromo) AS ids
  FROM pizzaamericana.promocion_reporte p
 ORDER BY p.orden;

SELECT COUNT(*) AS promociones, SUM(activo='S') AS activas FROM pizzaamericana.promocion_reporte;
SELECT COUNT(*) AS productos_mapeados FROM pizzaamericana.promocion_reporte_item;
