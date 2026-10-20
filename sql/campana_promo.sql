-- ---------------------------------------------------------------------------
-- Campanas de promocion: tiendas, dias y productos que disparan una pregunta
-- al finalizar el pedido.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- QUE RESUELVE
--
-- "Por la compra del producto X, en estas tiendas, estos dias, preguntarle al
-- cliente si quiere el premio, y al final de la semana mandarselo por correo."
-- Hoy eso no existe: cada promocion de este tipo habria que escribirla en el
-- codigo del POS y del central, y borrarla cuando termine.
--
-- El primer caso es la boleta de Cinepolis por el Combo Insuperable, pero las
-- tablas no saben nada de Cinepolis: la proxima promocion de esta forma son
-- cuatro filas de configuracion y cero codigo.
--
-- DE DONDE SALE LA FORMA
--
-- Es la misma que ya usa la campana "15 minutos o gratis"
-- (campana_15min_config + campana_15min_horario_dia + campana_15min_exclusion):
-- una cabecera con fechas y mensaje, y tablas satelite para dias y productos.
-- Se copia a proposito: esa arquitectura ya esta probada de punta a punta, el
-- POS ya sabe leerla y marcar localmente, y el equipo ya la conoce.
--
-- NO se cuelga de campana_15min_config porque esa tiene minutos_promesa y
-- porcentaje_retencion: es de UN tipo de promocion, y meterle otra la vuelve
-- un cajon de sastre.
--
-- OJO CON EL NOMBRE: en esta base "campana" ya significa dos cosas distintas.
-- La tabla `campana` es la de envios de publicidad y `campana_15min_*` es la de
-- 15 minutos. Esta tercera se llama campana_promo para que no se confundan.
--
-- EL PRODUCTO NO SE IDENTIFICA IGUAL EN LOS DOS LADOS
--
-- Esto es lo que obliga a la columna `ambito`, y no es un detalle:
--
--   En la TIENDA el Combo Insuperable es un PRODUCTO propio (480 XL, 481 GD).
--   En el CENTRAL no existe ese producto: es una EXCEPCION DE PRECIO sobre la
--   pizza base (excepcion 50 sobre Pizza XL, excepcion 51 sobre Pizza GD).
--
-- Son dos catalogos distintos, como ya pasa con forma_pago. Por eso cada fila
-- dice a que ambito pertenece y, cuando es del central, puede exigir ademas la
-- excepcion. La misma convencion de venta_integral_categoria_item.
-- ---------------------------------------------------------------------------

-- PASO 1. La campana.
create table if not exists campana_promo (
    idcampana           int           not null auto_increment,
    nombre              varchar(80)   not null,
    activo              char(1)       not null default 'N',
    fecha_desde         date              null,
    fecha_hasta         date              null,
    -- La pregunta tal como la lee quien atiende. Va en la tabla y no en el
    -- codigo para poder corregirle una palabra sin desplegar.
    pregunta            varchar(200)  not null default '',
    -- Que oferta emite el codigo del premio. Apunta a `oferta`, que ya maneja
    -- un uso, caducidad, tope por cliente y cupo maximo. Si va en cero, la
    -- campana solo marca y el premio se entrega por fuera del sistema.
    idoferta            int           not null default 0,
    -- Si se exige correo para poder entregar el premio. Con 'S' la pregunta
    -- tiene que capturarlo: en mostrador solo entre el 20% y el 35% de los
    -- clientes lo tienen registrado (medido el 2026-10-19 en las tres sedes),
    -- asi que verificar el que ya existe dejaria por fuera a la mayoria.
    exige_correo        char(1)       not null default 'S',
    fechacreacion       timestamp     not null default current_timestamp,
    primary key (idcampana)
) engine=InnoDB default charset=utf8;

-- PASO 2. En que tiendas aplica. Sin filas = en ninguna, nunca en todas:
-- una promocion que se prende sola en las 11 por un descuido cuesta plata.
create table if not exists campana_promo_tienda (
    idcampana           int           not null,
    idtienda            int           not null,
    primary key (idcampana, idtienda)
) engine=InnoDB default charset=utf8;

-- PASO 3. Que dias y a que horas. Mismo modelo de campana_15min_horario_dia.
--
-- dia_semana va 1=LUNES .. 7=DOMINGO, igual que campana_15min_horario_dia y
-- NO como el dayofweek() de MySQL, que empieza en domingo. La traduccion que
-- usa la campana de 15 minutos es:
--     ((dayofweek(curdate()) + 5) mod 7) + 1
-- Tener dos tablas de campana numerando los dias al reves seria una trampa
-- para el que escriba la tercera.
create table if not exists campana_promo_dia (
    idcampana           int           not null,
    dia_semana          tinyint       not null,
    activo              char(1)       not null default 'S',
    todo_el_dia         char(1)       not null default 'S',
    hora_desde          time              null,
    hora_hasta          time              null,
    primary key (idcampana, dia_semana)
) engine=InnoDB default charset=utf8;

-- PASO 4. Que dispara la pregunta.
--   ambito = 'TIENDA'         -> idproducto del catalogo de la tienda
--   ambito = 'CONTACTCENTER'  -> idproducto del catalogo del central, y si
--                                idexcepcion > 0, solo con esa excepcion
create table if not exists campana_promo_producto (
    iditem              int           not null auto_increment,
    idcampana           int           not null,
    ambito              varchar(15)   not null,
    idproducto          int           not null,
    idexcepcion         int           not null default 0,
    activo              char(1)       not null default 'S',
    primary key (iditem),
    key ix_campana_promo_producto (idcampana, ambito, idproducto)
) engine=InnoDB default charset=utf8;

-- PASO 5. Quien la pidio.
--
-- Se guarda tambien el que dijo que NO, igual que pedido_sin_cliente: sin eso
-- no hay forma de saber si la promocion no se pidio porque no interesa o
-- porque nadie pregunto, que son dos problemas distintos.
--
-- El estado deja ver en que va cada uno sin cruzar tablas:
--   MARCADO    el cliente dijo que si, falta emitir
--   EMITIDO    ya tiene codigo (idofertacliente)
--   ENVIADO    el correo salio
--   DESCARTADO el pedido se anulo, o el producto ya no estaba al cierre
create table if not exists pedido_campana_promo (
    idregistro          int           not null auto_increment,
    idcampana           int           not null,
    idtienda            int           not null,
    idpedidotienda      int           not null default 0,
    idpedido            int           not null default 0,
    idcliente           int           not null default 0,
    correo              varchar(120)  not null default '',
    solicito            char(1)       not null default 'N',
    usuario             varchar(40)   not null default '',
    estado              varchar(12)   not null default 'MARCADO',
    idofertacliente     int           not null default 0,
    observacion         varchar(160)  not null default '',
    fecha_hora          timestamp     not null default current_timestamp,
    primary key (idregistro),
    -- Un pedido no puede quedar marcado dos veces en la misma campana. Es la
    -- misma guarda que falto en las formas de pago y termino en doble cobro.
    unique key uq_pedido_campana (idcampana, idtienda, idpedidotienda),
    key ix_campana_promo_estado (idcampana, estado)
) engine=InnoDB default charset=utf8;

-- ---------------------------------------------------------------------------
-- PASO 6. La primera campana: boleta de Cinepolis por el Combo Insuperable.
--
-- Queda en activo = 'N' A PROPOSITO. Las tablas existen pero todavia no hay
-- codigo que las lea: ni la pregunta en el POS, ni la del contact center, ni el
-- proceso semanal que emite y envia. Prenderla antes de eso no hace nada, y
-- dejarla en 'S' seria sembrar una sorpresa para el dia que el codigo llegue.
--
-- Las fechas son las del volante (19 y 20 de octubre de 2026). Quedan como
-- quedaron: sirven de ejemplo de como se configura, no para que corra. Para la
-- proxima se cambian las dos fechas y se pone activo = 'S'.
--
-- idoferta va en cero porque todavia no se creo la oferta que emite el codigo.
-- Cuando se cree, se actualiza aqui. Recordar que esa oferta DEBE tener
-- dias_caducidad: desde el 2026-10-19 un correo con codigo no sale sin fecha
-- de vencimiento.
-- ---------------------------------------------------------------------------
insert into campana_promo (nombre, activo, fecha_desde, fecha_hasta, pregunta, idoferta, exige_correo)
select 'Boleta Cinepolis por Combo Insuperable', 'N', '2026-10-19', '2026-10-20',
       'El cliente lleva Combo Insuperable. Desea la boleta de Cinepolis? Se le envia al correo.',
       0, 'S'
  from dual
 where not exists (select 1 from campana_promo
                    where nombre = 'Boleta Cinepolis por Combo Insuperable');

set @idcamp = (select idcampana from campana_promo
                where nombre = 'Boleta Cinepolis por Combo Insuperable' limit 1);

-- Las tres sedes del volante: Envigado, La Mota e Itagui.
insert into campana_promo_tienda (idcampana, idtienda)
select @idcamp, t.idtienda from tienda t where t.idtienda in (5, 7, 8)
   and not exists (select 1 from campana_promo_tienda c
                    where c.idcampana = @idcamp and c.idtienda = t.idtienda);

-- Todos los dias dentro del rango de fechas. El rango ya limita a los dos dias
-- del volante; esta tabla esta para las campanas que aplican solo ciertos dias
-- de la semana (por ejemplo "todos los martes").
insert into campana_promo_dia (idcampana, dia_semana, activo, todo_el_dia)
select @idcamp, d.n, 'S', 'S'
  from (select 1 n union select 2 union select 3 union select 4
        union select 5 union select 6 union select 7) d
 where not exists (select 1 from campana_promo_dia c
                    where c.idcampana = @idcamp and c.dia_semana = d.n);

-- Lo que dispara la pregunta, en los dos catalogos.
--
--   TIENDA:        481 = Combo Insuperable GD ($57.900)
--                  480 = Combo Insuperable XL ($67.900)
--   CONTACTCENTER: producto 7 (Pizza GD) con excepcion 51 = GD Insuperable
--                  producto 6 (Pizza XL) con excepcion 50 = XL Insuperable
--
-- La MD Insuperable (excepcion 52, $47.900) NO entra: el volante es solo
-- Grande y XGrande.
insert into campana_promo_producto (idcampana, ambito, idproducto, idexcepcion, activo)
select @idcamp, v.ambito, v.idproducto, v.idexcepcion, 'S'
  from (select 'TIENDA' ambito, 481 idproducto, 0 idexcepcion
        union all select 'TIENDA', 480, 0
        union all select 'CONTACTCENTER', 7, 51
        union all select 'CONTACTCENTER', 6, 50) v
 where not exists (select 1 from campana_promo_producto c
                    where c.idcampana = @idcamp and c.ambito = v.ambito
                      and c.idproducto = v.idproducto and c.idexcepcion = v.idexcepcion);

-- ---------------------------------------------------------------------------
-- Como quedo.
-- ---------------------------------------------------------------------------
select c.idcampana, c.nombre, c.activo, c.fecha_desde, c.fecha_hasta, c.exige_correo,
       (select count(*) from campana_promo_tienda x where x.idcampana = c.idcampana) tiendas,
       (select count(*) from campana_promo_dia x where x.idcampana = c.idcampana) dias,
       (select count(*) from campana_promo_producto x where x.idcampana = c.idcampana) disparadores
  from campana_promo c;

select p.ambito, p.idproducto, p.idexcepcion,
       case when p.ambito = 'CONTACTCENTER'
            then (select e.descripcion from excepcion_precio e where e.idexcepcion = p.idexcepcion)
            else '(producto de la tienda)' end as que_es
  from campana_promo_producto p
  join campana_promo c on c.idcampana = p.idcampana
 order by p.ambito, p.idproducto;
