-- ---------------------------------------------------------------------------
-- Bold: entregar los eventos a la tienda aunque su firma no coincida.
-- Base de datos: general (172.19.0.25). Idempotente.
--
-- MODO TEMPORAL, bajo responsabilidad de quien lo prende (decision del usuario,
-- 2026-09-21). Con eventos reales, la firma x-bold-signature de Bold no se ha
-- podido reproducir con la llave secreta del panel; se probaron cientos de
-- combinaciones de cuerpo y de llave (ver log_evento_bold.firma_recibida) y
-- ninguna coincide. Mientras Bold aclara como se calcula, este parametro deja
-- pasar los eventos para poder probar el flujo completo.
--
--   'S'  entrega tambien los eventos cuya firma no coincide.
--   otro valor, o sin el parametro: solo se entregan los de firma valida.
--
-- La firma se sigue calculando y guardando (motivo_firma), aunque no frene la
-- entrega. RIESGO: sin firma, el webhook publico acepta cualquier cuerpo; quien
-- conozca la URL puede inventar un evento de pago QR. Apagar en cuanto se
-- resuelva la firma:
--   UPDATE general.parametros SET valortexto = 'N' WHERE valorparametro = 'BOLDENTREGASINFIRMA';
-- ---------------------------------------------------------------------------

INSERT INTO general.parametros (valorparametro, valortexto)
VALUES ('BOLDENTREGASINFIRMA', 'S') AS nuevo
ON DUPLICATE KEY UPDATE valortexto = nuevo.valortexto;
