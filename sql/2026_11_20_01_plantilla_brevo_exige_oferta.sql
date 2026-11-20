-- Marca en la tabla de plantillas de Brevo: "esta plantilla entrega un codigo,
-- asi que no puede salir sin oferta".
--
-- POR QUE
--
-- El 19-nov el envio 265 salio con la plantilla 84 -la del cupon del 50%- pero
-- SIN oferta seleccionada. EnvioPublicidadCtrl solo arma los params
-- (nombre/codigo/vence) cuando hay oferta, asi que Brevo pinto los comodines
-- vacios: 500 personas recibieron el correo sin nombre, sin codigo y sin
-- fecha. No se quemo ningun codigo, pero el correo quedo inservible.
--
-- La validacion de entonces solo revisaba cosas CUANDO habia oferta. Al reves
-- no habia nada. Esta columna es lo que permite cerrarlo sin tener que ir a
-- preguntarle a Brevo por el contenido de la plantilla en cada envio, que
-- ademas pondria a Brevo en el camino critico de una validacion.
--
-- 'S' = la plantilla muestra {{ params.codigo }} y exige oferta.
-- 'N' = plantilla informativa, puede salir sola.
--
-- Idempotente.

SET @hay := (SELECT count(*) FROM information_schema.columns
              WHERE table_schema = 'pizzaamericana' AND table_name = 'plantilla_brevo'
                AND column_name = 'exige_oferta');
SET @sql := IF(@hay = 0,
  'ALTER TABLE pizzaamericana.plantilla_brevo ADD COLUMN exige_oferta CHAR(1) NOT NULL DEFAULT ''N''',
  'SELECT ''la columna exige_oferta ya existia''');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- La 84 es la del cupon del 50%: entrega codigo por persona.
UPDATE pizzaamericana.plantilla_brevo SET exige_oferta = 'S' WHERE idplantilla = 84;

SELECT idplantilla, nombre, categoria, exige_oferta FROM pizzaamericana.plantilla_brevo ORDER BY idplantilla;
