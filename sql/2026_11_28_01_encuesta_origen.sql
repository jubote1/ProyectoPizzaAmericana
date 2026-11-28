-- ---------------------------------------------------------------------------
-- De donde llego cada encuesta: IP, navegador y hora.
--
-- POR QUE. Hoy no se guarda nada de quien envia una encuesta. Cuando aparecio
-- que un mismo telefono contesto 75 encuestas en una noche cambiando el numero
-- de pedido en la direccion del QR, lo unico con que se pudo medir fue la hora
-- de encuesta_servicio. No hubo forma de saber si las 75 salieron del mismo
-- equipo, de la tienda o de una casa. Sin registro no hay investigacion
-- posible, y por eso esto va primero, antes que cualquier bloqueo.
--
-- NO BLOQUEA NADA Y NO CAMBIA NADA PARA EL CLIENTE. Solo deja constancia.
--
-- UNA FILA POR ENVIO, no por respuesta: encuesta_servicio guarda una fila por
-- pregunta (seis por encuesta) y repetir ahi la IP seis veces seria guardar lo
-- mismo seis veces y tener que mantenerlas de acuerdo.
--
-- QUE SIRVE DE CADA DATO:
--   ip            lo que ve el servidor. Si hay proxy adelante, es el del proxy.
--   ip_reenviada  cabecera X-Forwarded-For: ahi viene la IP real del visitante
--                 cuando la peticion paso por un proxy, que es el caso de
--                 pizzaamericana.co. Se guardan las dos porque ninguna sola es
--                 confiable: la primera puede ser siempre la misma, y la segunda
--                 la puede falsear quien arma la peticion a mano.
--   navegador     User-Agent.
--   idioma        Accept-Language. Junto con el navegador distingue equipos que
--                 comparten la misma IP, que es lo normal en el wifi de una
--                 tienda.
--   referencia    Referer: de que pagina venia. Dice si entro por el QR o si
--                 escribio la direccion.
--
-- Ninguno de estos datos identifica a una persona por si solo, y asi hay que
-- leerlos: 75 envios desde el mismo navegador es evidencia aunque la IP no
-- tenga nombre.
--
-- Idempotente.
-- ---------------------------------------------------------------------------

create table if not exists pizzaamericana.encuesta_origen (
  idorigen      int          not null auto_increment,
  idtienda      int          not null,
  idpedido      int          not null,
  accion        varchar(20)  not null comment 'ENCUESTA o RULETA',
  fecha         timestamp    not null default current_timestamp,
  ip            varchar(45)      null,
  ip_reenviada  varchar(255)     null,
  navegador     varchar(255)     null,
  idioma        varchar(60)      null,
  referencia    varchar(255)     null,
  primary key (idorigen),
  key idx_origen_pedido (idtienda, idpedido),
  key idx_origen_fecha (fecha),
  key idx_origen_ip (ip, fecha)
) engine=InnoDB default charset=latin1;

select count(*) as filas_registradas from pizzaamericana.encuesta_origen;
