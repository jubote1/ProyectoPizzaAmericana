-- Consulta de puntos del cliente: que el Administrador PQRS / Contact Center tambien la tenga.
--
-- Hoy la pantalla 50 (ConsultaPuntosCliente.html) la tienen los roles 1 (Operario), 2 (Lider
-- Administrativo) y 6 (Superadministrador). Falta el rol 3 (Administrador PQRS / Contact Center).
-- Los operarios del contact center ya la ven por el rol 1.
--
-- Una sola fila, idempotente. Se corre sobre la base pizzaamericana del central.
-- Para quitarla: DELETE FROM rol_pantalla WHERE idrol = 3 AND idpantalla = 50;

INSERT INTO rol_pantalla (idrol, idpantalla)
SELECT 3, p.idpantalla
  FROM pantalla p
 WHERE p.url_html = 'ConsultaPuntosCliente.html'
   AND NOT EXISTS (SELECT 1 FROM rol_pantalla rp WHERE rp.idrol = 3 AND rp.idpantalla = p.idpantalla);
