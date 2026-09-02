-- ---------------------------------------------------------------------------
-- Dispersion de premios de la ruleta de encuestas.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Objetivo: que la asignacion de la oferta y el aviso al cliente dejen de
-- hacerse cliente por cliente a mano, y queden en una pantalla donde se
-- consulta por fecha y se dispersa con un boton.
--
-- Casi todo lo necesario ya existe: oferta_cliente.fecha_mensaje dice si ya se
-- aviso, y utilizada / uso_oferta / usuario_uso dicen si ya se redimio. Lo
-- unico que falta es saber QUE oferta se le asigno a cada resultado de ruleta.
-- ---------------------------------------------------------------------------

-- PASO 1. El enlace entre el resultado de la ruleta y la oferta asignada.
-- Sirve de dos cosas a la vez: es el enlace para consultar codigo, envio y uso,
-- y es la marca de que ese resultado ya fue dispersado (cero = pendiente). Al
-- ser la misma columna, no hay forma de que las dos cosas queden en desacuerdo.
alter table resultado_ruleta
    add column idofertacliente int not null default 0,
    add column fecha_dispersion datetime null,
    add column usuario_dispersion varchar(20) not null default '';

-- Para que la pantalla filtre rapido por fecha y por pendientes.
alter table resultado_ruleta add key fecha_dispersion (fecha, idofertacliente);


-- PASO 2. Que oferta corresponde a cada premio.
-- Va en tabla y no en el codigo a proposito: cuando cambien un premio o creen
-- una oferta nueva, se ajusta con un update y no hay que recompilar ni
-- desplegar nada.
create table if not exists ruleta_oferta (
    idopcion    int          not null,
    idoferta    int          not null,
    activo      char(1)      not null default 'S',
    observacion varchar(200) not null default '',
    primary key (idopcion)
);

insert into ruleta_oferta (idopcion, idoferta, observacion) values
    (248, 52, '20% de descuento -> Encuesta Descuento 20%'),
    (458, 45, 'Deditos gratis -> Encueta Deditos Masa Queso - Madurito'),
    (460, 45, 'Madurito gratis -> la misma oferta cubre deditos y madurito'),
    (461, 49, 'Pizzeta Gratis -> RECUPERACION PIZZETA'),
    (462, 52, 'Premio Sorpresa -> se entrega como 20% de descuento')
on duplicate key update idoferta = values(idoferta), observacion = values(observacion);

-- Las opciones 459 (Gira de nuevo) y 463 (Sin premio) no llevan oferta: no son
-- premio. Si algun dia aparece un premio sin fila aqui, la pantalla lo muestra
-- como pendiente de configurar en vez de dispersarlo mal.


-- PASO 3. Verificacion.
select o.idopcion, o.titulo as premio, o.premio as es_ganador, o.activo as premio_activo,
       r.idoferta, f.nombre_oferta, f.dias_caducidad, f.codigo_promocional, f.habilitado
from opciones_ruleta o
left join ruleta_oferta r on r.idopcion = o.idopcion
left join oferta f on f.idoferta = r.idoferta
order by o.premio desc, o.idopcion;

-- Revise en el resultado que cada premio activo tenga oferta, que la oferta
-- este habilitada y con codigo_promocional = 'S'. Ojo con dias_caducidad: las
-- ofertas 45, 52 y 53 tienen 15-16 dias, pero la 49 (Pizzeta) tiene 180.


-- PASO 4. Caducidad de la Pizzeta.
-- La oferta 49 nacio para recuperacion de clientes y quedo con 180 dias. Como
-- premio de encuesta debe alinearse con las demas (45 y 52 tienen 15 y 16).
--
-- Bajarla no afecta el uso que ya tiene: de 20 pizzetas redimidas historicamente,
-- el promedio de redencion es 4,8 dias, el maximo 14, y ninguna paso de 15.
update oferta set dias_caducidad = 15 where idoferta = 49;

-- Nota: la oferta 49 queda compartida entre recuperacion de clientes y premio de
-- ruleta. Para poder separarlas en los reportes, la dispersion marca cada
-- asignacion con usuario_ingreso = 'RULETA' y una observacion que dice de que
-- encuesta viene. No hace falta crear una oferta aparte.


-- PASO 5. Verificacion final: cada premio activo con su oferta y sus dias.
select o.idopcion, o.titulo as premio, o.activo as premio_activo,
       r.idoferta, f.nombre_oferta, f.dias_caducidad, f.codigo_promocional, f.habilitado
from opciones_ruleta o
left join ruleta_oferta r on r.idopcion = o.idopcion
left join oferta f on f.idoferta = r.idoferta
where o.premio = 1
order by o.idopcion;
