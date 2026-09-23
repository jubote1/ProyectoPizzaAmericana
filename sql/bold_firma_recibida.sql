-- ---------------------------------------------------------------------------
-- Bold: guardar la firma y el tipo de contenido tal como llegaron.
-- Base de datos: pizzaamericana (172.19.0.25). Idempotente.
--
-- Con eventos reales la firma no coincide (motivo_firma = NO_COINCIDE) y sin la
-- firma que Bold manda no hay forma de saber por que. La firma no es un secreto:
-- viaja en cada peticion. Con ella, el cuerpo (json_evento) y la llave se puede
-- comprobar que variante del calculo usa Bold sin adivinar.
--
-- Correr ANTES de desplegar el war del central que escribe estas columnas.
-- (Si se despliega antes, el webhook responde 500 y Bold reintenta despues; no se
-- pierde el evento, pero queda sin guardar mientras tanto.)
-- ---------------------------------------------------------------------------

SET @faltan = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'log_evento_bold' AND column_name = 'firma_recibida');

SET @alterar = IF(@faltan = 0, 'ALTER TABLE log_evento_bold ADD COLUMN firma_recibida varchar(300) DEFAULT NULL AFTER motivo_firma, ADD COLUMN content_type varchar(100) DEFAULT NULL AFTER firma_recibida', 'SELECT 1');

PREPARE ejecutar_alter FROM @alterar;

EXECUTE ejecutar_alter;

DEALLOCATE PREPARE ejecutar_alter;
