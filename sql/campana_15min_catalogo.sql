-- ---------------------------------------------------------------------------
-- Campana "15 MINUTOS O GRATIS" en punto de venta.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Config y exclusiones administrables desde el central; el POS las lee via
-- servicio (ObtenerCampana15MinActiva) y nunca bloquea la venta si no hay
-- respuesta. campana_15min_aplicado es el registro que deja el POS cuando la
-- campana aplico a un pedido (al enviarlo a cocina). campana_15min_incumplimiento
-- es la cola de revision para Servicio al Cliente, alimentada por el job
-- ServicioDeteccionCampana15Min en Servicios.
--
-- Correr esto antes de desplegar el war con las pantallas/servlets nuevos.
-- ---------------------------------------------------------------------------

create table if not exists campana_15min_config (
    idcampana                          int           not null auto_increment,
    nombre                              varchar(80)   not null default '15 MINUTOS O GRATIS',
    activo                              char(1)       not null default 'N',
    mensaje_operario                    varchar(300)  not null default '',
    mensaje_factura                     varchar(300)  not null default '',
    fecha_desde                         date          null,
    fecha_hasta                         date          null,
    dias_semana                         varchar(7)    not null default 'SSSSSSS', -- lun..dom, S/N
    hora_desde                          time          null,
    hora_hasta                          time          null,
    minutos_promesa                     int           not null default 15,
    porcentaje_retencion_medio_virtual  decimal(5,2)  not null default 5.00,
    fechacreacion                       datetime      not null default current_timestamp,
    primary key (idcampana)
);

create table if not exists campana_15min_exclusion (
    idexclusion       int           not null auto_increment,
    idcampana         int           not null,
    nombre_producto   varchar(120)  not null, -- match case-insensitive contra el nombre local del POS
    primary key (idexclusion),
    key ix_campana_15min_exclusion_campana (idcampana)
);

create table if not exists campana_15min_aplicado (
    idpedidotienda        int           not null,
    idtienda              int           not null,
    idcampana             int           not null,
    fecha_hora_inicio     datetime      not null,
    valor_base_pizza      decimal(10,2) not null default 0,
    nombre_cliente        varchar(150)  not null default '',
    celular_cliente       varchar(15)   not null default '',
    idformapago_virtual   char(1)       null, -- S/N, se completa al cerrar el pago
    estado_cumplido       char(1)       null, -- null = aun sin vencer/revisar, S/N cuando el job lo evalua
    fecha_registro        datetime      not null default current_timestamp,
    primary key (idpedidotienda, idtienda)
);

create table if not exists campana_15min_incumplimiento (
    idsolicitud            int           not null auto_increment,
    idpedidotienda         int           not null,
    idtienda               int           not null,
    fecha_hora_inicio      datetime      not null,
    fecha_deteccion        datetime      not null default current_timestamp,
    valor_base_pizza       decimal(10,2) not null default 0,
    retencion_aplicada     decimal(10,2) not null default 0,
    valor_a_devolver       decimal(10,2) not null default 0,
    estado                 varchar(12)   not null default 'PENDIENTE', -- PENDIENTE/APROBADA/RECHAZADA
    usuario_revisa          varchar(80)   not null default '',
    fecha_revision          datetime      null,
    observacion_revision    varchar(300)  not null default '',
    primary key (idsolicitud),
    key ix_campana_15min_incumplimiento_pedido (idpedidotienda, idtienda),
    key ix_campana_15min_incumplimiento_estado (estado)
);

-- Siembra inicial: campana desactivada por defecto (activo='N') hasta que el
-- usuario confirme fechas/horario reales; exclusiones ya conocidas.
insert into campana_15min_config
    (nombre, activo, mensaje_operario, mensaje_factura, dias_semana, minutos_promesa, porcentaje_retencion_medio_virtual)
select '15 MINUTOS O GRATIS', 'N',
    'Recuerda indicarle al cliente que su pedido aplica para 15 MINUTOS O GRATIS.',
    'Tu pedido aplica para 15 MINUTOS O GRATIS',
    'SSSSSSS', 15, 5.00
where not exists (select 1 from campana_15min_config);

insert into campana_15min_exclusion (idcampana, nombre_producto)
select c.idcampana, x.nombre
from campana_15min_config c
cross join (select 'THE WORKS' as nombre union all select 'CON TODO') x
where not exists (
    select 1 from campana_15min_exclusion e
    where e.idcampana = c.idcampana and e.nombre_producto = x.nombre
);
