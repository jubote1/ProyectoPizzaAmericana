-- ---------------------------------------------------------------------------
-- QUIEN PUEDE ENTRAR AL CRM
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana.
--
-- LA TABLA CORRECTA ES pizzaamericana.usuario
--
-- La primera version de este script puso la columna en general.empleado y
-- estaba MAL: el central no autentica contra esa tabla. capaDAOCC.UsuarioDAO
-- consulta "FROM usuario" con obtenerConexionBDPrincipal(), o sea
-- pizzaamericana.usuario. general.empleado es el maestro de personal, que usa
-- el POS; son cosas distintas y ni siquiera tienen los mismos registros: la
-- primera tiene 217 empleados y esta tiene 127 usuarios.
--
-- Este script ademas deshace esa equivocacion.
--
-- POR QUE UNA MARCA PROPIA Y NO EL CAMPO administrador
--
-- El CRM muestra datos personales de 450 mil personas: nombre, celular, correo,
-- direcciones, que compran y cuanto gastan. Eso no es lo mismo que ver la lista
-- de pedidos del dia.
--
-- Teniendo columna propia se le puede quitar el CRM a alguien sin quitarle el
-- perfil de administrador, que le sirve para otras pantallas. Se siembra con
-- los administradores porque fue lo que se pidio, pero de ahi en adelante son
-- dos cosas independientes.
--
-- POR QUE LA SIEMBRA NO FILTRA POR activo
--
-- Porque esa columna no se usa para entrar. El login es
-- "select * from usuario where nombre = ? and password = ?", sin mirar activo
-- (UsuarioDAO linea 36). Hoy 8 de los 11 administradores estan en activo = 0 y
-- siguen entrando, incluido el usuario jdbotero.
--
-- Filtrar por activo aqui dejaria sin CRM justo a los que lo necesitan. Queda
-- anotado aparte que una columna "activo" que no impide entrar es un problema
-- de seguridad por su cuenta, pero arreglarlo no es asunto de este script.
--
-- ARRANCA CERRADO: la columna nace en 'N', asi que un usuario nuevo no queda
-- con acceso por descuido.
--
-- DONDE SE VALIDA DE VERDAD: en los servicios -BuscarPersonaCRM,
-- ConsultarPersona360, ConsultarResumenCRM-, no solo en el menu. Esconder la
-- opcion no protege nada: la URL se escribe a mano.
--
-- ES IDEMPOTENTE, con un matiz: la siembra se salta sola si ya hay alguien
-- marcado. Si se volviera a correr despues de que usted revoque un acceso, se
-- lo devolveria, y un permiso que revive es la clase de error que nadie nota
-- hasta que es tarde.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. DESHACER LA COLUMNA QUE QUEDO EN LA TABLA EQUIVOCADA
--
-- Se creo hoy, no la lee nadie y no guarda nada que no se pueda volver a
-- calcular. Dejarla seria peor: una segunda marca de acceso, en otra tabla,
-- que nadie consulta.
-- ===========================================================================

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='general' AND TABLE_NAME='empleado'
                   AND COLUMN_NAME='acceso_crm');
SET @sql := IF(@existe = 1,
  'ALTER TABLE general.empleado DROP COLUMN acceso_crm',
  'SELECT ''general.empleado ya no tiene la columna'' AS paso1');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ===========================================================================
-- 2. LA COLUMNA, DONDE SI VA
-- ===========================================================================

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='pizzaamericana' AND TABLE_NAME='usuario'
                   AND COLUMN_NAME='acceso_crm');
SET @sql := IF(@existe = 0,
  'ALTER TABLE pizzaamericana.usuario
     ADD COLUMN acceso_crm CHAR(1) NOT NULL DEFAULT ''N''
       COMMENT ''S si el usuario puede entrar al area de CRM''',
  'SELECT ''usuario ya tiene acceso_crm'' AS paso2');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ===========================================================================
-- 3. LA SIEMBRA
-- ===========================================================================

SET @yaSembrado := (SELECT COUNT(*) FROM pizzaamericana.usuario WHERE acceso_crm = 'S');

UPDATE pizzaamericana.usuario
   SET acceso_crm = 'S'
 WHERE @yaSembrado = 0
   AND administrador = 'S';

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT acceso_crm, COUNT(*) AS usuarios FROM pizzaamericana.usuario GROUP BY acceso_crm;

SELECT nombre, nombre_largo, administrador, activo
  FROM pizzaamericana.usuario WHERE acceso_crm = 'S' ORDER BY nombre_largo;
