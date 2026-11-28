-- ---------------------------------------------------------------------------
-- Se va VIGILANTEENCUESTA_RETIENE: ya no hay camino de retencion.
--
-- El interruptor existia para que el vigilante, ademas de marcar, pudiera
-- bloquear el premio. Con el modelo que quedo -se marca en el momento del giro
-- y quien dispersa decide- bloquear no tiene lugar: le quitaria la decision a
-- la persona que oprime el boton, que es justamente quien debe tenerla.
--
-- Dejar el parametro sin codigo que lo lea es peor que quitarlo: alguien lo
-- pone en 'S' algun dia, no pasa nada, y se pierde media manana averiguando
-- por que.
--
-- Bloquear a mano sigue siendo posible cuando haga falta: es lo que hizo
-- sql/2026_11_27_01_bloquear_premios_ruleta_encuesta_masiva.sql con los 5
-- premios del 22 de noviembre, y esos siguen bloqueados.
--
-- Idempotente.
-- ---------------------------------------------------------------------------

delete from general.parametros where valorparametro = 'VIGILANTEENCUESTA_RETIENE';

select count(*) as debe_dar_cero
  from general.parametros where valorparametro = 'VIGILANTEENCUESTA_RETIENE';

select 'premios bloqueados a mano el 27-nov (siguen bloqueados)' as que, count(*) as cuantos
  from pizzaamericana.resultado_ruleta where idofertacliente = -2;
