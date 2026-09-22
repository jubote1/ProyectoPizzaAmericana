-- ---------------------------------------------------------------------------
-- Las etiquetas de los campos de segmentacion, con tilde
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- POR QUE
--
-- crm.segmento_campo nacio sin tildes, siguiendo la costumbre de la casa de no
-- meter acentos en la base. Esa costumbre es buena para los comentarios y para
-- los nombres tecnicos, pero estas no son ninguna de las dos cosas: son el
-- texto que sale IMPRESO en el tablero de segmentos y en la lista desplegable
-- de la pantalla de definiciones, delante de mercadeo.
--
-- "Dias sin comprar" y "Autorizo datos" se ven descuidados. La columna es
-- utf8mb3, que aguanta tildes y enes sin problema.
--
-- El nombre TECNICO del campo -la columna de persona_resumen- no se toca: ese
-- es el que se concatena en la condicion del recalculo y el que esta en las
-- reglas ya guardadas. Aqui solo cambia lo que se lee.
-- ---------------------------------------------------------------------------

UPDATE crm.segmento_campo SET etiqueta = 'Pedidos en total',
       ayuda = 'Domicilio más mostrador'          WHERE campo = 'pedidos';
UPDATE crm.segmento_campo SET etiqueta = 'Pedidos a domicilio',
       ayuda = 'Incluye virtual recoger'          WHERE campo = 'pedidos_central';
UPDATE crm.segmento_campo SET etiqueta = 'Pedidos en mostrador',
       ayuda = 'Punto de venta y para llevar'     WHERE campo = 'pedidos_tienda';
UPDATE crm.segmento_campo SET etiqueta = 'Total comprado',
       ayuda = 'En pesos'                         WHERE campo = 'valor';
UPDATE crm.segmento_campo SET etiqueta = 'Ticket promedio',
       ayuda = 'En pesos'                         WHERE campo = 'ticket_promedio';
UPDATE crm.segmento_campo SET etiqueta = 'Días sin comprar',
       ayuda = 'Desde el último pedido'           WHERE campo = 'dias_sin_comprar';
UPDATE crm.segmento_campo SET etiqueta = 'Días entre pedidos',
       ayuda = 'Cada cuánto vuelve'               WHERE campo = 'dias_entre_pedidos';
UPDATE crm.segmento_campo SET etiqueta = 'Tiendas donde compra',
       ayuda = NULL                               WHERE campo = 'tiendas_distintas';
UPDATE crm.segmento_campo SET etiqueta = 'Tienda habitual',
       ayuda = 'El id de la tienda'               WHERE campo = 'tienda_habitual';
UPDATE crm.segmento_campo SET etiqueta = 'Autorizó datos',
       ayuda = 'S o N'                            WHERE campo = 'politica_datos';

-- Como quedo
SELECT campo, etiqueta, tipo, ayuda FROM crm.segmento_campo ORDER BY orden, campo;
