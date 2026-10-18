-- ---------------------------------------------------------------------------
-- TOPE DE ENVIOS DEL LINK DE PAGO A LA MISMA DIRECCION (base general del CENTRAL)
--
-- Aditivo e idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
-- Correrlo ANTES de desplegar el .war (si no esta, el codigo asume 3 igual).
--
-- QUE PASO
--
-- El 2026-10-17, entre las 20:40 y las 20:41, el link de pago del pedido 2395879 salio
-- unas once veces a la misma direccion. Las once rebotaron con "No se ha encontrado la
-- direccion": la direccion no existe.
--
-- POR QUE NO SE DETECTO SOLO
--
-- El correo del link de pago sale por el Gmail de CUENTACORREOWOMPI, no por Brevo. Para
-- el servidor SMTP el envio sale bien: el rebote llega minutos despues, como un correo
-- de vuelta al buzon, que nadie lee desde el sistema. Por eso crm.correo_rebotado -que
-- la llena ProcesoRebotesBrevo- no se entera nunca de estos rebotes, y el codigo que
-- envia tampoco. Nada aprendia, y por eso se podia insistir sin limite.
--
-- QUE HACE ESTE PARAMETRO
--
-- Es el tope de veces que se le manda el link del MISMO pedido a la MISMA direccion,
-- contando los envios automaticos y los reenvios manuales juntos. Al llegar al tope, la
-- pantalla de reenvio deja de intentarlo y dice que hay que pedirle otra direccion al
-- cliente.
--
-- El tope es POR DIRECCION, no por pedido: escribir otra direccion siempre se puede, que
-- es justamente la salida cuando la que tenemos esta mala.
--
-- COMO AJUSTARLO
--
-- Subirlo si se ve que hay clientes legitimos quedandose sin su link; bajarlo a 2 si se
-- quiere ser mas estricto. En 0 o sin la fila, el codigo asume 3.
-- ---------------------------------------------------------------------------

USE general;

INSERT INTO parametros (valorparametro, valornumerico, valortexto)
SELECT 'TOPEENVIOSLINKPAGOCORREO', 3, 'Veces maximas que se manda el link de pago del mismo pedido a la misma direccion'
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'TOPEENVIOSLINKPAGOCORREO');
