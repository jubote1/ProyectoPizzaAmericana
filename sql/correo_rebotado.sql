-- ---------------------------------------------------------------------------
-- CORREOS QUE REBOTAN
--
-- Se corre en el CENTRAL (172.19.0.25). Idempotente y aditivo.
--
-- EL PROBLEMA
--
-- Una direccion muerta se intenta en CADA campana, para siempre: nada lo
-- aprende. Se gasta cupo de Brevo, se gasta tiempo, y sobre todo se gasta la
-- reputacion del dominio, que es lo primero que miran Gmail y Outlook para
-- mandar a spam. Ahi no se pierde el correo de la campana: se pierde el de las
-- facturas.
--
-- MARCAR, NO BORRAR
--
-- El correo NO se le quita al cliente. El saldo de puntos se lleva por la
-- cadena del correo -codigo_redencion_puntos.correo, cliente_fidelizacion-, asi
-- que borrarlo le rompe los puntos a esa persona. Es justo lo que hubo que
-- arreglar a mano con David Flores el 2026-09-29.
--
-- Esta tabla dice "no le escriba a esta direccion", no "esta persona no
-- existe". La identidad se conserva; lo unico que se bloquea es el envio.
--
-- DURO Y BLANDO NO SON LO MISMO
--
-- Un rebote DURO -la direccion no existe- es definitivo. Uno BLANDO -buzon
-- lleno, servidor caido- es pasajero y la direccion puede volver a servir. Se
-- guardan los dos pero solo el duro bloquea, para no perder un cliente bueno
-- porque un dia tuvo el buzon lleno.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS crm.correo_rebotado (
	email         VARCHAR(120) NOT NULL,
	tipo          VARCHAR(10)  NOT NULL DEFAULT 'DURO' COMMENT 'DURO o BLANDO',
	motivo        VARCHAR(300)     NULL COMMENT 'lo que dijo el proveedor',
	origen        VARCHAR(20)  NOT NULL DEFAULT 'BREVO' COMMENT 'BREVO o DIRECTO',
	veces         INT          NOT NULL DEFAULT 1,
	primera_vez   DATETIME     NOT NULL,
	ultima_vez    DATETIME     NOT NULL,
	PRIMARY KEY (email),
	KEY idx_rebote_tipo (tipo),
	KEY idx_rebote_ultima (ultima_vez)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci
  COMMENT='Direcciones que rebotaron: no se les vuelve a escribir';

-- Hasta donde ya se le pregunto a Brevo. Sin esto, cada corrida volveria a
-- pedir el historico completo.
INSERT INTO general.parametros (valorparametro, valornumerico, valortexto)
VALUES ('BREVOREBOTESDESDE', 0, '')
ON DUPLICATE KEY UPDATE valortexto = valortexto;

SELECT 'Listo. crm.correo_rebotado.' AS resultado;
