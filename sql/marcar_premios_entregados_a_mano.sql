-- ---------------------------------------------------------------------------
-- Marca como ya entregados los premios de ruleta que se atendieron a mano,
-- antes de que existiera la pantalla de dispersion.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Corte: hasta el 2026-08-20 inclusive. Del 21 en adelante se usa la pantalla.
--
-- Por que se marca con -1 y no con el idofertacliente real:
--
-- Se intento enlazar cada premio con la oferta que se le creo a mano, pero solo
-- 162 de 441 se pueden emparejar, y el criterio disponible es difuso (misma
-- oferta, mismo cliente, dentro de los 10 dias). Para la oferta 49, que ademas
-- se usa en recuperacion de clientes, ese criterio puede enlazar la oferta
-- EQUIVOCADA y dejar en la pantalla un codigo que no corresponde a ese premio.
--
-- Un dato ausente y reconocible es mejor que un dato presente y falso. Con -1 la
-- pantalla los muestra como ENTREGADO A MANO, en gris, sin codigo ni fecha de
-- aviso, que es exactamente lo que sabemos de ellos.
--
-- Efecto secundario util: el UPDATE de la dispersion exige idofertacliente = 0,
-- asi que estas filas quedan protegidas y no se pueden dispersar por accidente.
-- ---------------------------------------------------------------------------

-- PASO 1. Ver que se va a marcar, antes de tocar nada.
select date_format(r.fecha,'%Y-%m') mes, count(*) premios,
       min(date(r.fecha)) desde, max(date(r.fecha)) hasta
from resultado_ruleta r
join opciones_ruleta o on o.idopcion = r.idopcion and o.premio = 1
where date(r.fecha) <= '2026-08-20' and r.idofertacliente = 0
group by 1 order by 1;

-- Deben salir unos 441 premios repartidos entre marzo y agosto de 2026.


-- PASO 2. Marcar.
-- Solo toca filas de premio (premio = 1) y solo las que estan en cero, asi que
-- se puede correr dos veces sin hacer dano.
update resultado_ruleta r
   join opciones_ruleta o on o.idopcion = r.idopcion and o.premio = 1
   set r.idofertacliente = -1,
       r.fecha_dispersion = r.fecha,
       r.usuario_dispersion = 'MANUAL'
 where date(r.fecha) <= '2026-08-20'
   and r.idofertacliente = 0;

-- La fecha_dispersion queda igual a la fecha de la jugada y no a la de hoy: es
-- lo mas cercano a la verdad que tenemos, y poner la fecha de hoy diria que se
-- entregaron meses despues.


-- PASO 3. Verificar el resultado.
select case when r.idofertacliente < 0 then 'entregado a mano'
            when r.idofertacliente > 0 then 'dispersado por el sistema'
            else 'pendiente' end estado,
       count(*) premios, min(date(r.fecha)) desde, max(date(r.fecha)) hasta
from resultado_ruleta r
join opciones_ruleta o on o.idopcion = r.idopcion and o.premio = 1
group by 1 order by 1;

-- Esperado: unos 441 como 'entregado a mano' (marzo a 20-ago) y unos 58
-- 'pendiente' (21-ago en adelante), que son los que va a dispersar la pantalla.
