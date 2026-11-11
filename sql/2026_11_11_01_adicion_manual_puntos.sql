-- =====================================================================
-- PANTALLA "ADICIONAR PUNTOS A UN CLIENTE" (ajuste manual) Y SU ROL
-- Base de datos: pizzaamericana (la CENTRAL, 172.19.0.25). Idempotente.
--
-- Solo Juan David Botero (jdbotero). Mismo esquema que la pantalla de Auditoria de
-- Fidelizacion: un ROL con un solo miembro, y el servlet vuelve a revisar el rol en
-- cada peticion (SeguridadFilter esta en LOG: esconder la opcion del menu no
-- protege nada). Se da o se quita el acceso desde "Asignar rol a usuario".
--
-- NO se le da la pantalla al Superadministrador: se pidio que solo la use una persona.
-- =====================================================================

-- 1. EL ROL -----------------------------------------------------------
INSERT INTO pizzaamericana.rol (nombre, descripcion, activo)
SELECT 'Ajuste Manual de Puntos',
       'Adiciona puntos a un cliente del plan de fidelizacion con un motivo escrito y le avisa por correo.',
       'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.rol WHERE nombre = 'Ajuste Manual de Puntos');

-- 2. LA PANTALLA (Clientes y CRM, idmodulo 4, junto a la redencion manual) --
INSERT INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
SELECT 'Adicionar Puntos a Cliente', 4, 'AdicionarPuntosCliente.html', 105, 'S'
WHERE NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla WHERE url_html = 'AdicionarPuntosCliente.html');

-- 3. LA PANTALLA PARA EL ROL ------------------------------------------
INSERT INTO pizzaamericana.rol_pantalla (idrol, idpantalla)
SELECT r.idrol, p.idpantalla
FROM pizzaamericana.rol r, pizzaamericana.pantalla p
WHERE r.nombre = 'Ajuste Manual de Puntos'
  AND p.url_html = 'AdicionarPuntosCliente.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.rol_pantalla rp
                  WHERE rp.idrol = r.idrol AND rp.idpantalla = p.idpantalla);

-- 4. EL SERVLET QUE ALIMENTA LA PANTALLA ------------------------------
INSERT INTO pizzaamericana.pantalla_servlet (idpantalla, patron_url)
SELECT p.idpantalla, '/AdicionarPuntosCliente'
FROM pizzaamericana.pantalla p
WHERE p.url_html = 'AdicionarPuntosCliente.html'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.pantalla_servlet ps
                  WHERE ps.idpantalla = p.idpantalla AND ps.patron_url = '/AdicionarPuntosCliente');

-- 5. EL ROL PARA JUAN DAVID BOTERO ------------------------------------
INSERT INTO pizzaamericana.usuario_rol (idusuario, idrol)
SELECT u.id, r.idrol
FROM pizzaamericana.usuario u, pizzaamericana.rol r
WHERE u.nombre = 'jdbotero'
  AND r.nombre = 'Ajuste Manual de Puntos'
  AND NOT EXISTS (SELECT 1 FROM pizzaamericana.usuario_rol ur
                  WHERE ur.idusuario = u.id AND ur.idrol = r.idrol);

-- 6. EL REGISTRO DE CADA AJUSTE ---------------------------------------
-- Quien, cuanto, por que, cuando y como quedo el saldo. Es lo unico que
-- permite explicar un ajuste dentro de seis meses.
CREATE TABLE IF NOT EXISTS pizzaamericana.fidelizacion_ajuste_manual (
  idajuste int NOT NULL AUTO_INCREMENT,
  correo varchar(50) NOT NULL,
  puntos double NOT NULL,
  motivo varchar(200) NOT NULL,
  usuario varchar(40) NOT NULL COMMENT 'quien lo hizo, de la sesion',
  fecha timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_vencimiento date NOT NULL,
  idpedidotienda_ref int NOT NULL COMMENT 'llave negativa con la que quedo en fidelizacion_transaccion (idtienda 0)',
  saldo_antes double NOT NULL,
  saldo_despues double NOT NULL,
  correo_enviado char(1) NOT NULL DEFAULT 'N',
  detalle_correo varchar(200) DEFAULT NULL,
  PRIMARY KEY (idajuste),
  KEY idx_correo (correo),
  KEY idx_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- 7. TOPE POR OPERACION (se puede cambiar sin compilar; si falta se usa 5000) -
INSERT INTO general.parametros (valorparametro, valornumerico, valortexto, valornumericod)
SELECT 'TOPEAJUSTEPUNTOS', 5000, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM general.parametros WHERE valorparametro = 'TOPEAJUSTEPUNTOS');

-- 8. COMPROBACION -----------------------------------------------------
SELECT 'rol' AS objeto, COUNT(*) AS filas FROM pizzaamericana.rol WHERE nombre = 'Ajuste Manual de Puntos'
UNION ALL SELECT 'pantalla', COUNT(*) FROM pizzaamericana.pantalla WHERE url_html = 'AdicionarPuntosCliente.html'
UNION ALL SELECT 'rol_pantalla', COUNT(*) FROM pizzaamericana.rol_pantalla rp JOIN pizzaamericana.pantalla p ON p.idpantalla = rp.idpantalla WHERE p.url_html = 'AdicionarPuntosCliente.html'
UNION ALL SELECT 'pantalla_servlet', COUNT(*) FROM pizzaamericana.pantalla_servlet WHERE patron_url = '/AdicionarPuntosCliente'
UNION ALL SELECT 'miembros del rol (debe ser 1)', COUNT(*) FROM pizzaamericana.usuario_rol ur JOIN pizzaamericana.rol r ON r.idrol = ur.idrol WHERE r.nombre = 'Ajuste Manual de Puntos'
UNION ALL SELECT 'tabla de ajustes', COUNT(*) FROM information_schema.tables WHERE table_schema = 'pizzaamericana' AND table_name = 'fidelizacion_ajuste_manual';
