-- ---------------------------------------------------------------------------
-- LECTOR DE REBOTES DEL BUZON (base general del CENTRAL)
--
-- Aditivo e idempotente: se puede correr dos veces. Corre tal cual en HeidiSQL.
-- Correrlo ANTES de la primera corrida de ServiciosSer.ProcesoRebotesBuzon.
--
-- QUE CIERRA ESTO
--
-- ProcesoRebotesBrevo aprende de los rebotes de las campanas, pero el link de pago no
-- sale por Brevo: sale directo por SMTP desde el Gmail de CUENTACORREOWOMPI. Para el
-- SMTP ese envio es exitoso y el rebote llega minutos despues COMO UN MENSAJE al buzon,
-- que nadie leia. Por eso una direccion muerta se podia reintentar para siempre.
--
-- ProcesoRebotesBuzon lee ese buzon por IMAP y anota los rebotes en
-- crm.correo_rebotado con origen 'BUZON', que es la misma lista que ya consulta el
-- envio del link de pago (ver 2026_10_18_01_tope_envios_link_pago.sql).
--
-- LOS DOS PARAMETROS
--
--   REBOTESBUZONDESDE    hasta que fecha se leyo (lo escribe el proceso solo). Se deja
--                        vacio: en la primera corrida mira 30 dias hacia atras. Para
--                        releer un rango, basta con ponerle una fecha anterior.
--
--   IMAPSERVIDORREBOTES  servidor IMAP. Vacio o sin la fila = imap.gmail.com, que es lo
--                        que aplica hoy. Existe para no tener que desplegar si la cuenta
--                        se mueve de proveedor.
--
-- La cuenta y la clave NO se agregan aqui: el proceso usa las que ya existen,
-- CUENTACORREOWOMPI y CLAVECORREOWOMPI.
--
-- OJO ANTES DE LA PRIMERA CORRIDA
--
-- La cuenta necesita IMAP habilitado en Gmail, y si tiene verificacion en dos pasos, la
-- clave de CLAVECORREOWOMPI tiene que ser una CLAVE DE APLICACION. Si no, el proceso no
-- entra y lo dice en pantalla.
-- ---------------------------------------------------------------------------

USE general;

INSERT INTO parametros (valorparametro, valornumerico, valortexto)
SELECT 'REBOTESBUZONDESDE', 0, ''
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'REBOTESBUZONDESDE');

INSERT INTO parametros (valorparametro, valornumerico, valortexto)
SELECT 'IMAPSERVIDORREBOTES', 0, 'imap.gmail.com'
WHERE NOT EXISTS (SELECT 1 FROM parametros WHERE valorparametro = 'IMAPSERVIDORREBOTES');
