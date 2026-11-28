-- CUPON 50% 25 ANOS - LUNES 23 A JUEVES 26 DE NOVIEMBRE, TODO EL DIA Y TODA LA NOCHE
-- Destino: segmentos POR RECUPERAR y EN RIESGO. Una tanda cada dia, los cuatro dias.
--
-- Sigue siendo la oferta 61, la unica del cupon del 50% (decision del 14-nov: no
-- llenarse de ofertas y medir junto; cada tanda se separa por oferta_cliente.idenvio).
--
-- COMPROBADO ANTES DE REUSARLA: la 61 tiene 2.520 codigos historicos y CERO vivos
-- (el ultimo vencio el 21-nov), asi que reusarla no revive ninguno.
--
-- Cambios frente a la corrida del 19-nov:
--   fechas 19 y 20  ->  23 y 26
--   controla_hora   ->  se queda en 'N' (todo el dia; "toda la noche" = sin franja)
--
-- POR QUE dias_caducidad SE QUEDA EN 1 Y NO EN 3
--
-- La caducidad se calcula en la emision como hoy + dias_caducidad y es INCLUSIVA,
-- o sea que un codigo emitido el lunes 23 con 1 sirve el 23 y el 24. Con 3 el del
-- lunes llegaria hasta el jueves, pero el del jueves llegaria hasta el domingo, y
-- la promesa es "hasta el jueves". Como se manda una tanda cada dia, 1 le da a
-- cada persona dos dias para usarlo y nadie se pasa de la semana... salvo la del
-- jueves. Y NO puede ser 0: ofertasEnviables() filtra dias_caducidad > 0 y la
-- oferta desapareceria de la pantalla de Envio de Publicidad.
--
-- OJO CON LA TANDA DEL JUEVES: sus codigos venceran el viernes 27. Despues de ESE
-- envio hay que cerrarlos a mano:
--   UPDATE pizzaamericana.oferta_cliente
--      SET fecha_caducidad = '2026-11-26' WHERE idenvio = <envio del jueves>;
--
-- LA PLANTILLA: se reusa la 84 de Brevo (la misma de jueves 19 y viernes 20, de
-- todo el dia), reescrita hoy con las fechas de esta semana y con el bloque
-- "Nuestras dos de siempre" (Hawaiana Artesanal y Americana Original) tomado de la
-- plantilla 87 conmemorativa de los 25 anos. Se hace UNA vez y sirve los 4 dias,
-- por eso el cuerpo no dice "hoy y manana" sino "esta semana, hasta el jueves", y
-- la fecha concreta de cada persona sale de {{ params.vence }}.
--
-- Idempotente.

UPDATE pizzaamericana.oferta
   SET habilitado     = 'S',
       fecha_desde    = '2026-11-23',
       fecha_hasta    = '2026-11-26',
       dias_caducidad = 1,
       tipo_caducidad = 'P',
       controla_hora  = 'N',
       hora_inicio    = 0,
       hora_fin       = 0,
       mensaje1       = 'Esta semana, hasta el jueves, a cualquier hora del dia o de la noche, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.'
 WHERE idoferta = 61;

-- El nombre con que la pantalla de Envio de Publicidad lista la plantilla de Brevo.
UPDATE pizzaamericana.plantilla_brevo
   SET nombre = 'Cupon 50% 25 anos (23 al 26 nov, todo el dia)'
 WHERE idplantilla = 84;

-- A peticion de Juan David (23-nov): ponerle la fecha al nombre para no confundirla
-- en la pantalla de Envio de Publicidad con la 67 (CUPON 50% TARDE - 21 Y 22 NOV),
-- que sigue habilitada porque todavia tiene codigos vivos.
--
-- EL JUEVES 26, AL CERRAR LA CAMPANA, DEVOLVERLE EL NOMBRE GENERICO:
--   UPDATE pizzaamericana.oferta
--      SET nombre_oferta = 'CUPON 50% RECUPERACION - 25 ANOS' WHERE idoferta = 61;
-- Es la misma oferta de siempre (decision del 14-nov: una sola, para medir junto);
-- la fecha en el nombre es solo una ayuda visual de esta semana.

UPDATE pizzaamericana.oferta
   SET nombre_oferta = 'CUPON 50% 25 ANOS - 23 AL 26 NOV (TODO EL DIA)'
 WHERE idoferta = 61;
