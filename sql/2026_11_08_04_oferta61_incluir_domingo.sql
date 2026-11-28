-- Oferta 61 (cupon 50% por los 25 anos): se abre tambien HOY domingo 8-nov.
-- Decision de Juan David el 2026-11-08: la campana sale hoy mismo, asi que el
-- cupon debe servir domingo, lunes y martes.
--
-- OJO: fecha_desde/fecha_hasta NO frenan un codigo POR PERSONA (ver
-- CodigoPromoDAO.motivoDeRechazo: solo aplican a los codigos ABIERTOS). Lo que
-- manda es la caducidad del propio codigo = dia de emision + dias_caducidad,
-- inclusive. Con dias_caducidad=2 y emision el domingo 8, vence el 10-nov:
-- exactamente domingo, lunes y martes. Las fechas se ajustan de todos modos
-- para que la pantalla diga la verdad.
UPDATE pizzaamericana.oferta
   SET fecha_desde    = '2026-11-08',
       fecha_hasta    = '2026-11-10',
       dias_caducidad = 2,
       mensaje1       = 'Hoy, manana lunes y el martes, todo el dia, tu pizza con 50% de descuento en Pizza Americana. Usa tu codigo al pedir: llama al 604 4444553 o acercate a un punto de venta. Compra minima $40.000, tope de descuento $30.000.',
       nombre_oferta  = 'CUPON 50% 25 ANOS - 8, 9 Y 10 NOV',
       habilitado     = 'S'
 WHERE idoferta = 61;
