-- ---------------------------------------------------------------------------
-- Texto que se le muestra al cliente en el correo, por premio.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- ruleta_oferta.observacion pasa a ser el mensaje que VE EL CLIENTE, no una nota
-- interna. Solo se llena para los premios cuyo nombre no dice en que consisten:
-- "Premio Sorpresa" deja al cliente sin saber que gano. En los demas se deja
-- vacio, porque repetir "Pizzeta Gratis" debajo de "Pizzeta Gratis" no aporta.
--
-- La documentacion del mapeo premio -> oferta, que antes vivia en esta columna,
-- queda en sql/dispersion_premios_ruleta.sql y en el historial de git.
-- ---------------------------------------------------------------------------

-- PASO 1. Ver como esta hoy.
select o.idopcion, o.titulo as premio, r.idoferta, r.observacion
from opciones_ruleta o join ruleta_oferta r on r.idopcion = o.idopcion
where o.premio = 1 order by o.idopcion;

-- PASO 2. Vaciar los que no necesitan explicacion.
update ruleta_oferta set observacion = '' where idopcion <> 462;

-- PASO 3. El mensaje del Premio Sorpresa.
-- Se redacta como se lo diria a un cliente, no como nota tecnica: este texto le
-- llega tal cual en el correo, debajo del nombre del premio.
update ruleta_oferta
   set observacion = 'Tu sorpresa es un 20% de descuento en tu próxima compra.'
 where idopcion = 462;

-- PASO 4. Verificar.
select o.idopcion, o.titulo as premio, r.observacion as mensaje_al_cliente
from opciones_ruleta o join ruleta_oferta r on r.idopcion = o.idopcion
where o.premio = 1 order by o.idopcion;

-- Solo la opcion 462 (Premio Sorpresa) debe tener texto. Si algun dia se agrega
-- otro premio cuyo nombre no se explique solo, se le pone su mensaje aqui y el
-- correo lo empieza a mostrar sin necesidad de desplegar nada.
