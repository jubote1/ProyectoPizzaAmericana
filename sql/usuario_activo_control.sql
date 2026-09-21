-- ---------------------------------------------------------------------------
-- QUE "ACTIVO" SIRVA DE VERDAD
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Va JUNTO con el
-- cambio de capaDAOCC.UsuarioDAO que agrega "and activo = 1" al login.
--
-- CORRER ESTE SCRIPT ANTES DE DESPLEGAR EL WAR. Al reves, 116 de los 127
-- usuarios quedan sin poder entrar.
--
-- POR QUE HAY QUE PRENDER A TODOS PRIMERO
--
-- Hoy 116 de 127 usuarios estan en activo = 0, y solo 11 en 1. Eso NO quiere
-- decir que desactivaron a 116 personas: quiere decir que la columna nunca se
-- uso, porque el login jamas la miro. Los ceros no son una decision de nadie,
-- son el valor por defecto.
--
-- Si el control se prende con los valores de hoy, se cae el negocio:
--
--   BOTAUTO    activo = 0   y tomo 5.154 pedidos en 90 dias
--   bot_prog   activo = 0   37 pedidos
--   jdbotero   activo = 0   administrador, 30 pedidos
--   rzapatar   activo = 0   219 PQRS registradas
--
-- Y esos son solo los que dejan rastro. El central no guarda un registro de
-- entradas, asi que de los otros 112 no hay forma de saber quien trabaja: la
-- mayoria solo consulta pantallas y eso no deja huella en ninguna tabla.
--
-- Por eso se prenden todos: deja el sistema exactamente como esta hoy -nadie
-- gana ni pierde acceso- y a partir de aqui apagar a alguien SI lo saca.
--
-- LO QUE SIGUE, Y ES DECISION DE USTED
--
-- Revisar la lista que imprime este script y apagar a quien ya no trabaja aqui.
-- Ahi esta el valor de verdad del control: hoy un usuario de alguien que se fue
-- sigue entrando, y nadie se entera. Se apaga asi:
--
--   UPDATE pizzaamericana.usuario SET activo = 0 WHERE nombre = 'elusuario';
--
-- ES IDEMPOTENTE: el UPDATE deja un valor fijo y solo prende lo que este
-- apagado. Correrlo dos veces no cambia nada la segunda.
--
-- OJO: si ya se empezo a apagar gente a mano, NO volver a correr este script:
-- les devolveria la entrada. Es el mismo cuidado del acceso al CRM.
-- ---------------------------------------------------------------------------

-- Para saber de donde se partio, por si hay que devolverse.
SELECT activo, COUNT(*) AS usuarios FROM pizzaamericana.usuario GROUP BY activo;

UPDATE pizzaamericana.usuario SET activo = 1 WHERE activo = 0;

-- Como quedo
SELECT activo, COUNT(*) AS usuarios FROM pizzaamericana.usuario GROUP BY activo;

-- La lista para revisar: quien deberia quedar apagado. Se muestra la ultima
-- senal de trabajo que se le conoce, que es lo unico que hay para decidir.
--
-- Va con subconsultas agrupadas y no correlacionadas a proposito: correlacionada
-- recorre el millon de pedidos UNA VEZ POR USUARIO -127 veces- y la consulta se
-- pasa de los dos minutos. Asi recorre una sola vez y responde en segundos.
--
-- Y hay que decirlo: el central no guarda registro de entradas, asi que esto
-- solo ve a quien toma pedidos, toca clientes o registra PQRS. Quien solo
-- consulta pantallas aparece sin ninguna senal aunque trabaje todos los dias.
-- Una fila vacia NO es prueba de que la persona se fue.
SELECT u.nombre, u.nombre_largo, u.administrador, u.acceso_crm,
       p.ultimo_pedido,
       l.ultimo_toque_cliente,
       IFNULL(q.pqrs_registradas, 0) AS pqrs_registradas
  FROM pizzaamericana.usuario u
  LEFT JOIN (SELECT usuariopedido, MAX(fechapedido) AS ultimo_pedido
               FROM pizzaamericana.pedido GROUP BY usuariopedido) p
         ON p.usuariopedido = u.nombre
  LEFT JOIN (SELECT usuario, MAX(fecha) AS ultimo_toque_cliente
               FROM pizzaamericana.cliente_log GROUP BY usuario) l
         ON l.usuario = u.nombre
  LEFT JOIN (SELECT id_usuario_registro, COUNT(*) AS pqrs_registradas
               FROM pizzaamericana.solicitudPQRS GROUP BY id_usuario_registro) q
         ON q.id_usuario_registro = u.id
 ORDER BY p.ultimo_pedido DESC, l.ultimo_toque_cliente DESC, u.nombre_largo;
