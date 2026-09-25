-- ---------------------------------------------------------------------------
-- MAESTRO DE CAMPANAS Y ENVIOS POR TANDAS
--
-- Se corre en el CENTRAL (172.19.0.25). Es idempotente: se puede correr dos
-- veces sin dano.
--
-- POR QUE SE PARTE EN DOS
--
-- Hasta ahora crm.campana era las dos cosas a la vez: la idea -COMBO
-- FUTBOLERO- y la disparada del 24 de septiembre. Revueltas, solo se podia
-- responder "como le fue a la disparada del 24"; no se podia responder "como
-- le va al COMBO FUTBOLERO", que es la pregunta que se hace despues del
-- tercer partido.
--
--   crm.campana        la idea. Nombre, canal, plantilla, filtros. Se reusa.
--   crm.campana_envio  cada disparada. Su tope, su fecha, sus resultados.
--
-- POR QUE UN TOPE POR TANDA
--
-- Para poder decir "hoy 500, manana 200". Un segmento de 14.679 personas no se
-- manda de un solo golpe: se reparte, se mira como responde, y se ajusta.
--
-- LA TRAMPA QUE ESTO VIENE A CERRAR
--
-- La carga de destinatarios ordena por valor DESC. Sin nada mas, los 200 de
-- manana serian un SUBCONJUNTO de los 500 de hoy: siempre los mismos, los de
-- mas valor, machacados todos los dias, y el resto del segmento sin recibir
-- nunca.
--
-- Lo que lo evita es el filtro de dias sin publicidad, que se apoya en
-- pizzaamericana.cliente.ultima_fecha_publicidad. Esa columna la escribia solo
-- la pantalla vieja; la nueva la leia pero nunca la escribia, asi que el
-- filtro estaba ciego a sus propios envios. Eso se corrige en el codigo
-- (CampanaDAO.marcar). Aqui se crea el parametro con el valor por defecto.
--
-- POR QUE LA LLAVE DE DESTINATARIO CAMBIA
--
-- Era (idcampana, idpersona): una persona, un envio, PARA SIEMPRE en esa
-- campana. Eso servia mientras campana y disparada eran lo mismo, pero impide
-- reusar COMBO FUTBOLERO el mes entrante.
--
-- Pasa a ser (idenvio, idpersona): dentro de una tanda nadie recibe dos veces
-- -eso sigue siendo imposible, no algo que haya que cuidar-, y entre tandas
-- quien decide es el filtro de dias. Con 30 dias por defecto, las tandas de
-- esta semana no se pisan y el mes entrante si vuelve a salir.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LA CAMPANA PASA A SER EL MAESTRO
-- ===========================================================================

-- Los valores por defecto con los que arranca cada tanda de esta campana. Se
-- guardan aqui para no volver a armar el filtro cada vez.
SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana'
             AND COLUMN_NAME = 'dias_sin_publicidad');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana ADD COLUMN dias_sin_publicidad INT NOT NULL DEFAULT 30',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana' AND COLUMN_NAME = 'tope');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana ADD COLUMN tope INT NOT NULL DEFAULT 0',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana' AND COLUMN_NAME = 'envios');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana ADD COLUMN envios INT NOT NULL DEFAULT 0',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana'
             AND COLUMN_NAME = 'ultimo_envio_en');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana ADD COLUMN ultimo_envio_en DATETIME NULL',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- El estado del maestro ya no es BORRADOR/ENVIANDO/TERMINADA -eso es de cada
-- tanda- sino si la campana sigue sirviendo o se jubilo.
ALTER TABLE crm.campana
	MODIFY COLUMN estado VARCHAR(12) NOT NULL DEFAULT 'ACTIVA';

UPDATE crm.campana SET estado = 'ACTIVA'
 WHERE estado IN ('BORRADOR', 'ENVIANDO', 'TERMINADA');

-- Dos campanas con el mismo nombre son un error de dedo, y el dia que se
-- comparen desempenos no se sabria cual es cual.
SET @n := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana'
             AND INDEX_NAME = 'uk_campana_nombre');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana ADD UNIQUE KEY uk_campana_nombre (nombre)',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ===========================================================================
-- 2. LA TANDA
-- ===========================================================================

-- Canal, plantilla y asunto se copian aqui y no solo quedan en el maestro a
-- proposito: si el mes entrante se cambia la plantilla, el historial tiene que
-- seguir diciendo con cual se mando en septiembre. Sin eso, comparar tandas
-- compara cosas distintas creyendo que son la misma.
CREATE TABLE IF NOT EXISTS crm.campana_envio (
	idenvio             BIGINT       NOT NULL AUTO_INCREMENT,
	idcampana           BIGINT       NOT NULL,
	consecutivo         INT          NOT NULL DEFAULT 1,
	canal               CHAR(1)      NOT NULL,
	idplantilla         INT              NULL,
	asunto              VARCHAR(250)     NULL,
	cuerpo              MEDIUMTEXT       NULL,
	filtros             VARCHAR(2000)    NULL,
	tope                INT          NOT NULL DEFAULT 0,
	dias_sin_publicidad INT          NOT NULL DEFAULT 0,
	publico             INT          NOT NULL DEFAULT 0,
	enviados            INT          NOT NULL DEFAULT 0,
	fallidos            INT          NOT NULL DEFAULT 0,
	estado              VARCHAR(12)  NOT NULL DEFAULT 'BORRADOR',
	usuario             VARCHAR(50)      NULL,
	creado_en           DATETIME     NOT NULL,
	terminado_en        DATETIME         NULL,
	PRIMARY KEY (idenvio),
	KEY idx_envio_campana (idcampana, consecutivo),
	KEY idx_envio_estado (estado),
	KEY idx_envio_canal (canal, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Cada disparada de una campana';

-- ===========================================================================
-- 3. EL DESTINATARIO AHORA CUELGA DE LA TANDA
-- ===========================================================================

SET @n := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana_destinatario'
             AND COLUMN_NAME = 'idenvio');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana_destinatario ADD COLUMN idenvio BIGINT NOT NULL FIRST',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- La llave pasa de (idcampana, idpersona) a (idenvio, idpersona). Se hace solo
-- si todavia esta la vieja, para poder correr esto dos veces.
SET @n := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana_destinatario'
             AND INDEX_NAME = 'PRIMARY' AND COLUMN_NAME = 'idenvio');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana_destinatario DROP PRIMARY KEY, ADD PRIMARY KEY (idenvio, idpersona)',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- Se conserva el acceso por campana: es lo que responde "a esta persona ya le
-- mande algo de esta campana alguna vez".
SET @n := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana_destinatario'
             AND INDEX_NAME = 'idx_dest_campana');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana_destinatario ADD KEY idx_dest_campana (idcampana, idpersona)',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- La cola de pendientes ahora se pide por tanda, no por campana.
SET @n := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'crm' AND TABLE_NAME = 'campana_destinatario'
             AND INDEX_NAME = 'idx_dest_envio_estado');
SET @s := IF(@n = 0,
	'ALTER TABLE crm.campana_destinatario ADD KEY idx_dest_envio_estado (idenvio, estado)',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

-- ===========================================================================
-- 4. EL PARAMETRO DE DIAS SIN PUBLICIDAD
-- ===========================================================================

-- Treinta dias por defecto. La pantalla lo trae ya puesto y quien envia decide
-- si lo baja o lo quita; ponerlo por defecto es lo que hace que el caso normal
-- -no volverle a escribir al que acaba de recibir- no dependa de que alguien
-- se acuerde de escribirlo.
--
-- Vive en general.parametros y no en el codigo porque es justo lo que mercadeo
-- va a querer mover cuando vea como responde la gente, y eso no puede exigir
-- una version nueva.
INSERT INTO general.parametros (valorparametro, valornumerico, valortexto)
VALUES ('PUBLICIDADDIASMINIMOS', 30,
        'Dias que descansa una persona entre un envio de publicidad y el siguiente')
ON DUPLICATE KEY UPDATE valortexto = valortexto;

SELECT 'Listo. Campanas, tandas y parametro de dias minimos.' AS resultado;
