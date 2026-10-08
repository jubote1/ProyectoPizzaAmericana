-- Oferta base reusable para las campañas de Bono de Recompra (cashback por compra).
--
-- El valor en pesos de cada bono NO sale de esta fila: lo calcula cada campaña
-- (ej. BonoRecompraDAO, porcentaje x compra con su propio tope) y se pasa directo
-- a CodigoPromoDAO.emitirValor, junto con la fecha de vencimiento fija de la
-- campaña -que pisa dias_caducidad cuando viene informada-. Por eso los tres
-- campos de descuento quedan en 0: emitirValor nunca los lee para este flujo.
--
-- Idempotente: si ya existe una oferta con este nombre, no hace nada.
INSERT INTO oferta (
	nombre_oferta, idexcepcion, codigo_promocional, descuento_fijo_porcentaje,
	descuento_porcentaje_futuro, descuento_fijo_valor, mensaje1, mensaje2,
	dias_caducidad, tipo_caducidad, controla_hora, hora_inicio, hora_fin,
	tipo_oferta, fecha_desde, fecha_hasta, codigo_general, contact,
	red_parcial, reintegro, habilitado
)
SELECT
	'BONO RECOMPRA - BASE', 0, 'S', 0,
	0, 0, 'Tienes un bono de recompra disponible', 'Úsalo antes de que venza',
	7, 'P', 'N', '', '',
	'C', '2026-09-30', '2030-12-31', 'N', 'N',
	'S', 'N', 'S'
WHERE NOT EXISTS (
	SELECT 1 FROM oferta WHERE nombre_oferta = 'BONO RECOMPRA - BASE'
);
