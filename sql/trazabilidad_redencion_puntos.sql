-- ---------------------------------------------------------------------------
-- Columnas que necesita la trazabilidad de redenciones (commit 6e7d170).
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- SIN ESTAS COLUMNAS LAS REDENCIONES FALLAN, porque el codigo ya inserta
-- usuario y origen. Correr esto ANTES de desplegar el war del central.
--
-- MySQL 8 no admite ADD COLUMN IF NOT EXISTS, asi que primero se revisa.
-- ---------------------------------------------------------------------------

-- PASO 1. Revisar que existe hoy. Si una columna ya aparece aqui, no la agregue.
select table_name, column_name, column_type
from information_schema.columns
where table_schema = 'pizzaamericana'
  and table_name in ('fidelizacion_redencion', 'codigo_redencion_puntos')
  and column_name in ('idtienda', 'idpedidotienda', 'usuario', 'origen')
order by table_name, column_name;

-- PASO 2. Agregar solo las que falten.

-- Quien y desde donde se hizo la redencion. Antes solo se guardaba el correo y
-- los puntos, asi que no habia forma de saber quien la habia procesado.
alter table fidelizacion_redencion
    add column idtienda       int          not null default 0,
    add column idpedidotienda int          not null default 0,
    add column usuario        varchar(60)  not null default '',
    add column origen         varchar(10)  not null default '';

-- origen distingue el canal: 'CC' cuando viene del contact center (se toma de la
-- sesion HTTP y no se puede alterar) y 'POS' cuando viene del punto de venta
-- (llega como parametro, porque el POS no mantiene sesion).

-- Quien pidio el codigo de redencion.
alter table codigo_redencion_puntos
    add column idtienda int         not null default 0,
    add column usuario  varchar(60) not null default '';

-- PASO 3. Indice para el reporte por cajero y por tienda.
alter table fidelizacion_redencion add key usuario_fecha (usuario, idtienda);

-- PASO 4. Verificar que quedaron.
select table_name, column_name, column_type
from information_schema.columns
where table_schema = 'pizzaamericana'
  and table_name in ('fidelizacion_redencion', 'codigo_redencion_puntos')
  and column_name in ('idtienda', 'idpedidotienda', 'usuario', 'origen')
order by table_name, column_name;
