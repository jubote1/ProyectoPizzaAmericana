-- ---------------------------------------------------------------------------
-- EL PERFIL DE GUSTO: que especialidad prefiere cada persona
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente.
--
-- Es la base de las estrategias de recompra: para poder decirle a alguien
-- "sabemos que te encanta la Hawaiana, prueba la Pinaronni" hay que saber,
-- primero, que le encanta la Hawaiana. Hoy crm.persona_resumen sabe cuanto
-- compra cada quien y cada cuanto, pero no QUE compra.
--
-- ===========================================================================
-- POR QUE NO VA COMO SEGMENTO
-- ===========================================================================
--
-- Seria lo natural -ya existe segmento_definicion con sus reglas- pero NO se
-- puede: pr_clasificar_segmentos escribe UNA sola columna, persona_resumen.
-- segmento, y cada persona termina con un unico segmento. Si se agregara
-- "AMANTE HAWAIANA" ahi, esa gente dejaria de ser ORO, FIEL o POR RECUPERAR, y
-- se romperia la segmentacion que ya esta en uso.
--
-- El gusto es OTRO EJE: una persona es FIEL y ademas amante de la Hawaiana.
-- Por eso va en columnas propias, y por eso estas columnas NO se declaran en
-- segmento_campo: si se declararan, alguien podria armar un segmento con ellas
-- y volveria el mismo problema.
--
-- El publico de un envio se arma leyendo persona_resumen con un WHERE
-- (CampanaDAO.cargarDestinatarios), y es ahi donde estas columnas se van a
-- usar: como un filtro mas, encima del segmento, no en su lugar.
--
-- ===========================================================================
-- LA FAMILIA, Y POR QUE HACE FALTA
-- ===========================================================================
--
-- "Hawaiana" (idespecialidad 1) esta INACTIVA y hoy se vende como "Hawaiana
-- Artesanal" (38). Son el mismo gusto y hay que contarlas juntas: 114.014 mas
-- 56.510 pizzas en doce meses. Separadas, la favorita mas comun de la compania
-- aparece partida en dos.
--
-- Las demas van cada una por su lado. Americana, Americana Especial y
-- Americana Premium NO se agrupan a proposito: son productos distintos con
-- precios distintos, y sugerirle a alguien lo que ya compra no es una
-- sugerencia.
--
-- La tabla es editable: el dia que dos especialidades sean la misma cosa, se
-- cambia una fila y el proximo calculo lo refleja.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LA FAMILIA DE CADA ESPECIALIDAD
-- ===========================================================================

CREATE TABLE IF NOT EXISTS crm.especialidad_familia (
  idespecialidad INT         NOT NULL,
  familia        VARCHAR(40) NOT NULL,
  PRIMARY KEY (idespecialidad),
  KEY idx_especialidad_familia (familia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='Agrupa especialidades que son el mismo gusto';

-- El nombre se toma de la tabla, no se escribe aqui: asi este archivo queda en
-- ASCII puro y la familia dice exactamente lo que ve el cliente en la carta.
INSERT INTO crm.especialidad_familia (idespecialidad, familia)
SELECT e.idespecialidad, UPPER(e.nombre)
  FROM pizzaamericana.especialidad e
 WHERE NOT EXISTS (SELECT 1 FROM crm.especialidad_familia ya
                    WHERE ya.idespecialidad = e.idespecialidad);

-- La unica union: las dos hawaianas son el mismo gusto.
UPDATE crm.especialidad_familia f
   SET f.familia = (SELECT UPPER(e.nombre) FROM pizzaamericana.especialidad e
                     WHERE e.idespecialidad = 38)
 WHERE f.idespecialidad IN (1, 38);

-- ===========================================================================
-- 2. LAS COLUMNAS DEL PERFIL
-- ===========================================================================

SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA='crm' AND TABLE_NAME='persona_resumen'
                   AND COLUMN_NAME='familia_favorita');
SET @sql := IF(@existe = 0,
  'ALTER TABLE crm.persona_resumen
     ADD COLUMN familia_favorita VARCHAR(40) NULL COMMENT ''Especialidad que mas pide, agrupada por familia'',
     ADD COLUMN fidelidad_favorita INT NULL COMMENT ''Que porcentaje de sus pizzas es esa familia'',
     ADD COLUMN pizzas_12m INT NOT NULL DEFAULT 0 COMMENT ''Pizzas con especialidad en los ultimos 12 meses'',
     ADD COLUMN gusto_actualizado_en DATETIME NULL',
  'SELECT ''las columnas de gusto ya existen'' AS paso');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA='crm' AND TABLE_NAME='persona_resumen'
                   AND COLUMN_NAME='familia_favorita' AND SEQ_IN_INDEX=1);
SET @sql := IF(@existe = 0,
  'ALTER TABLE crm.persona_resumen ADD INDEX idx_persona_gusto (familia_favorita, fidelidad_favorita)',
  'SELECT ''el indice de gusto ya existe'' AS paso');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ===========================================================================
-- 3. EL CALCULO INICIAL
--
-- Despues lo refresca cada noche el proceso de Servicios. Esta primera corrida
-- se hace aqui para que los datos existan desde ya.
--
-- SOLO SE VE LO QUE LLEGA AL CENTRAL. El mostrador no tiene detalle de pedido
-- en esta base, asi que el gusto se calcula sobre domicilio y virtual. Es una
-- limitacion real y hay que tenerla presente: quien solo compra en mostrador
-- queda sin perfil, no con un perfil equivocado.
--
-- UNA PIZZA MITAD Y MITAD CUENTA PARA LAS DOS especialidades. Se cuenta por
-- aparicion y no por pizza: a quien pide mitad Hawaiana todas las veces, la
-- Hawaiana le gusta.
-- ===========================================================================

DROP TEMPORARY TABLE IF EXISTS crm.gusto_tmp;

CREATE TEMPORARY TABLE crm.gusto_tmp (
  idpersona BIGINT      NOT NULL,
  familia   VARCHAR(40) NOT NULL,
  veces     INT         NOT NULL,
  PRIMARY KEY (idpersona, familia)
) ENGINE=InnoDB;

INSERT INTO crm.gusto_tmp (idpersona, familia, veces)
SELECT t.idpersona, t.familia, SUM(t.veces)
  FROM (
    SELECT c.idpersona, f.familia, COUNT(*) AS veces
      FROM pizzaamericana.pedido p
      JOIN pizzaamericana.detalle_pedido d ON d.idpedido = p.idpedido
      JOIN pizzaamericana.cliente c ON c.idcliente = p.idcliente
      JOIN crm.especialidad_familia f ON f.idespecialidad = d.idespecialidad1
     WHERE p.fechapedido >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
       AND p.numposheader > 0 AND c.idpersona > 0 AND d.idespecialidad1 > 0
     GROUP BY c.idpersona, f.familia
    UNION ALL
    SELECT c.idpersona, f.familia, COUNT(*)
      FROM pizzaamericana.pedido p
      JOIN pizzaamericana.detalle_pedido d ON d.idpedido = p.idpedido
      JOIN pizzaamericana.cliente c ON c.idcliente = p.idcliente
      JOIN crm.especialidad_familia f ON f.idespecialidad = d.idespecialidad2
     WHERE p.fechapedido >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
       AND p.numposheader > 0 AND c.idpersona > 0 AND d.idespecialidad2 > 0
     GROUP BY c.idpersona, f.familia
  ) t
 GROUP BY t.idpersona, t.familia;

-- Se limpia primero: quien dejo de comprar hace mas de un ano tiene que quedar
-- sin perfil, no con el de hace dos anos.
UPDATE crm.persona_resumen
   SET familia_favorita = NULL, fidelidad_favorita = NULL, pizzas_12m = 0,
       gusto_actualizado_en = NOW();

-- La favorita es la que mas veces aparece. El desempate va por nombre para que
-- dos corridas seguidas den lo mismo.
UPDATE crm.persona_resumen r
  JOIN (
    SELECT g.idpersona,
           SUBSTRING_INDEX(GROUP_CONCAT(g.familia ORDER BY g.veces DESC, g.familia SEPARATOR '||'), '||', 1) AS familia,
           MAX(g.veces) AS veces_favorita,
           SUM(g.veces) AS total
      FROM crm.gusto_tmp g
     GROUP BY g.idpersona
  ) x ON x.idpersona = r.idpersona
   SET r.familia_favorita = x.familia,
       r.fidelidad_favorita = ROUND(x.veces_favorita * 100 / x.total),
       r.pizzas_12m = x.total,
       r.gusto_actualizado_en = NOW();

DROP TEMPORARY TABLE IF EXISTS crm.gusto_tmp;

-- ===========================================================================
-- COMO QUEDO
-- ===========================================================================

SELECT COUNT(*) AS personas_con_perfil,
       SUM(fidelidad_favorita >= 60) AS con_favorita_clara,
       SUM(fidelidad_favorita >= 60 AND pizzas_12m >= 3) AS publico_util
  FROM crm.persona_resumen WHERE familia_favorita IS NOT NULL;

SELECT familia_favorita, COUNT(*) AS personas,
       SUM(fidelidad_favorita >= 60 AND pizzas_12m >= 3) AS la_prefieren_claro
  FROM crm.persona_resumen
 WHERE familia_favorita IS NOT NULL
 GROUP BY familia_favorita
 ORDER BY la_prefieren_claro DESC
 LIMIT 12;
