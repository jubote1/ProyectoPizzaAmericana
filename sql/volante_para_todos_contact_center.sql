-- ---------------------------------------------------------------------------
-- VOLANTE PARA TODOS en el CONTACT CENTER
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- En el contact center el volante no es un producto sino una EXCEPCION DE
-- PRECIO sobre la pizza base: 9 sobre Pizza XL, 10 sobre Pizza GD, 38 sobre
-- Pizza MD. Se reutilizan esas tres, como en las tiendas se reutilizan los
-- productos 296, 297 y 463.
--
-- QUE CAMBIA
--
-- 1. El nombre, igual que en tienda: se quita el ano.
-- 2. El precio: 64.990 / 55.990 / 45.990.
-- 3. La gaseosa SIN COSTO: incluye_liquido 'S' con idtipoliquido 1 (Gaseosa
--    1.5). Los nueve sabores de ese tipo tienen valor_adicional 0, Coca Cola
--    Zero incluida, que es la del volante.
-- 4. Quedan habilitadas, los siete dias, de 00:00 a 23:59.
--
-- LOS DEDITOS NO VAN AQUI
--
-- A diferencia de la tienda, el contact center no tiene la pregunta del
-- acompanante: el asesor lo agrega a mano. Los productos a 0 ya existen:
--   294 "Deditos de masa con queso Promo(0)"
--   302 "Platano maduro Promo(0)"
-- No hay nada que configurar, pero si hay que decirselo al contact center.
--
-- LAS ESPECIALIDADES YA ESTAN
--
-- controla_especialidades queda en 'N', que es como estaba. Con 'N' el sistema
-- ofrece las 33 especialidades activas; con 'S' las limitaria a la tabla
-- controla_especialidades, que para estas excepciones esta vacia y dejaria la
-- promocion sin ninguna especialidad. El valor adicional sale solo de
-- especialidad_excepcion, que cobra por (especialidad, tamano) y no por
-- promocion: diez especialidades cobran entre 2.500 y 8.000.
-- ---------------------------------------------------------------------------

UPDATE pizzaamericana.excepcion_precio
   SET descripcion              = 'XL Volante Para Todos',
       precio                   = 64990,
       incluye_liquido          = 'S',
       idtipoliquido            = 1,
       controla_especialidades  = 'N',
       habilitado               = 'S',
       hora_inicial             = '00:00',
       hora_final               = '23:59',
       lunes='S', martes='S', miercoles='S', jueves='S', viernes='S', sabado='S', domingo='S'
 WHERE idexcepcion = 9 AND descripcion LIKE '%Volante%';

UPDATE pizzaamericana.excepcion_precio
   SET descripcion              = 'GD Volante Para Todos',
       precio                   = 55990,
       incluye_liquido          = 'S',
       idtipoliquido            = 1,
       controla_especialidades  = 'N',
       habilitado               = 'S',
       hora_inicial             = '00:00',
       hora_final               = '23:59',
       lunes='S', martes='S', miercoles='S', jueves='S', viernes='S', sabado='S', domingo='S'
 WHERE idexcepcion = 10 AND descripcion LIKE '%Volante%';

UPDATE pizzaamericana.excepcion_precio
   SET descripcion              = 'MD Volante Para Todos',
       precio                   = 45990,
       incluye_liquido          = 'S',
       idtipoliquido            = 1,
       controla_especialidades  = 'N',
       habilitado               = 'S',
       hora_inicial             = '00:00',
       hora_final               = '23:59',
       lunes='S', martes='S', miercoles='S', jueves='S', viernes='S', sabado='S', domingo='S'
 WHERE idexcepcion = 38 AND descripcion LIKE '%Volante%';

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT e.idexcepcion, e.descripcion, p.nombre AS sobre, e.precio,
       e.incluye_liquido AS gaseosa_incluida, t.nombre AS tipo_gaseosa,
       e.controla_especialidades AS limita_especialidades, e.habilitado,
       CONCAT(e.hora_inicial,'-',e.hora_final) AS horario,
       CONCAT(e.lunes,e.martes,e.miercoles,e.jueves,e.viernes,e.sabado,e.domingo) AS dias
  FROM pizzaamericana.excepcion_precio e
  LEFT JOIN pizzaamericana.producto p ON p.idproducto = e.idproducto
  LEFT JOIN pizzaamericana.tipo_liquido t ON t.idtipo_liquido = e.idtipoliquido
 WHERE e.idexcepcion IN (9,10,38) ORDER BY e.idexcepcion;
