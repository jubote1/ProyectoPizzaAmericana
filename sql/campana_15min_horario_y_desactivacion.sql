-- ---------------------------------------------------------------------------
-- Campana "15 minutos o gratis": horario variable por dia de la semana, y
-- desactivacion por tienda (por dia, se reactiva sola al cambiar de fecha).
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Reemplaza el horario unico (dias_semana + hora_desde/hora_hasta en
-- campana_15min_config) por una fila por dia de la semana: el horario real es
-- "lunes a jueves todo el dia, viernes hasta las 7pm, sabado y domingo hasta
-- las 5pm" -- no cabe en un solo horario global.
--
-- campana_15min_tienda_desactivada_dia la escribe el POS cuando el
-- administrador de una tienda desactiva la campana por el dia (horno danado,
-- falta de personal, etc.). Al ser por fecha, se reactiva sola al dia
-- siguiente sin depender de que alguien se acuerde de prenderla -- si el
-- problema sigue, el administrador la vuelve a desactivar esa manana.
-- ---------------------------------------------------------------------------

create table if not exists campana_15min_horario_dia (
    idcampana    int          not null,
    dia_semana   tinyint      not null, -- 1=lunes .. 7=domingo
    activo       char(1)      not null default 'S',
    todo_el_dia  char(1)      not null default 'S',
    hora_desde   time         null,
    hora_hasta   time         null,
    primary key (idcampana, dia_semana)
);

create table if not exists campana_15min_tienda_desactivada_dia (
    idtienda           int           not null,
    fecha              date          not null,
    motivo             varchar(300)  not null default '',
    usuario_autoriza    varchar(80)   not null default '',
    usuario_desactiva   varchar(80)   not null default '',
    fecha_hora          datetime      not null default current_timestamp,
    primary key (idtienda, fecha)
);

-- Siembra el horario descrito por el usuario para la campana que ya existe
-- (la unica sembrada por campana_15min_catalogo.sql): lunes a jueves todo el
-- dia, viernes hasta las 7pm, sabado y domingo hasta las 5pm.
insert into campana_15min_horario_dia (idcampana, dia_semana, activo, todo_el_dia, hora_desde, hora_hasta)
select c.idcampana, d.dia_semana, 'S', d.todo_el_dia, d.hora_desde, d.hora_hasta
from campana_15min_config c
cross join (
    select 1 as dia_semana, 'S' as todo_el_dia, null as hora_desde, null as hora_hasta union all -- lunes
    select 2, 'S', null, null union all -- martes
    select 3, 'S', null, null union all -- miercoles
    select 4, 'S', null, null union all -- jueves
    select 5, 'N', '00:00:00', '19:00:00' union all -- viernes
    select 6, 'N', '00:00:00', '17:00:00' union all -- sabado
    select 7, 'N', '00:00:00', '17:00:00' -- domingo
) d
where not exists (
    select 1 from campana_15min_horario_dia h
    where h.idcampana = c.idcampana and h.dia_semana = d.dia_semana
);

-- Las columnas viejas de horario unico quedan obsoletas; se limpian si existen.
SET @col_existe = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'campana_15min_config' AND column_name = 'dias_semana');
SET @sql_drop = IF(@col_existe > 0, 'ALTER TABLE campana_15min_config DROP COLUMN dias_semana, DROP COLUMN hora_desde, DROP COLUMN hora_hasta', 'SELECT 1');
PREPARE stm_drop FROM @sql_drop;
EXECUTE stm_drop;
DEALLOCATE PREPARE stm_drop;
