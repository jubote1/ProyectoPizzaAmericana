-- ---------------------------------------------------------------------------
-- Venta Integral: catalogo parametrizable y resumen semanal por tienda.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Hoy la estrategia "Venta Integral" se mide corriendo a mano, cada semana,
-- un set de consultas SQL por categoria (adiciones, deditos, estofadas,
-- especialidades premium...) contra la base local de cada tienda y otro set
-- contra el central (pedidos de contact center, origen='C'). Estas tablas
-- reemplazan esas consultas hardcodeadas por un catalogo administrable desde
-- ProyectoPizzaAmericana (menuadm > Monitoreo), y una tabla donde el proceso
-- de Servicios deja el cierre de cada semana (lunes a domingo).
--
-- Correr esto ANTES de desplegar el war del central con las pantallas nuevas,
-- y antes de correr por primera vez ServicioSemanalVentaIntegral en Servicios.
-- ---------------------------------------------------------------------------

-- PASO 1. Catalogo de categorias.
-- medicion_tienda / medicion_cc guardan COMO se mide cada categoria, para que
-- el proceso semanal arme el SQL dinamicamente en vez de tener las 7
-- categorias legacy escritas en el codigo.
create table if not exists venta_integral_categoria (
    idcategoria             int           not null auto_increment,
    nombre                  varchar(60)   not null,
    abreviatura             varchar(20)   not null default '',
    tipodato                varchar(15)   not null,  -- PRODUCTO | ESPECIALIDAD
    medicion_tienda         varchar(20)   not null,  -- SUMA_CANTIDAD | CONTEO_FILAS
    excluye_anulados_tienda char(1)       not null default 'S',
    filtro_estacion_tienda  varchar(20)   not null default '%servid%',
    medicion_cc             varchar(30)   not null,  -- CONTEO_PRODUCTO | CONTEO_ESPECIALIDAD_CON_DEDUP
    activo                  char(1)       not null default 'S',
    orden                   int           not null default 0,
    fechacreacion           datetime      not null default current_timestamp,
    primary key (idcategoria)
);

-- PASO 2. Items de cada categoria (idproducto o idespecialidad segun tipodato).
-- ambito es obligatorio: Estofadas ya prueba que el mismo concepto tiene
-- codigos distintos en tienda (469,470) que en el central (311,312), asi que
-- nunca se puede asumir que un idproducto sirve para los dos lados.
create table if not exists venta_integral_categoria_item (
    iditem     int         not null auto_increment,
    idcategoria int        not null,
    ambito     varchar(15) not null,  -- TIENDA | CONTACTCENTER
    idvalor    int         not null,
    activo     char(1)     not null default 'S',
    primary key (iditem),
    key idcategoria_ambito (idcategoria, ambito)
);

-- PASO 3. Resumen semanal por tienda y categoria, lo que llena el proceso de
-- Servicios cada lunes al cerrar la semana anterior. La llave unica hace que
-- un reproceso sea idempotente (upsert, no duplica filas).
create table if not exists venta_integral_resumen_semanal (
    idresumen              int           not null auto_increment,
    idtienda               int           not null,
    idcategoria            int           not null,
    semanainicio           date          not null,
    semanafin              date          not null,
    cantidad_tienda        decimal(10,2) not null default 0,
    cantidad_contactcenter decimal(10,2) not null default 0,
    cantidad_total         decimal(10,2) not null default 0,
    fechaproceso           datetime      not null default current_timestamp,
    primary key (idresumen),
    unique key tienda_categoria_semana (idtienda, idcategoria, semanainicio, semanafin)
);

-- PASO 4. Seed de las 7 categorias legacy, con sus valores reales, para no
-- perder comparabilidad contra lo que se venia midiendo a mano.
--
-- OJO, dos inconsistencias heredadas que se preservan a proposito (documentar,
-- no corregir en silencio -- el usuario decide si son bug o intencional):
--  - Estofadas/Adiciones/Deditos NO excluian anulados en tienda; las de
--    especialidad si. Por eso excluye_anulados_tienda difiere entre filas.
--  - El filtro de estacion es '%servid%' (estricto) en las de especialidad y
--    '%serv%' (mas laxo) en Estofadas/Adiciones/Deditos.
insert into venta_integral_categoria
    (nombre, abreviatura, tipodato, medicion_tienda, excluye_anulados_tienda, filtro_estacion_tienda, medicion_cc, orden)
values
    ('Americana Premium', 'PREMIUM', 'ESPECIALIDAD', 'SUMA_CANTIDAD', 'S', '%servid%', 'CONTEO_ESPECIALIDAD_CON_DEDUP', 10),
    ('Super',              'SUPER',   'ESPECIALIDAD', 'SUMA_CANTIDAD', 'S', '%servid%', 'CONTEO_ESPECIALIDAD_CON_DEDUP', 20),
    ('Paisa',              'PAISA',   'ESPECIALIDAD', 'SUMA_CANTIDAD', 'S', '%servid%', 'CONTEO_ESPECIALIDAD_CON_DEDUP', 30),
    ('Mexicana',           'MEXICANA','ESPECIALIDAD', 'SUMA_CANTIDAD', 'S', '%servid%', 'CONTEO_ESPECIALIDAD_CON_DEDUP', 40),
    ('Estofadas',          'ESTOFADA','PRODUCTO',     'CONTEO_FILAS',  'N', '%serv%',   'CONTEO_PRODUCTO', 50),
    ('Adiciones',          'ADICION', 'PRODUCTO',     'CONTEO_FILAS',  'N', '%serv%',   'CONTEO_PRODUCTO', 60),
    ('Deditos',            'DEDITO',  'PRODUCTO',     'CONTEO_FILAS',  'N', '%serv%',   'CONTEO_PRODUCTO', 70)
on duplicate key update idcategoria = idcategoria;

-- PASO 5. Items de cada categoria. Los idproducto de tienda de las 4
-- especialidades son los 4 tamanos; el idvalor de contact center para esas
-- mismas 4 es el idespecialidad (39/28/21/14), no un idproducto.
insert into venta_integral_categoria_item (idcategoria, ambito, idvalor)
select idcategoria, 'TIENDA', v.idproducto
from venta_integral_categoria c
join (
    select 'Americana Premium' as nombre, 597 as idproducto union all
    select 'Americana Premium', 598 union all
    select 'Americana Premium', 599 union all
    select 'Americana Premium', 600 union all
    select 'Super', 324 union all
    select 'Super', 351 union all
    select 'Super', 378 union all
    select 'Super', 405 union all
    select 'Paisa', 317 union all
    select 'Paisa', 344 union all
    select 'Paisa', 371 union all
    select 'Paisa', 398 union all
    select 'Mexicana', 310 union all
    select 'Mexicana', 337 union all
    select 'Mexicana', 364 union all
    select 'Mexicana', 391 union all
    select 'Estofadas', 469 union all
    select 'Estofadas', 470 union all
    select 'Adiciones', 12 union all
    select 'Adiciones', 42 union all
    select 'Adiciones', 60 union all
    select 'Adiciones', 78 union all
    select 'Deditos', 4
) v on v.nombre = c.nombre
where not exists (
    select 1 from venta_integral_categoria_item i
    where i.idcategoria = c.idcategoria and i.ambito = 'TIENDA' and i.idvalor = v.idproducto
);

insert into venta_integral_categoria_item (idcategoria, ambito, idvalor)
select idcategoria, 'CONTACTCENTER', v.idvalor
from venta_integral_categoria c
join (
    select 'Americana Premium' as nombre, 39 as idvalor union all
    select 'Super', 28 union all
    select 'Paisa', 21 union all
    select 'Mexicana', 14 union all
    select 'Estofadas', 311 union all
    select 'Estofadas', 312 union all
    select 'Adiciones', 12 union all
    select 'Adiciones', 42 union all
    select 'Adiciones', 60 union all
    select 'Adiciones', 78 union all
    select 'Deditos', 4
) v on v.nombre = c.nombre
where not exists (
    select 1 from venta_integral_categoria_item i
    where i.idcategoria = c.idcategoria and i.ambito = 'CONTACTCENTER' and i.idvalor = v.idvalor
);

-- PASO 6. Verificacion: 7 categorias, cada una con items de tienda y de CC.
select c.idcategoria, c.nombre, c.medicion_tienda, c.medicion_cc,
       sum(case when i.ambito = 'TIENDA' then 1 else 0 end) items_tienda,
       sum(case when i.ambito = 'CONTACTCENTER' then 1 else 0 end) items_cc
from venta_integral_categoria c
left join venta_integral_categoria_item i on i.idcategoria = c.idcategoria
group by c.idcategoria, c.nombre, c.medicion_tienda, c.medicion_cc
order by c.orden;
