-- ---------------------------------------------------------------------------
-- PREGUNTAS A LOS EMPLEADOS EN LA BIOMETRIA (banco de preguntas + estadistica)
--
-- Se corre en el CENTRAL (172.19.0.25), base GENERAL. Es idempotente.
--
-- QUE ES
--
-- Cada vez que un empleado da INGRESO en el huellero del POS se le hace una
-- pregunta de opcion multiple del banco, y se guarda que respondio. La pantalla
-- ResultadoPreguntasEmpleado.html del central muestra, por empleado activo y por
-- rango de fechas, cuantas contesto y con que porcentaje de acierto.
--
-- El banco se administra desde el POS (Administrador > Preguntas a empleados),
-- igual que las encuestas de mercadeo; se escribe directo en estas tablas, las
-- mismas que ya lee el huellero (EmpleadoBiometriaDAO.obtenerPreguntaAleatoria).
--
-- ATENCION: LAS TABLAS YA EXISTEN EN PRODUCCION
--
-- El huellero ya las lee desde hace tiempo ("Desafio de Cultura Empresarial"),
-- pero ningun script del repositorio las crea, asi que sus columnas se dedujeron
-- de las consultas del POS. Los CREATE TABLE de abajo son IF NOT EXISTS: si la
-- tabla ya esta, no la tocan. LO UNICO QUE SI SE MODIFICA es respuesta_empleado,
-- a la que se le agregan tres columnas si faltan:
--
--   fecha_respuesta  cuando contesto (sin esto no hay rango de fechas)
--   correcta         si acerto, guardado AL MOMENTO de responder: si mas tarde
--                    se edita cual era la opcion correcta, el historico no cambia
--   idtienda         desde que tienda respondio
--
-- ANTES DE CORRERLO, mirar como estan hoy (PASO 0). Si alguna tabla tiene
-- columnas que no aparecen aqui, o nombres distintos, avisar antes de seguir:
-- el codigo del POS y del central asume estos nombres.
-- ---------------------------------------------------------------------------

USE general;

-- PASO 0. Como estan hoy (solo lectura)
SHOW CREATE TABLE pregunta_empleado;
SHOW CREATE TABLE pregunta_empleado_tipo;
SHOW CREATE TABLE opcion_respuesta;
SHOW CREATE TABLE opcion_respuesta_pregunta;
SHOW CREATE TABLE respuesta_empleado;

-- PASO 1. Tablas (no hacen nada si ya existen)
CREATE TABLE IF NOT EXISTS pregunta_empleado (
  id INT NOT NULL AUTO_INCREMENT,
  descripcion VARCHAR(500) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_final DATE NOT NULL,
  activo TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Banco de preguntas que el huellero le hace al empleado al dar ingreso.';

CREATE TABLE IF NOT EXISTS pregunta_empleado_tipo (
  idpregunta INT NOT NULL,
  idtipoempleado INT NOT NULL,
  PRIMARY KEY (idpregunta, idtipoempleado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='A que tipos de empleado (cargos) se les hace cada pregunta.';

CREATE TABLE IF NOT EXISTS opcion_respuesta (
  id INT NOT NULL AUTO_INCREMENT,
  contenido VARCHAR(300) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Catalogo de textos de opcion de respuesta; se reutilizan entre preguntas.';

CREATE TABLE IF NOT EXISTS opcion_respuesta_pregunta (
  idpregunta INT NOT NULL,
  idopcion INT NOT NULL,
  correcta TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (idpregunta, idopcion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Las opciones de cada pregunta y cual es la correcta.';

CREATE TABLE IF NOT EXISTS respuesta_empleado (
  id INT NOT NULL AUTO_INCREMENT,
  idempleado INT NOT NULL,
  idopcion INT NOT NULL,
  idpregunta INT NOT NULL,
  fecha_respuesta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  correcta TINYINT DEFAULT NULL,
  idtienda INT DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='Lo que respondio cada empleado al ingresar.';

-- PASO 2. Columnas que faltan en respuesta_empleado (si la tabla ya existia sin ellas)
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE respuesta_empleado ADD COLUMN fecha_respuesta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT ''fecha_respuesta ya existe'' AS nota')
  FROM information_schema.columns
  WHERE table_schema = 'general' AND table_name = 'respuesta_empleado' AND column_name = 'fecha_respuesta');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE respuesta_empleado ADD COLUMN correcta TINYINT DEFAULT NULL',
  'SELECT ''correcta ya existe'' AS nota')
  FROM information_schema.columns
  WHERE table_schema = 'general' AND table_name = 'respuesta_empleado' AND column_name = 'correcta');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE respuesta_empleado ADD COLUMN idtienda INT DEFAULT NULL',
  'SELECT ''idtienda ya existe'' AS nota')
  FROM information_schema.columns
  WHERE table_schema = 'general' AND table_name = 'respuesta_empleado' AND column_name = 'idtienda');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- PASO 3. Indice para la pantalla de resultados (por empleado y rango de fechas)
SET @sql = (SELECT IF(COUNT(*) = 0,
  'CREATE INDEX idx_respuesta_empleado_fecha ON respuesta_empleado (idempleado, fecha_respuesta)',
  'SELECT ''indice ya existe'' AS nota')
  FROM information_schema.statistics
  WHERE table_schema = 'general' AND table_name = 'respuesta_empleado' AND index_name = 'idx_respuesta_empleado_fecha');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- PASO 4. La pantalla nueva en el catalogo de seguridad (modulo 3: monitoreo y analisis).
--         Solo agrega la fila; los permisos se asignan aparte, como con las demas.
INSERT IGNORE INTO pizzaamericana.pantalla (nombre, idmodulo, url_html, orden, activo)
VALUES ('Resultado Preguntas a Empleados', 3, 'ResultadoPreguntasEmpleado.html', 111, 'S');

-- PASO 5. Verificacion
SHOW COLUMNS FROM respuesta_empleado;
SELECT COUNT(*) AS respuestas_guardadas FROM respuesta_empleado;
SELECT idpantalla, nombre, url_html FROM pizzaamericana.pantalla WHERE url_html = 'ResultadoPreguntasEmpleado.html';
