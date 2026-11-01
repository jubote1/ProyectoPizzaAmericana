-- ---------------------------------------------------------------------------
-- MARCAR A LOS CLIENTES COMO HINCHAS, CON EL CALENDARIO DE MAYO A OCTUBRE 2026
--
-- Idempotente: se puede correr dos veces, no duplica ni cambia a quien ya este marcado.
-- Necesita 2026_11_01_01 y 2026_11_01_03 corridos antes.
--
-- LA REGLA
--
-- Compro el Combo Futbolero (datamart.producto 444) un dia en que jugaba UNICAMENTE un
-- equipo -> es hincha de ese equipo. Las fechas salen de partido_futbol y SOLO las
-- confirmadas. Una fecha con los dos equipos no dice nada y se descarta sola.
--
-- QUIEN NO SE MARCA
--
-- - El que compro en dias de los dos equipos (24 personas): o va con quien lo invite, o
--   en esa casa hay hinchas de ambos. Quedarse con el que mas veces aparezca seria
--   inventar.
-- - El que ya tenga una marca de hinchada: si alguien la corrigio a mano, esto no se la
--   cambia.
-- - El pedido sin cliente identificado o sin telefono de al menos 7 digitos.
--
-- POR QUE EL PUENTE AL CENTRAL
--
-- El detalle del pedido vive en el datamart, pero el TELEFONO solo esta en el central,
-- y las marcas van por telefono. El puente es (idtienda, numposheader, fechapedido). Se
-- pierden los pedidos que no alcanzaron a subir -16% - y es preferible a adivinar.
--
-- LO QUE DEJA (previsualizado el 2026-11-01): 367 hinchas de Nacional, 212 del
-- Medellin, 24 sin marcar por ambiguos.
--
-- PARA DEVOLVERLO
--
--   DELETE FROM pizzaamericana.cliente_marca
--    WHERE idtipo IN ('HINCHA_NACIONAL','HINCHA_MEDELLIN') AND usuario = 'marcado_masivo';
-- ---------------------------------------------------------------------------

USE pizzaamericana;

INSERT INTO cliente_marca (telefono, idtipo, fecha_marca, usuario)
SELECT z.telefono,
       CASE WHEN z.n > 0 THEN 'HINCHA_NACIONAL' ELSE 'HINCHA_MEDELLIN' END,
       NOW(), 'marcado_masivo'
FROM (
  SELECT tel.telefono,
         SUM(dia.equipo = 'NACIONAL') AS n,
         SUM(dia.equipo = 'MEDELLIN') AS m
    FROM (SELECT fecha, MIN(equipo) AS equipo FROM partido_futbol
           WHERE confirmado = 'S' GROUP BY fecha HAVING COUNT(DISTINCT equipo) = 1) dia
    JOIN datamart.pedido p
      ON p.fechapedido = dia.fecha AND p.idmotivoanulacion IS NULL
    JOIN datamart.detalle_pedido dp
      ON dp.idpedidotienda = p.idpedidotienda AND dp.idtienda = p.idtienda AND dp.idproducto = 444
    JOIN pedido pc
      ON pc.numposheader = p.idpedidotienda AND pc.idtienda = p.idtienda
     AND pc.fechapedido = p.fechapedido AND pc.idcliente > 1
    JOIN (SELECT idcliente,
                 REGEXP_REPLACE(IFNULL(NULLIF(TRIM(telefono_celular),''), IFNULL(telefono,'')),
                                '[^0-9]', '') AS telefono
            FROM cliente) tel ON tel.idcliente = pc.idcliente
   WHERE CHAR_LENGTH(tel.telefono) >= 7
   GROUP BY tel.telefono
) z
WHERE NOT (z.n > 0 AND z.m > 0)
  AND NOT EXISTS (SELECT 1 FROM cliente_marca cm
                   WHERE cm.telefono = z.telefono
                     AND cm.idtipo IN ('HINCHA_NACIONAL','HINCHA_MEDELLIN'));

-- Comprobacion.
SELECT t.nombre, COUNT(*) AS clientes
  FROM cliente_marca m JOIN cliente_marca_tipo t ON t.idtipo = m.idtipo
 WHERE m.idtipo IN ('HINCHA_NACIONAL','HINCHA_MEDELLIN')
 GROUP BY t.nombre ORDER BY clientes DESC;
