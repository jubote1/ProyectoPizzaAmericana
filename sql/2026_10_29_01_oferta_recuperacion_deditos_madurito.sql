-- Oferta "RECUPERACION DEDITOS O MADURITO": regalo de $14.000 pensado para clientes en riesgo o perdidos.
--
-- Base: pizzaamericana (central).
--
-- Es copia de la oferta 45 ("Encueta Deditos Masa Queso - Madurito", la de la encuesta): mismo valor
-- ($14.000), mismos 15 dias de caducidad, codigo personal de un solo uso, todas las tiendas, sin monto
-- minimo, contact 'S' (para que salga en el selector "Oferta con codigo" de EnvioPublicidad.html).
-- Cambia solo lo que cambia: el nombre y el mensaje, que en vez de "te premiamos por la encuesta"
-- invita a volver a pedir y dice que el valor tambien sirve para completar una pizza extra grande.
--
-- Las fechas desde/hasta quedan en NULL: en un codigo personal manda la caducidad del propio codigo
-- (CodigoPromoDAO), y la 45 trae 2021 de cuando se creo.
--
-- Nombre en mayusculas, como las otras de recuperacion (49 "RECUPERACION PIZZETA", 50 "RECUPERACION  MEDIANA").
--
-- Idempotente: si ya existe una oferta con ese nombre, no inserta otra.

INSERT INTO oferta
    (nombre_oferta, idexcepcion, codigo_promocional, descuento_fijo_porcentaje,
     descuento_porcentaje_futuro, descuento_fijo_valor, mensaje1, mensaje2, dias_caducidad,
     tipo_caducidad, controla_hora, hora_inicio, hora_fin, tipo_oferta, fecha_desde, fecha_hasta,
     codigo_general, contact, red_parcial, reintegro, habilitado, monto_minimo, tope_descuento,
     tiendas, aplica_a, max_usos_cliente, max_emision)
SELECT 'RECUPERACION DEDITOS O MADURITO', o.idexcepcion, o.codigo_promocional, o.descuento_fijo_porcentaje,
       o.descuento_porcentaje_futuro, o.descuento_fijo_valor,
       'Pizza Americana te extraña. Vuelve a pedir y te regalamos $ 14.000 pesos, el equivalente a unos deditos o un madurito. Si prefieres, úsalos para completar una pizza extra grande. Llama al 604 4444553 o acércate a un punto de venta, tienes 15 días para redimirlo en Pizza Americana.',
       o.mensaje2, o.dias_caducidad,
       o.tipo_caducidad, o.controla_hora, o.hora_inicio, o.hora_fin, o.tipo_oferta, NULL,
       NULL, o.codigo_general, o.contact, o.red_parcial, o.reintegro, o.habilitado,
       o.monto_minimo, o.tope_descuento, o.tiendas, o.aplica_a, o.max_usos_cliente, o.max_emision
  FROM oferta o
 WHERE o.idoferta = 45
   AND NOT EXISTS (SELECT 1 FROM oferta x WHERE x.nombre_oferta = 'RECUPERACION DEDITOS O MADURITO');

-- Comprobacion: una fila con 14000, 15 dias, codigo_promocional S, contact S, habilitado S, y el acento bien guardado (acento_ok = 1).
SELECT idoferta, nombre_oferta, codigo_promocional, descuento_fijo_valor, dias_caducidad, tipo_caducidad,
       contact, habilitado, red_parcial, aplica_a, max_usos_cliente, fecha_desde, fecha_hasta,
       LOCATE('extraña', mensaje1) > 0 AS acento_ok
  FROM oferta
 WHERE nombre_oferta = 'RECUPERACION DEDITOS O MADURITO';
