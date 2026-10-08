-- ---------------------------------------------------------------------------
-- El registro de corridas del reporte diario de promociones
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
-- Requiere promociones_reporte_catalogo.sql.
--
-- ===========================================================================
-- POR QUE NO ALCANZA CON MIRAR promocion_dia
-- ===========================================================================
--
-- La idea es que el proceso, cada madrugada, revise si quedaron dias atras sin
-- migrar y los recupere solo. Lo natural seria preguntar "que fechas no tienen
-- filas en promocion_dia", pero eso NO funciona:
--
--     un dia sin filas puede ser un dia que no se migro,
--     o un dia en que de verdad no se vendio ninguna promocion.
--
-- Los dos se ven igual. Con esa regla, un martes festivo sin ventas se
-- intentaria recuperar todas las noches para siempre, y ademas nunca se sabria
-- si un dia quedo a medias.
--
-- Por eso el proceso deja constancia explicita de cada dia que trabajo, con el
-- resultado. Lo que decide si hay que recuperar un dia es ESTA tabla, no la
-- ausencia de datos en la otra.
--
-- ===========================================================================
-- LOS DOS ESTADOS, Y POR QUE IMPORTA LA DIFERENCIA
-- ===========================================================================
--
--   COMPLETA     las once tiendas respondieron. El dia esta cerrado y no se
--                vuelve a tocar.
--   INCOMPLETA   alguna tienda no respondio. El dia NO se guarda en
--                promocion_dia -un dia a medias se convierte manana en la
--                referencia contra la que se compara, y nadie se acordaria de
--                que faltaban dos tiendas- y se vuelve a intentar la noche
--                siguiente.
--
-- En INCOMPLETA queda escrito cuales tiendas faltaron, para que al mirar la
-- tabla dentro de un mes se entienda por que ese dia no esta.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS datamart.promocion_dia_corrida (
  fecha           DATE         NOT NULL,
  estado          VARCHAR(12)  NOT NULL COMMENT 'COMPLETA o INCOMPLETA',
  tiendas_ok      INT          NOT NULL DEFAULT 0,
  tiendas_falla   INT          NOT NULL DEFAULT 0,
  detalle_falla   VARCHAR(400) NOT NULL DEFAULT '' COMMENT 'Que tiendas no respondieron',
  filas           INT          NOT NULL DEFAULT 0,
  recuperado      CHAR(1)      NOT NULL DEFAULT 'N' COMMENT 'S cuando se migro despues, no esa noche',
  grabado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (fecha),
  KEY idx_promocion_corrida_estado (estado, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Que dias se migraron, con que resultado. Decide que hay que recuperar';

-- ===========================================================================
-- HASTA DONDE MIRAR HACIA ATRAS
--
-- El proceso revisa esta cantidad de dias hacia atras buscando dias sin migrar
-- y los recupera solo. Treinta dias cubren unas vacaciones completas: la idea
-- es que nadie tenga que estar pendiente ni acordarse de correr el reproceso.
--
-- No se pone mas alto porque cada dia que falta son once consultas a once
-- tiendas, y un barrido de meses en la madrugada compite con el resto de
-- procesos. Treinta dias en el peor caso son 330 consultas, que es asumible.
--
-- Si aun asi quedara un dia por fuera de la ventana, el correo lo dice con
-- nombre y fecha en vez de dejarlo pasar en silencio, y ahi si se recupera a
-- mano con ReportePromocionesDiaReproceso y FECHAREPROCESO.
-- ===========================================================================

INSERT INTO general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'PROMOCIONESDIASATRAS', 30,
       'Dias hacia atras que revisa el reporte de promociones buscando dias sin migrar', 0
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM general.parametros
                    WHERE valorparametro = 'PROMOCIONESDIASATRAS');

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT TABLE_NAME, TABLE_COMMENT
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'datamart' AND TABLE_NAME = 'promocion_dia_corrida';

SELECT valorparametro, valornumerico AS dias FROM general.parametros
 WHERE valorparametro = 'PROMOCIONESDIASATRAS';
