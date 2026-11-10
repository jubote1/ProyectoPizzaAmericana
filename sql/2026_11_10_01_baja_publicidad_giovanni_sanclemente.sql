-- BAJA DE PUBLICIDAD - Giovanni Sanclemente Delgado
-- Solicitud formal por correo a mercadeo@pizzaamericana.com.co el 2026-11-07 10:27.
-- Pide baja de listas de publicidad, promociones, boletines y prospeccion comercial,
-- y revoca la autorizacion para uso publicitario. ALCANCE SOLO COMERCIAL: las
-- comunicaciones transaccionales, de facturacion y de seguridad deben continuar,
-- y por eso NO se toca nada de pedidos, puntos ni facturacion.
--
-- persona CRM 56375 | correo gsanclemente0729@gmail.com | celular 3023889645
-- Esta en 2 filas de pizzaamericana.cliente (309802, 505790) y 3 en Pilarica
-- (10658, 18813, 28961), todas de la tienda 9.
--
-- Idempotente: se puede volver a correr.

-- ============================================================
-- 1. CENTRAL
-- ============================================================
-- envio_publicidad es el "no me manden promociones" especifico y es el que miran
-- CampanaDAO (Brevo) y SegmentacionClienteDAO (correo directo).
-- La regla de CampanaDAO es la mas restrictiva: basta UNA fila en 'N' para dejar
-- a la persona por fuera. Se marcan las dos de todos modos.
UPDATE pizzaamericana.cliente
   SET envio_publicidad = 'N',
       politica_datos   = 'N',
       observacion      = CONCAT(IFNULL(observacion,''),
                          ' | 2026-11-07 solicito baja de publicidad por correo a mercadeo. NO enviar promociones.')
 WHERE idcliente IN (309802, 505790)
   AND IFNULL(envio_publicidad,'S') <> 'N';

-- ============================================================
-- 2. CRM
-- ============================================================
-- El CRM no tiene bandera propia de publicidad: el filtro de campanas lee
-- persona_resumen.politica_datos y ademas cruza en vivo contra
-- pizzaamericana.cliente.envio_publicidad. Se marcan las dos caras.
UPDATE crm.persona         SET politica_datos = 'N' WHERE idpersona = 56375;
UPDATE crm.persona_resumen SET politica_datos = 'N' WHERE idpersona = 56375;
