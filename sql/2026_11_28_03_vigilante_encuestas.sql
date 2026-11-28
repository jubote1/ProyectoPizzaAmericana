-- ---------------------------------------------------------------------------
-- El vigilante de encuestas: tabla de control, parametros y destinatarios.
--
-- QUE VIGILA. Encuestas llenadas en masa. La regla que se valido contra los
-- datos reales el 27-nov-2026: un mismo telefono con 5 o mas encuestas en un
-- dia y una misma tienda. Con esa regla aparecieron 16 telefonos y 315 pedidos
-- entre mayo y noviembre, incluidas 75 encuestas de un solo telefono en una
-- noche y 27 en 23 minutos.
--
-- QUE HACE CUANDO ENCUENTRA. Dos cosas:
--   1. RETIENE EL PREMIO, no la encuesta. La encuesta se queda -puede ser buena
--      informacion- pero el premio de ruleta que salio de ella no se entrega
--      hasta que alguien lo revise. Se marca igual que el bloqueo del 27-nov:
--      idofertacliente = -2, que la pantalla de dispersion no puede tocar
--      porque solo actualiza "WHERE idofertacliente = 0".
--   2. AVISA por correo.
--
-- POR QUE RETENER EL PREMIO Y NO LA ENCUESTA. El motivo del fraude es el
-- premio. Rechazar la encuesta castiga tambien al cliente honesto que cae en la
-- regla por casualidad -una familia, un pedido corregido varias veces-, y
-- encima nos deja sin la informacion. Reteniendo el premio, equivocarse sale
-- barato: alguien mira y lo suelta.
--
-- Idempotente.
-- ---------------------------------------------------------------------------

-- 1. Lo ya avisado, para no repetir el mismo aviso todos los dias.
create table if not exists pizzaamericana.alerta_encuesta_masiva (
  idalerta        int          not null auto_increment,
  regla           varchar(20)  not null comment 'TELEFONO o NAVEGADOR',
  llave           varchar(120) not null comment 'el telefono, o la IP + navegador',
  idtienda        int          not null,
  dia             date         not null,
  encuestas       int          not null,
  minutos         int          not null comment 'entre la primera y la ultima',
  premios_retenidos int        not null default 0,
  fecha_aviso     timestamp    not null default current_timestamp,
  primary key (idalerta),
  unique key uk_alerta (regla, llave, idtienda, dia),
  key idx_alerta_fecha (fecha_aviso)
) engine=InnoDB default charset=latin1;

-- 2. Parametros. Viven en general.parametros, igual que los demas del central.
insert into general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
select 'VIGILANTEENCUESTA_UMBRAL', 5, '', 0
 where not exists (select 1 from general.parametros where valorparametro = 'VIGILANTEENCUESTA_UMBRAL');

insert into general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
select 'VIGILANTEENCUESTA_DIAS', 2, '', 0
 where not exists (select 1 from general.parametros where valorparametro = 'VIGILANTEENCUESTA_DIAS');

-- 'S' retiene los premios; 'N' solo avisa. Se puede apagar sin redesplegar.
insert into general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
select 'VIGILANTEENCUESTA_RETIENE', 0, 'S', 0
 where not exists (select 1 from general.parametros where valorparametro = 'VIGILANTEENCUESTA_RETIENE');

-- 3. Destinatarios. Se copian de RESULTADORULETA, que es la gente que ya se
--    ocupa de los premios de la ruleta. No se inventa ningun correo aqui; si
--    hay que cambiar la lista, se cambia en la tabla.
insert into general.parametros_correo (valorparametro, correo)
select 'ALERTAFRAUDEENCUESTA', c.correo
  from general.parametros_correo c
 where c.valorparametro = 'RESULTADORULETA'
   and not exists (select 1 from general.parametros_correo d
                    where d.valorparametro = 'ALERTAFRAUDEENCUESTA'
                      and d.correo = c.correo);

-- Verificacion.
select 'destinatarios de la alerta' as que, count(*) as cuantos
  from general.parametros_correo where valorparametro = 'ALERTAFRAUDEENCUESTA'
union all
select 'avisos ya enviados', count(*) from pizzaamericana.alerta_encuesta_masiva;

select valorparametro, valornumerico, valortexto
  from general.parametros
 where valorparametro like 'VIGILANTEENCUESTA%'
 order by valorparametro;
