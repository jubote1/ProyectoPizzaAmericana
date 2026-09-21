-- ---------------------------------------------------------------------------
-- PASO 5 del plan "Un Solo Cliente": la tabla resumen por persona
--
-- Se ejecuta en el CENTRAL (172.19.0.25). Idempotente.
--
-- QUE RESUELVE
--
-- crm.v_persona_360 responde muy bien UNA persona -0,4 s con WHERE idpersona =
-- N- pero esta hecha de subconsultas correlacionadas: un tablero que agrupe las
-- 450 mil personas la haria recorrer los 785 mil pedidos una vez por persona.
-- Sobre el mismo servidor donde estan vendiendo las once tiendas, eso no se
-- puede. La tabla resumen se calcula UNA vez por noche y el tablero la lee.
--
-- EL HALLAZGO QUE OBLIGA A LA SEGUNDA TABLA
--
-- El central NO tiene los pedidos de mostrador. Medido el 2026-09-20 sobre
-- siete dias:
--
--   tipo 1 Domicilio          central 4.427    tiendas 4.394
--   tipo 4 Virtual Recoger    central    28    tiendas    28
--   tipo 2 Punto de Venta     central     -    tiendas 1.868
--   tipo 3 Para Llevar        central     -    tiendas 1.163
--
-- O sea que el central ve 4.455 de 7.486 pedidos: el 41% es invisible. Un
-- resumen armado solo con pizzaamericana.pedido diria que un cliente de
-- mostrador no compra nunca, y esa es justo la clase de error que no se nota
-- -da un numero, no da un error-.
--
-- Por eso el reparto, verificado en las once tiendas: los tipos 1 y 4 se toman
-- del central, y los tipos 2 y 3 de cada tienda. Sin traslape y sin doble
-- conteo. Los cuatro ids son identicos en las once, se comprobo uno por uno.
--
-- crm.stage_pedido_tienda es donde aterriza lo que aporta cada tienda. Es
-- agregado -una fila por tienda y persona, no por pedido-, asi que son unas
-- decenas de miles de filas y no millones.
--
-- POR QUE AHORA Y NO ANTES
--
-- Esto solo es posible porque cliente.idpersona ya vive EN LA TIENDA. La
-- tienda puede agrupar sus propios pedidos por persona y mandar el agregado
-- listo, en vez de mandar los pedidos crudos para que el central los cruce.
-- ---------------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS crm;

-- ===========================================================================
-- 1. LO QUE APORTA CADA TIENDA
--
-- Una fila por (tienda, persona). La llena el proceso nocturno, que ya se
-- conecta a las once tiendas para mantener el maestro.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS crm.stage_pedido_tienda (
  idtienda       INT      NOT NULL,
  idpersona      BIGINT   NOT NULL,
  pedidos        INT      NOT NULL DEFAULT 0,
  valor          DOUBLE   NOT NULL DEFAULT 0,
  primer_pedido  DATE     NULL,
  ultimo_pedido  DATE     NULL,
  actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (idtienda, idpersona),
  KEY idx_spt_persona (idpersona)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3
  COMMENT='Mostrador y para llevar agregados por persona. Solo tipos 2 y 3.';

-- ===========================================================================
-- 2. LOS UMBRALES, PARAMETRIZADOS
--
-- Los dos numeros que deciden el segmento NO son una verdad del negocio, son
-- una propuesta mia. Van en tabla y no en el codigo para que operacion o
-- mercadeo los muevan sin volver a armar el jar.
--
-- 60 y 120 dias salen de que una pizzeria de barrio se compra cada pocas
-- semanas: quien no vuelve en dos meses ya cambio de habito, y a los cuatro
-- meses se perdio. Hay que confirmarlos con quien maneje mercadeo.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS crm.parametro_segmento (
  nombre      VARCHAR(40)  NOT NULL,
  valor       INT          NOT NULL,
  descripcion VARCHAR(255) NULL,
  PRIMARY KEY (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

INSERT IGNORE INTO crm.parametro_segmento (nombre, valor, descripcion) VALUES
  ('DIAS_ACTIVO', 60,  'Hasta cuantos dias sin comprar sigue activo el cliente'),
  ('DIAS_RIESGO', 120, 'Hasta cuantos dias esta en riesgo. Mas alla, dormido');

-- ===========================================================================
-- 3. LA TABLA RESUMEN
--
-- Una fila por persona REAL -las alias no entran, sus pedidos se suman a la
-- principal-. Se reconstruye entera cada noche, asi que no guarda nada que no
-- se pueda recalcular: es derivada, no es fuente.
--
-- EL CHARSET VA COLUMNA POR COLUMNA, igualando el de la columna de origen.
-- nombre y apellido son utf8mb4 en crm.persona -hay nombres con caracteres de
-- cuatro bytes- y el resto es utf8mb3. Declarar la tabla entera en uno solo
-- rompe: en utf8mb3 falla la copia de los nombres con ERROR 1366, y en utf8mb4
-- se caen los JOIN por celular contra cliente.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS crm.persona_resumen (
  idpersona       BIGINT NOT NULL,

  -- Quien es. Denormalizado a proposito: la pantalla de segmentacion filtra y
  -- lista por estos campos, y volver a crm.persona por cada fila la volveria
  -- lenta justo en lo que esta tabla existe para evitar.
  nombre          VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  apellido        VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  celular         VARCHAR(10)  CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  email           VARCHAR(50)  CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  politica_datos  CHAR(1)      CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,

  -- Separado por origen para poder auditar. Si manana el reparto de tipos se
  -- rompe -una tienda que empiece a subir mostrador al central-, el doble
  -- conteo se ve comparando estas dos columnas contra el total, y no queda
  -- escondido dentro de una sola suma.
  pedidos_central INT    NOT NULL DEFAULT 0,
  valor_central   DOUBLE NOT NULL DEFAULT 0,
  pedidos_tienda  INT    NOT NULL DEFAULT 0,
  valor_tienda    DOUBLE NOT NULL DEFAULT 0,

  pedidos         INT    NOT NULL DEFAULT 0,
  valor           DOUBLE NOT NULL DEFAULT 0,
  ticket_promedio DOUBLE NOT NULL DEFAULT 0,

  primer_pedido   DATE NULL,
  ultimo_pedido   DATE NULL,

  -- Recencia y frecuencia, ya calculadas. Son las dos preguntas que hace
  -- mercadeo -hace cuanto no viene, cada cuanto venia- y calcularlas en la
  -- pantalla obligaria a repetir la formula en cada consulta.
  dias_sin_comprar   INT NULL,
  dias_entre_pedidos INT NULL,

  tiendas_distintas INT NOT NULL DEFAULT 0,
  tienda_habitual   INT NULL,

  -- NUEVO / ACTIVO / EN RIESGO / DORMIDO / SIN PEDIDOS. Ver los umbrales en
  -- crm.parametro_segmento.
  segmento VARCHAR(12) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'SIN PEDIDOS',

  actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (idpersona),
  KEY idx_pr_segmento (segmento),
  KEY idx_pr_ultimo   (ultimo_pedido),
  KEY idx_pr_valor    (valor),
  KEY idx_pr_tienda   (tienda_habitual),
  KEY idx_pr_celular  (celular)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3
  COMMENT='Resumen por persona para el CRM. DERIVADA: se reconstruye cada noche.';

-- Como quedo
SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='crm'
   AND TABLE_NAME IN ('stage_pedido_tienda','persona_resumen','parametro_segmento')
 ORDER BY TABLE_NAME;
SELECT nombre, valor FROM crm.parametro_segmento ORDER BY nombre;
