-- Costo por tienda y por semana: los conceptos de la pestana PORCENTAJE.
--
-- La pestana de hoy tiene trece conceptos por tienda y NINGUNO tiene formula: los
-- trece valores estan digitados a mano. Por eso no se puede reproducir: al medirlos
-- contra los datos reales, cada tienda cuadra con un corte distinto. Lo que se hace
-- aqui es dejar cada concepto amarrado a su fuente, para que de aqui en adelante los
-- once salgan del mismo periodo.
--
-- La semana se identifica por su DOMINGO, igual que datamart.venta_semanal_tienda y
-- que inventarioamericana.gasto_empleado_temporal. Cambiar esa convencion aqui
-- dejaria el costo de una semana contra la venta de otra.
--
-- Idempotente: se puede correr varias veces.

-- ---------------------------------------------------------------------------
-- 1. El catalogo de conceptos de la pestana.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pizzaamericana.concepto_costo (
  idconcepto   INT          NOT NULL,
  nombre       VARCHAR(60)  NOT NULL,
  orden        INT          NOT NULL,
  -- De donde sale el valor:
  --   EGRESO  se suma de datamart.egreso_historico_tienda por tipo de egreso
  --   TEMPORAL  sale de inventarioamericana.gasto_empleado_temporal
  --   SEMANAL   sale de inventarioamericana.gasto_semanal, que ya trae por tienda y
  --             por semana lo que liquidaron las plataformas y los recaudadores
  --   AREA      se reparte desde gasto_area por participacion en la venta
  --   TIENDA    gasto propio de la tienda: gerencia_gasto_fijo_tienda mas
  --             gerencia_gasto_servicio. No se reparte entre nadie
  --   DIGITADO  no hay de donde sacarlo: se digita por tienda y mes
  --   MIXTO     parte digitada y parte de egresos. Hoy solo la nomina: la fija se
  --             digita y el incentivo del temporal sale del egreso tipo 1
  origen       VARCHAR(20)  NOT NULL,
  activo       CHAR(1)      NOT NULL DEFAULT 'S',
  PRIMARY KEY (idconcepto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pizzaamericana.concepto_costo (idconcepto, nombre, orden, origen) VALUES
  ( 1, 'COMISION VIRTUAL',               1, 'SEMANAL'),
  ( 2, 'NOMINA PUNTO VENTA Y TEMPORAL',  2, 'MIXTO'),
  ( 3, 'VARIOS PUNTO VENTA',             3, 'EGRESO'),
  ( 4, 'PREMIOS',                        4, 'EGRESO'),
  ( 5, 'DOMICILIOS',                     5, 'EGRESO'),
  ( 6, 'MISCELANEA',                     6, 'DIGITADO'),
  ( 7, 'CUOTA PUNTO VENTA',              7, 'TIENDA'),
  ( 8, 'MENSAJERIA EXTERNA',             8, 'TEMPORAL'),
  ( 9, 'CUOTA ADMINISTRATIVA',           9, 'AREA'),
  (10, 'CUOTA LOGISTICA Y PRODUCCION',  10, 'AREA'),
  (11, 'NOMINA CONTACT',                11, 'AREA'),
  (12, 'PUBLICIDAD Y MERCADEO',         12, 'AREA'),
  (13, 'COMISION DATAFONOS',            13, 'SEMANAL')
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre), orden = VALUES(orden), origen = VALUES(origen);

-- ---------------------------------------------------------------------------
-- 2. Que tipo de egreso entra en que concepto.
--
-- Esto es tabla y no codigo a proposito: la discusion de si "Pago Apoyos" es un
-- premio o no se resuelve cambiando una fila, no recompilando. Un tipo de egreso
-- que no este aqui simplemente no entra al costo, y eso tambien se puede ver.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pizzaamericana.concepto_costo_tipo_egreso (
  idconcepto     INT NOT NULL,
  idtipo_egreso  INT NOT NULL,
  PRIMARY KEY (idconcepto, idtipo_egreso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Medido contra septiembre de 2026, las once tiendas: $35,2 millones en egresos.
-- Lo que queda por fuera del costo queda por fuera A PROPOSITO y se explica abajo.

-- DOMICILIOS: el rodamiento del domiciliario de la tienda y los datos del celular.
INSERT INTO pizzaamericana.concepto_costo_tipo_egreso (idconcepto, idtipo_egreso)
     VALUES (5, 2), (5, 8)
ON DUPLICATE KEY UPDATE idconcepto = idconcepto;

-- PREMIOS: los premios y la comision de deditos.
-- "Pago Apoyos" (19), $1.748.000 en septiembre, NO esta incluido: falta confirmar
-- si es premio. Para meterlo basta agregar la fila (4, 19).
INSERT INTO pizzaamericana.concepto_costo_tipo_egreso (idconcepto, idtipo_egreso)
     VALUES (4, 17), (4, 10)
ON DUPLICATE KEY UPDATE idconcepto = idconcepto;

-- VARIOS PUNTO VENTA: lo menudo de la tienda, mas la vigilancia.
INSERT INTO pizzaamericana.concepto_costo_tipo_egreso (idconcepto, idtipo_egreso)
     VALUES (3, 4), (3, 6), (3, 9), (3, 11), (3, 13), (3, 14)
ON DUPLICATE KEY UPDATE idconcepto = idconcepto;

-- NOMINA PUNTO VENTA Y TEMPORAL: el incentivo que se le liquida al domiciliario
-- temporal y las horas extraordinarias. La nomina fija sigue digitada (ver abajo).
INSERT INTO pizzaamericana.concepto_costo_tipo_egreso (idconcepto, idtipo_egreso)
     VALUES (2, 1), (2, 18)
ON DUPLICATE KEY UPDATE idconcepto = idconcepto;

-- QUE QUEDA POR FUERA Y POR QUE (cifras de septiembre, las once tiendas):
--   5  Vale nomina            $4.134.250  es un adelanto que se descuenta despues;
--                                         sumarlo seria cobrar la nomina dos veces.
--   19 Pago Apoyos            $1.748.000  falta decidir si es premio.
--   16 Adelanto Pago Producto   $887.800  adelanto, no gasto.
--   12 Descuadre Dom Externo    $635.300  es una perdida, no un costo de operar;
--                                         si se quiere medir, merece concepto propio.
--   7  Producto en otra tienda  $266.300  traslado entre tiendas, se anula en el total.
--   3  Propina datafono          $70.350  es del empleado, pasa derecho.
-- Entre todos son $7,7 de los $35,2 millones. Agregar cualquiera es una fila aqui.
-- ---------------------------------------------------------------------------
-- 3. Lo que toca digitar, por tienda y por mes.
--
-- Hoy son tres: la nomina fija del punto de venta, la miscelanea y la comision
-- virtual. No es que falte voluntad de calcularlos, es que el dato no existe:
--   - la nomina no se puede repartir por tienda porque general.empleado no dice
--     en que tienda esta cada quien, y las horas marcadas cubren como la tercera
--     parte de lo que se paga;
--   - la miscelanea todavia no tiene repositorio;
--   - la comision virtual no cuadra con ninguna tarifa de pasarela.
-- Cada uno que se logre calcular despues sale de aqui y pasa a su fuente.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pizzaamericana.costo_tienda_digitado (
  idtienda    INT          NOT NULL,
  idconcepto  INT          NOT NULL,
  anio        INT          NOT NULL,
  mes         INT          NOT NULL,
  valor       DOUBLE       NOT NULL DEFAULT 0,
  usuario     VARCHAR(50)  NULL,
  fecha_cambio TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (idtienda, idconcepto, anio, mes)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- La comision de datafonos y la comision virtual NO se digitan: salen de
-- gasto_semanal. Ver sql/costo_tienda_comisiones.sql.
