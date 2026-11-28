-- ---------------------------------------------------------------------------
-- El central revisa la firma del QR de la encuesta. En modo que SOLO ANOTA.
--
-- POR QUE ARRANCA SIN RECHAZAR. Los tiquetes impresos antes del cambio no
-- traen firma, y el cliente que guardo el suyo tiene derecho a contestar. Si el
-- central empezara a exigirla de un dia para otro, esos clientes se quedan por
-- fuera y nosotros perdemos encuestas de verdad. Entonces primero se ANOTA que
-- llego con cada encuesta, y cuando las que llegan sin firma bajen a casi cero,
-- se cambia QRENCUESTA_MODO a EXIGE.
--
-- LO QUE SE VA A VER AL PRINCIPIO: todo en SIN_FIRMA. Eso NO es una falla; es
-- que la pagina de pizzaamericana.co todavia no reenvia los dos parametros
-- nuevos (exp y f) al servicio. Justamente por eso se anota: el dia que la
-- pagina los reenvie, van a empezar a aparecer OK y asi se sabe, con datos, que
-- ya se puede exigir. Mientras tanto nada se rechaza.
--
-- LA CLAVE QUEDA VACIA A PROPOSITO. Tiene que ser IDENTICA a la de la tienda
-- (tiendaamericana.parametros, mismo nombre) y no se guarda en ningun
-- repositorio ni script: se pone directo en la base. Con la clave vacia el
-- central responde SIN_CLAVE y, otra vez, no rechaza nada.
--
-- Los parametros del central viven en general.parametros -ParametrosDAO usa
-- obtenerConexionBDGeneral-, no en pizzaamericana.
--
-- Idempotente.
-- ---------------------------------------------------------------------------

-- 1. Donde se anota: una columna mas en la tabla de origenes del paso 1.
--    MySQL no admite "add column if not exists", de ahi el rodeo.
set @hay := (select count(*) from information_schema.columns
             where table_schema = 'pizzaamericana'
               and table_name   = 'encuesta_origen'
               and column_name  = 'firma_estado');
set @sql := if(@hay = 0,
  'alter table pizzaamericana.encuesta_origen
     add column firma_estado varchar(20) null
       comment ''OK, SIN_FIRMA, INVALIDA, VENCIDA o SIN_CLAVE''',
  'select ''la columna firma_estado ya existia'' as nota');
prepare p from @sql; execute p; deallocate prepare p;

-- 2. La clave compartida con las tiendas. VACIA: hay que ponerla a mano.
insert into general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
select 'QRENCUESTA_CLAVE', 0, '', 0
 where not exists (select 1 from general.parametros where valorparametro = 'QRENCUESTA_CLAVE');

-- 3. El interruptor. LOG = solo anota. EXIGE = rechaza lo que no cuadre.
--    Se queda en LOG hasta que los SIN_FIRMA bajen a casi cero.
insert into general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
select 'QRENCUESTA_MODO', 0, 'LOG', 0
 where not exists (select 1 from general.parametros where valorparametro = 'QRENCUESTA_MODO');

-- Verificacion.
select valorparametro,
       case when valorparametro = 'QRENCUESTA_CLAVE'
            then case when coalesce(valortexto,'') = '' then 'VACIA (no se puede revisar nada todavia)'
                      else 'puesta' end
            else valortexto end as estado
  from general.parametros
 where valorparametro in ('QRENCUESTA_CLAVE','QRENCUESTA_MODO')
 order by valorparametro;

select column_name, column_type
  from information_schema.columns
 where table_schema = 'pizzaamericana' and table_name = 'encuesta_origen'
   and column_name = 'firma_estado';
