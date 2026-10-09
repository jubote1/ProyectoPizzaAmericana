-- ---------------------------------------------------------------------------
-- SACAR LAS BEBIDAS DESCONTINUADAS DE LA HOMOLOGACION DEL BOT
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- NO CORRER SIN LEER ESTO
--
-- El tipo de liquido 8 se llama literalmente NO EXISTEN y tiene 32 sabores:
-- Manzana, Colombiana, 7up, Pepsi, Uva, Naranja y companhia. Alguien retiro ahi
-- las bebidas descontinuadas, y para el POS funciono -no las ofrece- pero la
-- homologacion del BOT busca por SKU y nunca mira la familia, asi que 93 de los
-- 279 SKU de bebida siguen apuntando a una bebida muerta.
--
-- Medido entre el 24 de septiembre y el 8 de octubre en Manrique, America y
-- Niquia: la Manzana 1.5 Lts aparecio 13 veces, TODAS por el CRM y NINGUNA por
-- el POS, Rappi, DiDi o la tienda virtual. Ninguno de esos canales usa esta
-- tabla; el CRM si.
--
-- ESTO ES LA RED DE SEGURIDAD, NO EL ARREGLO
--
-- El arreglo de verdad es que el BOT deje de OFRECERLAS en su menu, que vive en
-- el flujo del CRM y no aca. Mientras eso pasa, el cliente que pida Manzana va
-- a recibir su pedido SIN bebida, que es mejor que recibir una que la tienda no
-- tiene; y el asesor lo va a ver al confirmar.
--
-- SE DESACTIVAN, NO SE BORRAN
--
-- Si manana vuelve la Colombiana, se devuelve una fila a 'S' y listo. Borrarlas
-- obligaria a volver a escribir los 16 SKU de cada una.
-- ---------------------------------------------------------------------------

-- La tabla no tiene columna para desactivar, asi que se agrega.
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='pizzaamericana'
                   AND TABLE_NAME='homologacion_producto_pedidovirtual'
                   AND COLUMN_NAME='activo');
SET @sql := IF(@existe = 0,
  'ALTER TABLE pizzaamericana.homologacion_producto_pedidovirtual
     ADD COLUMN activo CHAR(1) NOT NULL DEFAULT ''S''
     COMMENT ''N saca el SKU de la homologacion sin perder el mapeo''',
  'SELECT ''la columna activo ya existe'' AS paso');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Fuera las que apuntan a la familia de las descontinuadas.
UPDATE pizzaamericana.homologacion_producto_pedidovirtual h
  JOIN pizzaamericana.sabor_x_tipo_liquido s
    ON s.idsabor_x_tipo_liquido = h.idinterno
   SET h.activo = 'N'
 WHERE h.tipo = 'T' AND s.idtipo_liquido = 8 AND h.activo = 'S';

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT CASE WHEN s.idtipo_liquido = 8 THEN 'descontinuada' ELSE 'se vende' END AS familia,
       h.activo, COUNT(*) AS skus, COUNT(DISTINCT h.idinterno) AS bebidas
  FROM pizzaamericana.homologacion_producto_pedidovirtual h
  JOIN pizzaamericana.sabor_x_tipo_liquido s ON s.idsabor_x_tipo_liquido = h.idinterno
 WHERE h.tipo = 'T' GROUP BY familia, h.activo;
