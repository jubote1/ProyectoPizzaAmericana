-- Las comisiones y la comision de datafonos del costo por tienda.
--
-- Resuelve las tres cosas que faltaban de la pestana PORCENTAJE. Las tres salen de
-- inventarioamericana.gasto_semanal, que ya trae por tienda y por semana lo que
-- liquidaron las plataformas y los recaudadores, desde 2021.
--
-- Idempotente: se puede correr varias veces.
--
-- ---------------------------------------------------------------------------
-- 1. PAGO APOYOS ES PREMIO
--
-- El tipo 19 es el incentivo que se le da a quien apoya el punto de venta. Entra
-- en PREMIOS. En septiembre de 2026 eran $1.748.000 en las once tiendas.
-- ---------------------------------------------------------------------------
INSERT INTO pizzaamericana.concepto_costo_tipo_egreso (idconcepto, idtipo_egreso)
     VALUES (4, 19)
ON DUPLICATE KEY UPDATE idconcepto = idconcepto;

-- ---------------------------------------------------------------------------
-- 2. QUE CONCEPTO DE gasto_semanal ALIMENTA QUE CONCEPTO DEL COSTO
--
-- Igual que con los tipos de egreso: esto es tabla y no codigo, para que agregar
-- Bold o quitar una pasarela sea una fila.
--
-- aplica_porcentaje distingue dos casos que se ven iguales en la tabla:
--   'N'  valor_gasto YA es la comision. Es lo normal: los procesos de
--        conciliacion escriben la comision calculada.
--   'S'  valor_gasto es la BASE y hay que aplicarle el porcentaje que el propio
--        gasto_configuracion.porcentaje_gasto tiene guardado.
--
-- LA SEMANA SE CRUZA POR SU DOMINGO, Y ESO NO ES COSMETICO
--
-- El concepto 18 (comision de Rappi) lo escriben DOS procesos: ReporteSemanalRappi
-- corta domingo con la semana de lunes a domingo, y ReporteSemanalPagoRappi corta
-- miercoles con la semana de sabado a viernes. Son la misma plata medida en dos
-- ventanas, y las dos quedan en el mismo concepto. En 2026, hasta septiembre, el
-- domingo sumaba $250 millones y el viernes $247: sumar los dos duplica Rappi.
-- Cruzar por el domingo de la semana deja por fuera las filas del viernes, que es
-- lo correcto. El Menu Gerencia se salva por lo mismo, porque une contra
-- gerencia_calendario_semana.fecha_fin, que siempre es domingo.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pizzaamericana.concepto_costo_gasto_semanal (
  idconcepto        INT     NOT NULL,
  idgasto_conf      INT     NOT NULL,
  aplica_porcentaje CHAR(1) NOT NULL DEFAULT 'N',
  PRIMARY KEY (idconcepto, idgasto_conf)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- COMISION VIRTUAL: las cinco comisiones que ya se calculan.
--   16 Comision Pago Virtual   (Wompi, el link de pago)
--   17 Comision Pagos PAYU     (hoy Rapyd)
--   18 Comision Pedidos Rappi
--   20 Comision Pedidos DiDi
--   25 QR Bancolombia          (ojo: el catalogo todavia lo llama "NO USAR - XL
--                               Promo Deditos Santa". El nombre esta muerto, el
--                               concepto no: ReporteConciliacionWompi le escribe
--                               la comision del QR con el parametro QRBANCOLOMBIA)
-- Falta Bold, que no pasa por gasto_semanal: vive en log_evento_bold.
INSERT INTO pizzaamericana.concepto_costo_gasto_semanal (idconcepto, idgasto_conf, aplica_porcentaje)
     VALUES (1, 16, 'N'), (1, 17, 'N'), (1, 18, 'N'), (1, 20, 'N'), (1, 25, 'N')
ON DUPLICATE KEY UPDATE aplica_porcentaje = VALUES(aplica_porcentaje);

-- COMISION DATAFONOS: el concepto 24 trae la venta con tarjeta conciliada, y
-- gasto_configuracion.porcentaje_gasto ya dice 5. Ver la nota de abajo.
INSERT INTO pizzaamericana.concepto_costo_gasto_semanal (idconcepto, idgasto_conf, aplica_porcentaje)
     VALUES (13, 24, 'S')
ON DUPLICATE KEY UPDATE aplica_porcentaje = VALUES(aplica_porcentaje);

-- Los dos conceptos dejan de ser digitados.
UPDATE pizzaamericana.concepto_costo SET origen = 'SEMANAL' WHERE idconcepto IN (1, 13);

-- ---------------------------------------------------------------------------
-- 3. EL 5% DEL DATAFONO YA EXISTIA, Y NADIE LO APLICABA
--
-- gasto_configuracion id 24 tiene porcentaje_gasto = 5 desde hace rato. No servia
-- de nada: ese porcentaje solo lo aplica ReporteConsolidacionRentabilidad, y ese
-- proceso se salta los conceptos con consulta_sql = 'NA', que es el caso del 24.
-- Quien escribe el 24 es ReporteConciliacionWompi, y mete la venta con tarjeta en
-- valor_calculo Y en valor_gasto, sin tocar el porcentaje. Por eso el 5 estaba ahi
-- sin efecto.
--
-- Aqui si se aplica. El parametro COMISIONDATAFONO que se habia creado sobra y se
-- borra: tener la tarifa en dos sitios es garantia de que algun dia digan distinto.
DELETE FROM general.parametros WHERE valorparametro = 'COMISIONDATAFONO';

-- OJO CON EL 5%, QUE NO CUADRA CON EL RESTO DEL SISTEMA
--
-- Los procesos de consignacion -ReporteConsignacionDavivienda y la parte de
-- datafonos de ReporteConsignacionWompi- descuentan 1,9% mas IVA, o sea 2,261%,
-- quemado en el codigo, para las dos adquirencias (6 tiendas Bancolombia y 5
-- Davivienda). Eso es menos de la mitad del 5%.
--
-- El 5% es el que reproduce el Excel: Manrique, septiembre, $16.037.590 de venta
-- con tarjeta contra $753.943 de la hoja, da 4,70%. Asi que la hoja viene usando
-- 5 y las consignaciones 2,261, y las dos no pueden estar bien. Queda en 5 porque
-- es lo que se pidio y lo que reproduce la hoja, pero la diferencia hay que
-- resolverla con el contrato en la mano.
