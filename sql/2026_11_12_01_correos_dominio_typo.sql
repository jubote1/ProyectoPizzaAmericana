-- ============================================================================
-- 2026_11_12_01_correos_dominio_typo.sql
--
-- Arregla el DOMINIO de los correos mal digitados: gamil.com -> gmail.com,
-- hormail.com -> hotmail.com, gmail.con -> gmail.com, y 385 variantes mas.
--
-- POR QUE SE PUEDE HACER SIN ADIVINAR
-- Solo se cambia el dominio, que es un nombre conocido y finito. La parte de
-- antes del arroba NO SE TOCA. No se esta inventando una direccion: se esta
-- corrigiendo el nombre del proveedor, que el cliente escribio mal.
--
-- LO QUE SE DEJO POR FUERA A PROPOSITO
--  - Dominios que SI existen aunque se parezcan: ymail.com (es de Yahoo),
--    yahoo.ca, hotmail.it, hotmail.ca, hotmail.fr, mac.com, live.cl,
--    outlook.cl, email.com, yopmail.com, ms.com.
--  - Ambiguos, que podrian ser dos cosas: hmail.com (gmail? hotmail?),
--    cmail.com, tmail.com, fmail.com, lider.com, vive.com.
--  - Institucionales .edu.co / .gov.co: son reales.
--  - rappi.com: es el marcador del integrador, no es de un cliente.
--
-- Es idempotente: la segunda corrida no encuentra nada que cambiar.
-- Es reversible: crm.respaldo_email_typo_20261112 guarda el antes de cada fila.
--
-- OJO CON EL ORDEN
-- Hay que correrlo COMPLETO de una vez. La tabla de trabajo se arma leyendo
-- los correos malos; si se corre a medias, el respaldo queda incompleto.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Catalogo de correcciones. Queda permanente para que el proceso de
--    mantenimiento lo pueda usar despues sobre lo que entre nuevo.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm.dominio_correccion (
  malo             VARCHAR(60)  NOT NULL,
  bueno            VARCHAR(60)  NOT NULL,
  veces_detectadas INT          NOT NULL DEFAULT 0,
  creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (malo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

INSERT INTO crm.dominio_correccion (malo, bueno, veces_detectadas) VALUES
  ('gmail.con','gmail.com',578),
  ('gamil.com','gmail.com',296),
  ('hotmail.con','hotmail.com',242),
  ('gmial.com','gmail.com',200),
  ('hormail.com','hotmail.com',97),
  ('gmai.com','gmail.com',81),
  ('gmail.co','gmail.com',77),
  ('hotmai.com','hotmail.com',59),
  ('hotmial.com','hotmail.com',51),
  ('gmil.com','gmail.com',50),
  ('hotamil.com','hotmail.com',42),
  ('gnail.com','gmail.com',36),
  ('homail.com','hotmail.com',34),
  ('gamail.com','gmail.com',30),
  ('gimail.com','gmail.com',30),
  ('hotmal.com','hotmail.com',27),
  ('gmal.com','gmail.com',25),
  ('hotmail.co','hotmail.com',25),
  ('gmaill.com','gmail.com',22),
  ('hotmil.com','hotmail.com',18),
  ('yahoot.com','yahoo.com',18),
  ('gmail.cpm','gmail.com',16),
  ('hitmail.com','hotmail.com',15),
  ('gmail.col','gmail.com',13),
  ('gmail.vom','gmail.com',13),
  ('oulook.com','outlook.com',12),
  ('outlook.con','outlook.com',12),
  ('gmail.comm','gmail.com',11),
  ('gmaul.com','gmail.com',11),
  ('htmail.com','hotmail.com',11),
  ('autlook.com','outlook.com',10),
  ('gail.com','gmail.com',10),
  ('gmaim.com','gmail.com',10),
  ('hotmaill.com','hotmail.com',10),
  ('icloud.con','icloud.com',10),
  ('gmaiil.com','gmail.com',9),
  ('gmail.om','gmail.com',9),
  ('gmsil.com','gmail.com',9),
  ('gotmail.com','hotmail.com',9),
  ('hotamail.com','hotmail.com',9),
  ('gmail.cm','gmail.com',8),
  ('gmailo.com','gmail.com',8),
  ('hotmail.cpm','hotmail.com',8),
  ('hotmail.om','hotmail.com',8),
  ('hotmail.vom','hotmail.com',8),
  ('hotnail.com','hotmail.com',8),
  ('gma.com','gmail.com',7),
  ('hotmailo.com','hotmail.com',7),
  ('iclud.com','icloud.com',7),
  ('yahoo.con','yahoo.com',7),
  ('ghotmail.com','hotmail.com',6),
  ('glail.com','gmail.com',6),
  ('gmail.comn','gmail.com',6),
  ('gnmail.com','gmail.com',6),
  ('hotmsil.com','hotmail.com',6),
  ('hoymail.com','hotmail.com',6),
  ('icluod.com','icloud.com',6),
  ('otlook.com','outlook.com',6),
  ('outlok.com','outlook.com',6),
  ('ail.com','gmail.com',5),
  ('gmail.clm','gmail.com',5),
  ('gmail.como','gmail.com',5),
  ('gmaiol.com','gmail.com',5),
  ('gmaol.com','gmail.com',5),
  ('hotmail.col','hotmail.com',5),
  ('hotmail.comm','hotmail.com',5),
  ('hotmqil.com','hotmail.com',5),
  ('hptmail.com','hotmail.com',5),
  ('htomail.com','hotmail.com',5),
  ('otlok.com','outlook.com',5),
  ('gemail.com','gmail.com',4),
  ('gmaio.com','gmail.com',4),
  ('gmeil.com','gmail.com',4),
  ('gmmail.com','gmail.com',4),
  ('gmqil.com','gmail.com',4),
  ('hormail.es','hotmail.es',4),
  ('hotmaim.com','hotmail.com',4),
  ('hotnmail.com','hotmail.com',4),
  ('ouclook.com','outlook.com',4),
  ('4gmail.com','gmail.com',3),
  ('ganil.com','gmail.com',3),
  ('gemail.con','gmail.com',3),
  ('gmai.lcom','gmail.com',3),
  ('gmail.cim','gmail.com',3),
  ('gmail.cok','gmail.com',3),
  ('gmail.coma','gmail.com',3),
  ('gmail.comc','gmail.com',3),
  ('gmail.comj','gmail.com',3),
  ('gmail.coml','gmail.com',3),
  ('gmail.conm','gmail.com',3),
  ('gmail.coom','gmail.com',3),
  ('gmail.copm','gmail.com',3),
  ('gmail.fom','gmail.com',3),
  ('hoptmail.com','hotmail.com',3),
  ('hortmail.com','hotmail.com',3),
  ('hotail.com','hotmail.com',3),
  ('hotmail.ccom','hotmail.com',3),
  ('hotmailm.com','hotmail.com',3),
  ('hotmaio.com','hotmail.com',3),
  ('hotmaol.com','hotmail.com',3),
  ('hotmasil.com','hotmail.com',3),
  ('hotmaul.com','hotmail.com',3),
  ('hotrmail.com','hotmail.com',3),
  ('iclow.com','icloud.com',3),
  ('live.co','live.com',3),
  ('mgail.com','gmail.com',3),
  ('otlook.es','outlook.es',3),
  ('oulook.es','outlook.es',3),
  ('outllok.com','outlook.com',3),
  ('outloock.com','outlook.com',3),
  ('uotlook.com','outlook.com',3),
  ('yahoo.co','yahoo.com',3),
  ('yhoo.es','yahoo.es',3),
  ('12gmail.com','gmail.com',2),
  ('autlook.es','outlook.es',2),
  ('gfmail.com','gmail.com',2),
  ('gimal.com','gmail.com',2),
  ('gmaikl.com','gmail.com',2),
  ('gmail.ccom','gmail.com',2),
  ('gmail.comb','gmail.com',2),
  ('gmail.come','gmail.com',2),
  ('gmail.comk','gmail.com',2),
  ('gmail.coms','gmail.com',2),
  ('gmail.gom','gmail.com',2),
  ('gmail.lcom','gmail.com',2),
  ('gmail.ocm','gmail.com',2),
  ('gmailk.com','gmail.com',2),
  ('gmali.com','gmail.com',2),
  ('gmalil.com','gmail.com',2),
  ('gmamil.com','gmail.com',2),
  ('gmaqil.com','gmail.com',2),
  ('gmasil.com','gmail.com',2),
  ('gmauil.com','gmail.com',2),
  ('gnail.con','gmail.com',2),
  ('gomail.com','gmail.com',2),
  ('gtmail.com','gmail.com',2),
  ('haotmail.com','hotmail.com',2),
  ('hatmail.com','hotmail.com',2),
  ('hhotmail.com','hotmail.com',2),
  ('hitmail.es','hotmail.es',2),
  ('homtail.com','hotmail.com',2),
  ('hootmail.com','hotmail.com',2),
  ('hormail.con','hotmail.com',2),
  ('hotma.com','hotmail.com',2),
  ('hotmaail.com','hotmail.com',2),
  ('hotmaiil.com','hotmail.com',2),
  ('hotmail.cm','hotmail.com',2),
  ('hotmail.coml','hotmail.com',2),
  ('hotmail.comn','hotmail.com',2),
  ('hotmail.coms','hotmail.com',2),
  ('hotmail.coom','hotmail.com',2),
  ('hotmailc.com','hotmail.com',2),
  ('hottmail.com','hotmail.com',2),
  ('iclout.com','icloud.com',2),
  ('icolud.com','icloud.com',2),
  ('ivloud.com','icloud.com',2),
  ('jotmail.com','hotmail.com',2),
  ('lgmail.com','gmail.com',2),
  ('live.con','live.com',2),
  ('m.com','msn.com',2),
  ('msn.con','msn.com',2),
  ('oitlook.com','outlook.com',2),
  ('otlokk.com','outlook.com',2),
  ('otmail.com','hotmail.com',2),
  ('oukloo.com','outlook.com',2),
  ('outlok.es','outlook.es',2),
  ('outloo.com','outlook.com',2),
  ('outloo.es','outlook.es',2),
  ('outloook.com','outlook.com',2),
  ('outlwk.com','outlook.com',2),
  ('outook.es','outlook.es',2),
  ('putlook.es','outlook.es',2),
  ('utlook.com','outlook.com',2),
  ('yagoo.com','yahoo.com',2),
  ('yaoo.com','yahoo.com',2),
  ('yaoot.es','yahoo.es',2),
  ('yhaoo.com','yahoo.com',2),
  ('01gmail.com','gmail.com',1),
  ('0utlook.com','outlook.com',1),
  ('1.gmail.com','gmail.com',1),
  ('17gmail.com','gmail.com',1),
  ('20gmail.com','gmail.com',1),
  ('23gmail.com','gmail.com',1),
  ('4hotmail.com','hotmail.com',1),
  ('4yahoo.com','yahoo.com',1),
  ('51gmail.com','gmail.com',1),
  ('57gmail.com','gmail.com',1),
  ('5hotmail.com','hotmail.com',1),
  ('97hotmail.com','hotmail.com',1),
  ('agmail.com','gmail.com',1),
  ('aiclud.com','icloud.com',1),
  ('autlok.com','outlook.com',1),
  ('autloock.com','outlook.com',1),
  ('dyaboo.com','yahoo.com',1),
  ('fgmail.com','gmail.com',1),
  ('g.mail.com','gmail.com',1),
  ('galil.com','gmail.com',1),
  ('gemil.com','gmail.com',1),
  ('ggmail.com','gmail.com',1),
  ('ghamil.com','gmail.com',1),
  ('ghmail.com','gmail.com',1),
  ('gial.com','gmail.com',1),
  ('gimael.com','gmail.com',1),
  ('gimai.com','gmail.com',1),
  ('gimaiil.com','gmail.com',1),
  ('gimail.co','gmail.com',1),
  ('gitmail.es','hotmail.es',1),
  ('glmail.com','gmail.com',1),
  ('gmaail.com','gmail.com',1),
  ('gmacil.com','gmail.com',1),
  ('gmai.con','gmail.com',1),
  ('gmaial.com','gmail.com',1),
  ('gmail.bom','gmail.com',1),
  ('gmail.ciom','gmail.com',1),
  ('gmail.cmm','gmail.com',1),
  ('gmail.cmo','gmail.com',1),
  ('gmail.coim','gmail.com',1),
  ('gmail.cokm','gmail.com',1),
  ('gmail.comd','gmail.com',1),
  ('gmail.comii','gmail.com',1),
  ('gmail.comq','gmail.com',1),
  ('gmail.comt','gmail.com',1),
  ('gmail.comv','gmail.com',1),
  ('gmail.comw','gmail.com',1),
  ('gmail.comya','gmail.com',1),
  ('gmail.cov','gmail.com',1),
  ('gmail.cvom','gmail.com',1),
  ('gmail.xcom','gmail.com',1),
  ('gmail.zom','gmail.com',1),
  ('gmaila.com','gmail.com',1),
  ('gmaim.con','gmail.com',1),
  ('gmaiml.com','gmail.com',1),
  ('gmall.com','gmail.com',1),
  ('gmamail.com','gmail.com',1),
  ('gmao.com','gmail.com',1),
  ('gmaoil.com','gmail.com',1),
  ('gmaqqil.com','gmail.com',1),
  ('gmei.com','gmail.com',1),
  ('gmeill.com','gmail.com',1),
  ('gmiail.com','gmail.com',1),
  ('gmil.con','gmail.com',1),
  ('gmimail.com','gmail.com',1),
  ('gmnail.com','gmail.com',1),
  ('gmqil.fom','gmail.com',1),
  ('gmsul.com','gmail.com',1),
  ('gmual.com','gmail.com',1),
  ('gnmai.com','gmail.com',1),
  ('gqmil.com','gmail.com',1),
  ('guimail.com','gmail.com',1),
  ('hahoo.es','yahoo.es',1),
  ('hahoot.com','yahoo.com',1),
  ('hgmail.com','gmail.com',1),
  ('hiotmail.com','hotmail.com',1),
  ('hitmail.con','hotmail.com',1),
  ('hitnail.com','hotmail.com',1),
  ('hlotmail.com','hotmail.com',1),
  ('hltmail.com','hotmail.com',1),
  ('hmail.con','gmail.com',1),
  ('hoatmail.com','hotmail.com',1),
  ('hogmail.com','hotmail.com',1),
  ('hoitmail.com','hotmail.com',1),
  ('holmal.com','hotmail.com',1),
  ('homail.co','hotmail.com',1),
  ('homail.es','hotmail.es',1),
  ('homtmail.com','hotmail.com',1),
  ('hornail.com','hotmail.com',1),
  ('hornail.es','hotmail.es',1),
  ('hotamiail.com','hotmail.com',1),
  ('hotamil.es','hotmail.es',1),
  ('hotlook.es','outlook.es',1),
  ('hotma.es','hotmail.es',1),
  ('hotmai.es','hotmail.es',1),
  ('hotmaii.com','hotmail.com',1),
  ('hotmaik.com','hotmail.com',1),
  ('hotmail.c.om','hotmail.com',1),
  ('hotmail.ces','hotmail.es',1),
  ('hotmail.cim','hotmail.com',1),
  ('hotmail.cin','hotmail.com',1),
  ('hotmail.copm','hotmail.com',1),
  ('hotmail.cvom','hotmail.com',1),
  ('hotmail.lcom','hotmail.com',1),
  ('hotmail.von','hotmail.com',1),
  ('hotmail.xom','hotmail.com',1),
  ('hotmaila.com','hotmail.com',1),
  ('hotmaill.con','hotmail.com',1),
  ('hotmait.com','hotmail.com',1),
  ('hotmaiul.cpm','hotmail.com',1),
  ('hotmal.co','hotmail.com',1),
  ('hotmal.con','hotmail.com',1),
  ('hotmal.es','hotmail.es',1),
  ('hotmalil.com','hotmail.com',1),
  ('hotmall.com','hotmail.com',1),
  ('hotmamil.com','hotmail.com',1),
  ('hotmaol.es','hotmail.es',1),
  ('hotmaqil.co','hotmail.com',1),
  ('hotmaqil.com','hotmail.com',1),
  ('hotmauil.com','hotmail.com',1),
  ('hotmei.com','hotmail.com',1),
  ('hotmeil.com','hotmail.com',1),
  ('hotmiail.com','hotmail.com',1),
  ('hotmiil.com','hotmail.com',1),
  ('hotmmail.com','hotmail.com',1),
  ('hotmoal.com','hotmail.com',1),
  ('hotmsil.es','hotmail.es',1),
  ('hotmsoil.com','hotmail.com',1),
  ('hotmual.com','hotmail.com',1),
  ('hotmzail.com','hotmail.com',1),
  ('hotymal.com','hotmail.com',1),
  ('houtlok.com','outlook.com',1),
  ('houtlook.com','outlook.com',1),
  ('houtlook.es','outlook.es',1),
  ('hoymail.es','hotmail.es',1),
  ('hoytmail.com','hotmail.com',1),
  ('hpptmail.com','hotmail.com',1),
  ('hptmaol.com','hotmail.com',1),
  ('htmail.es','hotmail.es',1),
  ('iclaud.com','icloud.com',1),
  ('iclod.com','icloud.com',1),
  ('iclood.com','icloud.com',1),
  ('iclou.com','icloud.com',1),
  ('icloub.com','icloud.com',1),
  ('iclouk.com','icloud.com',1),
  ('iclowd.com','icloud.com',1),
  ('icluoud.com','icloud.com',1),
  ('icoud.com','icloud.com',1),
  ('icould.com','icloud.com',1),
  ('igloud.com','icloud.com',1),
  ('isloud.com','icloud.com',1),
  ('itloud.com','icloud.com',1),
  ('khotmail.com','hotmail.com',1),
  ('lhotmail.com','hotmail.com',1),
  ('lide.com','live.com',1),
  ('livc.com','live.com',1),
  ('live.cm','live.com',1),
  ('maigl.com','gmail.com',1),
  ('mail.con','gmail.com',1),
  ('ngmail.com','gmail.com',1),
  ('notmail.com','hotmail.com',1),
  ('oclud.com','icloud.com',1),
  ('oitlook.es','outlook.es',1),
  ('oputlook.es','outlook.es',1),
  ('otllok.com','outlook.com',1),
  ('otluook.com','outlook.com',1),
  ('otulook.com','outlook.com',1),
  ('oucklook.com','outlook.com',1),
  ('ouclok.com','outlook.com',1),
  ('ouclood.es','outlook.es',1),
  ('oukloot.es','outlook.es',1),
  ('ouktloo.com','outlook.com',1),
  ('oulok.com','outlook.com',1),
  ('oultlock.com','outlook.com',1),
  ('oultook.es','outlook.es',1),
  ('outkloo.com','outlook.com',1),
  ('outkook.com','outlook.com',1),
  ('outkool.com','outlook.com',1),
  ('outllok.es','outlook.es',1),
  ('outllook.com','outlook.com',1),
  ('outlock.com','outlook.com',1),
  ('outlokk.es','outlook.es',1),
  ('outloock.es','outlook.es',1),
  ('outlook.lc','outlook.es',1),
  ('outlool.com','outlook.com',1),
  ('outloook.es','outlook.es',1),
  ('outlot.com','outlook.com',1),
  ('outlouk.com','outlook.com',1),
  ('outock.com','outlook.com',1),
  ('outooak.es','outlook.es',1),
  ('outook.com','outlook.com',1),
  ('putlook.con','outlook.com',1),
  ('qhotmail.com','hotmail.com',1),
  ('thotmail.com','hotmail.com',1),
  ('uahoo.com','yahoo.com',1),
  ('yaahoot.es','yahoo.es',1),
  ('yahhoo.es','yahoo.es',1),
  ('yahlo.com','yahoo.com',1),
  ('yaho.com','yahoo.com',1),
  ('yahol.com','yahoo.com',1),
  ('yahoo.esd','yahoo.es',1),
  ('yahooo.es','yahoo.es',1),
  ('yahot.com','yahoo.com',1),
  ('yahot.es','yahoo.es',1),
  ('yajo.es','yahoo.es',1),
  ('yajoo.es','yahoo.es',1),
  ('yanhoo.es','yahoo.es',1),
  ('yhaoo.es','yahoo.es',1),
  ('yhoo.com','yahoo.com',1),
  ('yoho.es','yahoo.es',1),
  ('yohoot.com','yahoo.com',1)
ON DUPLICATE KEY UPDATE bueno = VALUES(bueno), veces_detectadas = VALUES(veces_detectadas);

-- ---------------------------------------------------------------------------
-- 2. Respaldo. Guarda el antes y el despues de cada fila que se toque.
--    Para devolverse:
--      UPDATE crm.persona p JOIN crm.respaldo_email_typo_20261112 r
--         ON r.tabla='crm.persona' AND r.llave=p.idpersona
--        SET p.email = r.email_antes WHERE p.email = r.email_despues;
--    (y lo mismo para las otras dos tablas)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crm.respaldo_email_typo_20261112 (
  tabla         VARCHAR(30)  NOT NULL,
  llave         BIGINT       NOT NULL,
  email_antes   VARCHAR(100) NOT NULL,
  email_despues VARCHAR(100) NOT NULL,
  creado_en     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (tabla, llave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- ---------------------------------------------------------------------------
-- 3. Tabla de trabajo con las LLAVES PRIMARIAS afectadas.
--
--    POR QUE PRIMERO SE MATERIALIZA Y DESPUES SE ACTUALIZA
--    El cruce por SUBSTRING_INDEX(email,'@',-1) no puede usar ningun indice,
--    asi que es un recorrido completo de la tabla. Hacerlo DENTRO de un UPDATE
--    sobre pizzaamericana.cliente es exactamente lo que bloqueo la tabla seis
--    minutos el 2026-09-15. Aca el recorrido se hace en un SELECT, que no
--    estorba, y el UPDATE va por PRIMARY KEY -eq_ref-, que es instantaneo.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS crm.tmp_correo_arreglo;
CREATE TABLE crm.tmp_correo_arreglo (
  tabla       VARCHAR(30)  NOT NULL,
  llave       BIGINT       NOT NULL,
  email_antes VARCHAR(100) NOT NULL,
  email_nuevo VARCHAR(100) NOT NULL,
  PRIMARY KEY (tabla, llave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

INSERT INTO crm.tmp_correo_arreglo (tabla, llave, email_antes, email_nuevo)
SELECT 'crm.persona', p.idpersona, p.email,
       CONCAT(SUBSTRING_INDEX(p.email,'@',1), '@', d.bueno)
  FROM crm.persona p
  JOIN crm.dominio_correccion d
    ON d.malo = LOWER(TRIM(SUBSTRING_INDEX(p.email,'@',-1)))
 WHERE p.email LIKE '%@%';

INSERT INTO crm.tmp_correo_arreglo (tabla, llave, email_antes, email_nuevo)
SELECT 'crm.persona_resumen', r.idpersona, r.email,
       CONCAT(SUBSTRING_INDEX(r.email,'@',1), '@', d.bueno)
  FROM crm.persona_resumen r
  JOIN crm.dominio_correccion d
    ON d.malo = LOWER(TRIM(SUBSTRING_INDEX(r.email,'@',-1)))
 WHERE r.email LIKE '%@%';

INSERT INTO crm.tmp_correo_arreglo (tabla, llave, email_antes, email_nuevo)
SELECT 'pizzaamericana.cliente', c.idcliente, c.email,
       CONCAT(SUBSTRING_INDEX(c.email,'@',1), '@', d.bueno)
  FROM pizzaamericana.cliente c
  JOIN crm.dominio_correccion d
    ON d.malo = LOWER(TRIM(SUBSTRING_INDEX(c.email,'@',-1)))
 WHERE c.email LIKE '%@%';

-- ---------------------------------------------------------------------------
-- 4. Respaldo antes de tocar nada.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO crm.respaldo_email_typo_20261112 (tabla, llave, email_antes, email_despues)
SELECT tabla, llave, email_antes, email_nuevo FROM crm.tmp_correo_arreglo;

-- ---------------------------------------------------------------------------
-- 5. La correccion. Cada UPDATE va por PRIMARY KEY.
--    El "AND email = email_antes" es lo que lo hace idempotente y ademas
--    impide pisar un correo que alguien haya cambiado entre tanto.
-- ---------------------------------------------------------------------------
UPDATE crm.persona p
  JOIN crm.tmp_correo_arreglo t
    ON t.tabla = 'crm.persona' AND t.llave = p.idpersona
   SET p.email = t.email_nuevo
 WHERE p.email = t.email_antes;

UPDATE crm.persona_resumen r
  JOIN crm.tmp_correo_arreglo t
    ON t.tabla = 'crm.persona_resumen' AND t.llave = r.idpersona
   SET r.email = t.email_nuevo
 WHERE r.email = t.email_antes;

UPDATE pizzaamericana.cliente c
  JOIN crm.tmp_correo_arreglo t
    ON t.tabla = 'pizzaamericana.cliente' AND t.llave = c.idcliente
   SET c.email = t.email_nuevo
 WHERE c.email = t.email_antes;

-- ---------------------------------------------------------------------------
-- 6. Comprobacion. Las tres cifras tienen que quedar en CERO.
-- ---------------------------------------------------------------------------
SELECT 'crm.persona'            AS tabla, COUNT(*) AS quedan_malos
  FROM crm.persona p JOIN crm.dominio_correccion d
    ON d.malo = LOWER(TRIM(SUBSTRING_INDEX(p.email,'@',-1))) WHERE p.email LIKE '%@%'
UNION ALL
SELECT 'crm.persona_resumen', COUNT(*)
  FROM crm.persona_resumen r JOIN crm.dominio_correccion d
    ON d.malo = LOWER(TRIM(SUBSTRING_INDEX(r.email,'@',-1))) WHERE r.email LIKE '%@%'
UNION ALL
SELECT 'pizzaamericana.cliente', COUNT(*)
  FROM pizzaamericana.cliente c JOIN crm.dominio_correccion d
    ON d.malo = LOWER(TRIM(SUBSTRING_INDEX(c.email,'@',-1))) WHERE c.email LIKE '%@%';

DROP TABLE IF EXISTS crm.tmp_correo_arreglo;

-- Y se recoge la mesa: tabla de trabajo del analisis que armo este mapa.
DROP TABLE IF EXISTS crm.tmp_dominio_malo;
