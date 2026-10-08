-- ---------------------------------------------------------------------------
-- Parametro IDTIPOEMPLEADOSAC (base general del CENTRAL) y asignacion masiva de
-- clave rapida a Servicio al Cliente que no la tenga.
--
-- QUE RESUELVE
--
-- Con el usuario CAJA desaparecido, cada persona de Servicio al Cliente (SAC,
-- tipo de empleado 4) necesita usuario propio en el POS. Hasta ahora la clave
-- rapida (los ultimos 6 digitos de la cedula) se les venia poniendo a mano.
-- El POS (ProyectoTiendaAmericanaCliente) ya lo hace automatico para
-- Domiciliarios al crearlos; con este parametro hace lo mismo para SAC, sin
-- quemar el id 4 en el codigo: si el tipo de empleado que hace de SAC cambia,
-- se actualiza aqui, no en el POS.
--
-- Es aditivo e idempotente: solo agrega la fila si no existe.
-- ---------------------------------------------------------------------------

USE general;

INSERT INTO parametros (valorparametro, valornumerico, valortexto)
SELECT 'IDTIPOEMPLEADOSAC', 4, ''
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'IDTIPOEMPLEADOSAC');

-- ---------------------------------------------------------------------------
-- Asignacion de una sola vez: empleados ACTIVOS de tipo SAC que hoy no tienen
-- clave rapida, quedan con los ultimos 6 digitos de su cedula (columna
-- "nombre", que es el usuario/cedula con el que se creo el empleado).
--
-- No se toca a quien ya tiene clave (para no pisar una clave que alguien puso
-- a mano), ni a quien tiene una cedula de menos de 6 digitos (revisar esos a
-- mano; RIGHT() les daria una clave mas corta que a los demas).
-- ---------------------------------------------------------------------------

-- Antes de correrlo: cuantos quedarian afectados.
SELECT COUNT(*) AS a_asignar
  FROM empleado e
  JOIN parametros p ON p.valorparametro = 'IDTIPOEMPLEADOSAC'
 WHERE e.activo = 1
   AND e.idtipoempleado = p.valornumerico
   AND (e.claverapida IS NULL OR e.claverapida = '')
   AND CHAR_LENGTH(e.nombre) >= 6;

-- Los que quedan por fuera por tener una cedula de menos de 6 digitos (revisar a mano).
SELECT e.id, e.nombre, e.nombre_largo
  FROM empleado e
  JOIN parametros p ON p.valorparametro = 'IDTIPOEMPLEADOSAC'
 WHERE e.activo = 1
   AND e.idtipoempleado = p.valornumerico
   AND (e.claverapida IS NULL OR e.claverapida = '')
   AND CHAR_LENGTH(e.nombre) < 6;

UPDATE empleado e
  JOIN parametros p ON p.valorparametro = 'IDTIPOEMPLEADOSAC'
   SET e.claverapida = RIGHT(e.nombre, 6)
 WHERE e.activo = 1
   AND e.idtipoempleado = p.valornumerico
   AND (e.claverapida IS NULL OR e.claverapida = '')
   AND CHAR_LENGTH(e.nombre) >= 6;

-- Despues de correrlo: si dos empleados (de cualquier tipo, no solo SAC) quedaron con
-- la MISMA clave rapida, el que entre con esa clave cae en el primero que encuentre.
-- Esto ya podia pasar antes (nadie lo validaba al asignar a mano); esta consulta lo
-- deja a la vista para corregirlo si aparece.
SELECT claverapida, COUNT(*) AS cuantos, GROUP_CONCAT(id) AS ids
  FROM empleado
 WHERE claverapida IS NOT NULL AND claverapida <> '' AND activo = 1
 GROUP BY claverapida
HAVING COUNT(*) > 1;
