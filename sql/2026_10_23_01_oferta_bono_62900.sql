-- Oferta "Bono $62.900": bono por valor fijo, redencion parcial, 60 dias.
--
-- Base: pizzaamericana (central).
--
-- Se crea copiando la oferta 58 ("Bono Influencer - Pago en producto"), que ya es un bono de
-- redencion parcial, y cambiando solo lo que cambia: el nombre, el valor (62900), los dias de
-- caducidad (60 = "2 meses") y el valor que dice el mensaje. Copiarla asegura que el texto
-- conserve el encoding original y que queden igual los demas campos (todas las tiendas, sin
-- monto minimo, max_usos_cliente 0 = sin limite, codigo_promocional 'S').
--
-- El saldo NO se escribe aqui: al asignarsela a una persona desde CRMClientes.html el sistema
-- genera el codigo, siembra oferta_cliente.saldo = 62900 y fija la fecha de caducidad a 60 dias
-- desde ese dia.
--
-- Idempotente: si ya existe una oferta con ese nombre, no inserta otra.

INSERT INTO oferta
    (nombre_oferta, idexcepcion, codigo_promocional, descuento_fijo_porcentaje,
     descuento_porcentaje_futuro, descuento_fijo_valor, mensaje1, mensaje2, dias_caducidad,
     tipo_caducidad, controla_hora, hora_inicio, hora_fin, tipo_oferta, fecha_desde, fecha_hasta,
     codigo_general, contact, red_parcial, reintegro, habilitado, monto_minimo, tope_descuento,
     tiendas, aplica_a, max_usos_cliente, max_emision)
SELECT 'Bono $62.900', o.idexcepcion, o.codigo_promocional, o.descuento_fijo_porcentaje,
       o.descuento_porcentaje_futuro, 62900, REPLACE(o.mensaje1, '132.000', '62.900'), o.mensaje2, 60,
       o.tipo_caducidad, o.controla_hora, o.hora_inicio, o.hora_fin, o.tipo_oferta, o.fecha_desde,
       o.fecha_hasta, o.codigo_general, o.contact, o.red_parcial, o.reintegro, o.habilitado,
       o.monto_minimo, o.tope_descuento, o.tiendas, o.aplica_a, o.max_usos_cliente, o.max_emision
  FROM oferta o
 WHERE o.idoferta = 58
   AND NOT EXISTS (SELECT 1 FROM oferta x WHERE x.nombre_oferta = 'Bono $62.900');

-- Comprobacion: debe salir una fila con 62900, 60, red_parcial S, habilitado S, y trae_valor_nuevo = 1.
SELECT idoferta, nombre_oferta, descuento_fijo_valor, dias_caducidad, tipo_caducidad, red_parcial,
       habilitado, monto_minimo, max_usos_cliente, LOCATE('62.900', mensaje1) > 0 AS trae_valor_nuevo
  FROM oferta
 WHERE nombre_oferta = 'Bono $62.900';
