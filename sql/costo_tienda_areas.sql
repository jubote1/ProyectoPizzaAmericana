-- Las cuotas de area y el gasto propio de la tienda: la fase 4 del costo por tienda.
--
-- Idempotente: se puede correr varias veces.
--
-- ---------------------------------------------------------------------------
-- 1. CUOTA PUNTO VENTA NO ES UNA CUOTA DE AREA
--
-- Los cinco conceptos que quedaron marcados AREA no son cinco areas: son
-- cuatro. El septimo, CUOTA PUNTO VENTA, es el gasto PROPIO de cada tienda y
-- no se reparte entre nadie.
--
-- Ya esta en el sistema, cargado para las once:
--   inventarioamericana.gerencia_gasto_fijo_tienda   arriendo, alarma, seguro,
--       fumigacion, contador, industria y comercio... con vigencia desde/hasta
--   inventarioamericana.gerencia_gasto_servicio      los servicios del mes
--
-- Cuadra con el Excel. Manrique, septiembre: 3.950.200 de fijos mas 2.765.900
-- de servicios da 6.716.100, que es exactamente lo que dice la hoja MANRIQUE
-- del libro. La pestana PORCENTAJE dice 6.253.000, pero esa esta digitada.
-- ---------------------------------------------------------------------------
UPDATE pizzaamericana.concepto_costo SET origen = 'TIENDA' WHERE idconcepto = 7;

-- ---------------------------------------------------------------------------
-- 2. QUE AREA ALIMENTA QUE CONCEPTO
--
-- Tabla y no codigo, igual que los tipos de egreso y los conceptos semanales.
-- El area tiene que existir en GastoAreaCtrl.AREAS y tener gastos cargados en
-- la pantalla de Gastos por Area; si no, el concepto sale vacio y la pantalla
-- dice "pendiente", que es lo correcto: nadie ha cargado esa area todavia.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pizzaamericana.concepto_costo_area (
  idconcepto INT         NOT NULL,
  area       VARCHAR(30) NOT NULL,
  PRIMARY KEY (idconcepto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pizzaamericana.concepto_costo_area (idconcepto, area) VALUES
  ( 9, 'ADMINISTRATIVA'),
  (10, 'LOGISTICA'),
  (11, 'CONTACT'),
  (12, 'PUBLICIDAD')
ON DUPLICATE KEY UPDATE area = VALUES(area);

-- ---------------------------------------------------------------------------
-- 3. COMO SE REPARTE, Y POR QUE NO PAREJO
--
-- El Excel reparte PAREJO: las once tiendas tienen exactamente 14.715.000 de
-- cuota administrativa, 4.387.000 de logistica y 4.180.000 de contact. Eso
-- hace que una tienda que vende la mitad que otra cargue el mismo peso, y en
-- porcentaje de su venta se vea el doble de cara sin haber hecho nada mal.
--
-- Aqui se reparte por PARTICIPACION EN LA VENTA de la semana: el costo del
-- area de ese mes, dividido entre las semanas del mes, y cada tienda paga la
-- fraccion que le corresponde segun lo que vendio esa semana contra lo que
-- vendieron todas.
--
-- La participacion se calcula SIEMPRE sobre las once, aunque en pantalla se
-- este mirando una sola: el denominador es la venta de toda la operacion.
--
-- Solo PUBLICIDAD tiene gastos cargados hoy. Las otras tres se cargan en la
-- pantalla de Gastos por Area y aparecen solas.
-- ---------------------------------------------------------------------------

-- Como quedo.
SELECT c.idconcepto, c.nombre, c.origen, a.area
  FROM pizzaamericana.concepto_costo c
  LEFT JOIN pizzaamericana.concepto_costo_area a ON a.idconcepto = c.idconcepto
 WHERE c.origen IN ('AREA', 'TIENDA')
 ORDER BY c.orden;
