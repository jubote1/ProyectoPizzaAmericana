-- ---------------------------------------------------------------------------
-- QUIEN PUEDE ENTRAR AL CRM
--
-- Se corre en el CENTRAL (172.19.0.25), base general.
--
-- POR QUE UNA MARCA POR USUARIO Y NO EL PERFIL DE SIEMPRE
--
-- El CRM muestra datos personales de 450 mil personas: nombre, celular, correo,
-- direcciones, que compra y cuanto gasta. Eso no es lo mismo que ver la lista
-- de pedidos del dia, y no deberia abrirse con el mismo criterio.
--
-- Amarrarlo a empleado.administrador = 'S' tal cual dejaria entrar a 35
-- personas, y entre ellas hay Auxiliares de Administrador de punto de venta y
-- hasta un Pizzero que quedo marcado como administrador. Por eso la marca es
-- SUYA, independiente: se arranca con los administradores porque es lo que se
-- pidio, y desde ahi se quita a quien no deba entrar sin tener que quitarle el
-- perfil de administrador, que le sirve para otras cosas.
--
-- ARRANCA CERRADO
--
-- La columna nace en 'N'. Quien no este marcado NO entra, asi que un empleado
-- nuevo no queda con acceso por descuido. Es al reves de lo que estaba hoy,
-- donde cualquiera con sesion podia abrir la pantalla.
--
-- DONDE SE VALIDA DE VERDAD
--
-- En los servicios -BuscarPersonaCRM, ConsultarPersona360, ConsultarResumenCRM-,
-- no solo en el menu. Esconder la opcion no protege nada: la URL se puede
-- escribir a mano y los servicios responden JSON sin pasar por la pantalla.
--
-- ES IDEMPOTENTE, Y ESO AQUI TIENE UN MATIZ
--
-- El ALTER va con la guarda de siempre. El UPDATE es una SIEMBRA INICIAL y se
-- salta solo si ya hay alguien marcado: si se volviera a correr despues de que
-- usted le quite el acceso a alguien, se lo devolveria. Un permiso que revive
-- solo es la clase de error que nadie nota hasta que es tarde.
-- ---------------------------------------------------------------------------

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='general' AND TABLE_NAME='empleado'
                   AND COLUMN_NAME='acceso_crm');
SET @sql := IF(@existe = 0,
  'ALTER TABLE general.empleado
     ADD COLUMN acceso_crm CHAR(1) NOT NULL DEFAULT ''N''
       COMMENT ''S si el usuario puede entrar al area de CRM''',
  'SELECT ''empleado ya tiene acceso_crm'' AS paso1');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- Siembra inicial: los administradores activos. Si ya hay alguien marcado es
-- que esto ya se sembro y el acceso lo esta manejando una persona, asi que no
-- se toca nada.
SET @yaSembrado := (SELECT COUNT(*) FROM general.empleado WHERE acceso_crm = 'S');

UPDATE general.empleado
   SET acceso_crm = 'S'
 WHERE @yaSembrado = 0
   AND administrador = 'S'
   AND activo = '1';

-- Como quedo
SELECT acceso_crm, COUNT(*) AS empleados, SUM(activo='1') AS activos
  FROM general.empleado GROUP BY acceso_crm;
