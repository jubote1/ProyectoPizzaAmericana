-- ---------------------------------------------------------------------------
-- Bloquea los premios de ruleta que salieron de encuestas llenadas en masa.
--
-- QUE PASO. El QR del tiquete lleva el numero de pedido a la vista en la
-- direccion (?idpedido=NNN&idtienda=N), asi que cambiando ese numero en el
-- navegador se puede contestar la encuesta de un pedido ajeno. Cada encuesta
-- da un giro de ruleta. Medido el 2026-11-27 sobre encuesta_servicio: hay 16
-- telefonos con 5 o mas encuestas en un mismo dia y una misma tienda, 315
-- pedidos en total, entre el 2 de mayo y el 22 de noviembre de 2026. El caso
-- mas grande son 75 encuestas de un solo telefono en una noche, y el mas
-- rapido 27 encuestas en 23 minutos: una cada 51 segundos. Entre los pedidos
-- "encuestados" hay domicilios de Rappi, DiDi y BOT, que nunca pasaron por el
-- punto de venta, asi que no son compras de quien responde.
--
-- QUE HACE ESTE SCRIPT. De esos 315 pedidos salieron 20 premios: 13 ya
-- entregados, 2 marcados a mano desde antes, y 5 PENDIENTES. Este script marca
-- esos 5 para que la pantalla de dispersion no los pueda entregar. No toca los
-- que ya se entregaron -eso es decision aparte- ni los 294 giros que no
-- ganaron nada, que no hay para que bloquear.
--
-- COMO BLOQUEA. resultado_ruleta.idofertacliente es enlace y marca a la vez, y
-- RuletaDAO.marcarDispersion solo actualiza "WHERE idofertacliente = 0". Con
-- cualquier otro valor el boton de dispersar no puede tocar la fila. Se usa -2
-- y no -1 porque -1 ya significa "entregado a mano" (441 filas de antes de que
-- existiera la pantalla) y confundir las dos cosas seria peor que no marcar.
--
-- OJO: hasta que se despliegue el .war con el cambio de RuletaDAO.calcularEstado,
-- la pantalla va a mostrar estos 5 como "ENTREGADO A MANO", que es la etiqueta
-- vieja de todos los negativos. El bloqueo si opera desde ya; lo que falta es
-- el rotulo.
--
-- REVERSIBLE: para devolver uno a la cola, ponerle idofertacliente = 0 de nuevo
-- (el respaldo de abajo guarda el valor anterior de cada fila).
--
-- Idempotente: correrlo dos veces no cambia nada la segunda vez.
-- ---------------------------------------------------------------------------

-- Respaldo del valor anterior, antes de tocar nada.
create table if not exists pizzaamericana.respaldo_ruleta_bloqueo_20261127 (
  idresultado      int primary key,
  idofertacliente  int,
  fecha_respaldo   timestamp default current_timestamp
);

insert ignore into pizzaamericana.respaldo_ruleta_bloqueo_20261127 (idresultado, idofertacliente)
select idresultado, idofertacliente
from pizzaamericana.resultado_ruleta
where idresultado in (15922, 15923, 15930, 16000, 16032);

-- Los cinco premios pendientes. Todos del mismo telefono, la misma noche, en
-- Itagui: la jornada de las 75 encuestas del 22 de noviembre. Cuatro son el 20%
-- de descuento (oferta 52) y uno la pizza personal gratis (oferta 49).
update pizzaamericana.resultado_ruleta
   set idofertacliente = -2
 where idresultado in (15922, 15923, 15930, 16000, 16032)
   and idofertacliente = 0;

-- Verificacion.
select r.idresultado,
       r.idpedido,
       date(r.fecha)        as fecha_giro,
       r.idofertacliente,
       case when r.idofertacliente = -2 then 'BLOQUEADO'
            when r.idofertacliente =  0 then 'sigue pendiente (revisar)'
            else 'otro estado' end as resultado
  from pizzaamericana.resultado_ruleta r
 where r.idresultado in (15922, 15923, 15930, 16000, 16032)
 order by r.idresultado;
