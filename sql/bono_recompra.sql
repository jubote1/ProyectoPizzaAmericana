-- ---------------------------------------------------------------------------
-- BONO DE RECOMPRA
--
-- Se corre en el CENTRAL (172.19.0.25), base pizzaamericana. Idempotente y
-- aditivo: no borra ni modifica nada existente.
--
-- QUE ES
--
-- "Compre entre el 1 y el 15 y le devolvemos el 10% en un bono para usar entre
-- el 16 y el 30." Es al reves de lo que habia: hasta ahora el codigo se le
-- asignaba al cliente ANTES de comprar. Aqui el bono se GANA comprando.
--
-- POR QUE EL CALCULO NO PUEDE VIVIR EN EL CENTRAL
--
-- El central solo tiene los pedidos que el mismo tomo -medido: tipos 1 y 4-.
-- Todo el mostrador vive en la base de cada tienda. Si esto se calculara aqui,
-- el bono se lo ganaria unicamente quien pide a domicilio, y quien compra en
-- el punto de venta nunca, por mas que cumpla.
--
-- Por eso el calculo lo hace el barrido nocturno de Servicios, que ya entra a
-- las once tiendas, y estas tablas son donde deja el resultado.
--
-- LAS TRES TABLAS
--
--   bono_campana   la regla: ventana, porcentaje, tope, que productos cuentan
--   bono_pedido    el libro: que pedido de que tienda conto y por cuanto
--   bono_emitido   el resultado: a quien se le dio, por cuanto, con que codigo
--
-- POR QUE UN LIBRO DE PEDIDOS Y NO SOLO EL TOTAL
--
-- Tres razones:
--
--   1. El barrido corre todas las noches sobre la misma ventana. Sin saber que
--      pedido ya conto, la segunda noche sumaria otra vez lo de la primera y el
--      bono creceria solo. La llave (idbono, idtienda, idpedido) lo hace
--      imposible, no algo que haya que cuidar.
--   2. Es lo unico que responde "por que a este cliente le dieron $12.400".
--      Sin el detalle, un reclamo no se puede atender.
--   3. Permite que la campana sea repetible: se sabe cuales pedidos ya se
--      pagaron con un bono y cuales todavia no.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- 1. LA CAMPANA
-- ===========================================================================

CREATE TABLE IF NOT EXISTS pizzaamericana.bono_campana (
	idbono            INT          NOT NULL AUTO_INCREMENT,
	nombre            VARCHAR(100) NOT NULL,

	-- La oferta que se emite. Es una oferta normal de las que ya existen, con
	-- red_parcial = 'S' -para que el bono lleve saldo- y dias_caducidad > 0.
	-- De ella salen tambien las reglas de uso: monto minimo para redimir,
	-- tiendas donde vale, mostrador o domicilio, usos por cliente.
	idoferta          INT          NOT NULL,

	-- La ventana en la que la compra cuenta.
	compra_desde      DATE         NOT NULL,
	compra_hasta      DATE         NOT NULL,

	-- Cuanto se devuelve. El tope es el techo del bono, no de la compra.
	porcentaje        DECIMAL(5,2) NOT NULL DEFAULT 10.00,
	tope_bono         DOUBLE       NOT NULL DEFAULT 0 COMMENT '0 = sin tope',

	-- Cuanto hay que haber comprado -ya filtrado por producto y promociones-
	-- para ganarse algo.
	base_minima       DOUBLE       NOT NULL DEFAULT 0,

	-- Que cuenta para el porcentaje. Vacio = todo lo que se vendio.
	-- Lista de idproducto separados por coma, por ejemplo '6,7' para Grande y
	-- Extragrande.
	productos         VARCHAR(500)     NULL,

	-- Si una linea vendida con promocion suma a la base. Por defecto NO: pagar
	-- cashback sobre lo que ya salio con descuento es descontar dos veces.
	excluir_promociones CHAR(1)    NOT NULL DEFAULT 'S',

	-- N = una sola vez por persona en toda la campana.
	-- S = puede volver a ganarselo con pedidos que todavia no han contado.
	repetible         CHAR(1)      NOT NULL DEFAULT 'N',

	estado            VARCHAR(12)  NOT NULL DEFAULT 'BORRADOR'
	                               COMMENT 'BORRADOR, ACTIVA, CERRADA',
	-- Mientras este en N el barrido calcula y deja todo listo pero NO emite ni
	-- avisa. Es el ensayo: se mira a cuantos les daria y por cuanto, y despues
	-- se prende.
	emitir            CHAR(1)      NOT NULL DEFAULT 'N',
	avisar            CHAR(1)      NOT NULL DEFAULT 'S',

	usuario           VARCHAR(50)      NULL,
	creada_en         DATETIME     NOT NULL,
	ultimo_calculo_en DATETIME         NULL,
	PRIMARY KEY (idbono),
	KEY idx_bono_estado (estado, compra_desde, compra_hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Campanas de bono de recompra: compre ahora y le devolvemos un bono';

-- ===========================================================================
-- 2. EL LIBRO DE PEDIDOS QUE CONTARON
-- ===========================================================================

CREATE TABLE IF NOT EXISTS pizzaamericana.bono_pedido (
	idbono       INT      NOT NULL,
	idtienda     INT      NOT NULL,
	idpedido     INT      NOT NULL COMMENT 'el id en la base de ESA tienda',
	idpersona    BIGINT   NOT NULL,
	fecha        DATE     NOT NULL,
	base         DOUBLE   NOT NULL COMMENT 'lo que de este pedido cuenta',
	-- Cuando este pedido ya se pago dentro de un bono, queda amarrado a el.
	-- NULL = todavia no se ha pagado, esta esperando.
	idemision    INT          NULL,
	leido_en     DATETIME NOT NULL,
	PRIMARY KEY (idbono, idtienda, idpedido),
	KEY idx_bonoped_persona (idbono, idpersona, idemision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Que pedido conto para que bono y por cuanto';

-- ===========================================================================
-- 3. LO QUE SE EMITIO
-- ===========================================================================

CREATE TABLE IF NOT EXISTS pizzaamericana.bono_emitido (
	idemision       INT      NOT NULL AUTO_INCREMENT,
	idbono          INT      NOT NULL,
	idpersona       BIGINT   NOT NULL,
	-- Cuantas veces se le ha dado bono a esta persona en esta campana. En una
	-- campana NO repetible siempre vale 1, y el indice unico de abajo hace que
	-- un segundo bono sea IMPOSIBLE, no algo que el programa tenga que cuidar.
	secuencia       INT      NOT NULL DEFAULT 1,
	idcliente       INT          NULL COMMENT 'la fila de cliente a la que se le colgo la oferta',
	pedidos         INT      NOT NULL DEFAULT 0,
	base            DOUBLE   NOT NULL DEFAULT 0,
	valor           DOUBLE   NOT NULL DEFAULT 0,
	topado          CHAR(1)  NOT NULL DEFAULT 'N' COMMENT 'S si el tope le recorto el bono',
	idofertacliente INT          NULL,
	codigo          VARCHAR(20)  NULL,
	fecha_caducidad DATE         NULL,
	destino         VARCHAR(120) NULL COMMENT 'a donde se le aviso',
	estado          VARCHAR(12) NOT NULL DEFAULT 'CALCULADO'
	                            COMMENT 'CALCULADO, EMITIDO, AVISADO, FALLIDO',
	detalle         VARCHAR(300) NULL,
	calculado_en    DATETIME NOT NULL,
	avisado_en      DATETIME     NULL,
	PRIMARY KEY (idemision),
	KEY idx_emision_bono (idbono, estado),
	KEY idx_emision_persona (idbono, idpersona)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='A quien se le dio bono, por cuanto y con que codigo';

-- En una campana no repetible la secuencia siempre es 1, asi que el segundo
-- bono a la misma persona choca contra esta llave y no entra. Si el barrido se
-- corre dos veces la misma noche -o dos veces a la vez-, el resultado es el
-- mismo: un solo bono.
SET @n := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'pizzaamericana' AND TABLE_NAME = 'bono_emitido'
             AND INDEX_NAME = 'uk_emision_unica');
SET @s := IF(@n = 0,
	'ALTER TABLE pizzaamericana.bono_emitido ADD UNIQUE KEY uk_emision_unica (idbono, idpersona, secuencia)',
	'DO 0');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

SELECT 'Listo. bono_campana, bono_pedido y bono_emitido.' AS resultado;
