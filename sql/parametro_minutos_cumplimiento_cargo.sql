-- ---------------------------------------------------------------------------
-- Dashboard Cargo: meta de minutos para considerar un pedido "cumplido".
-- Base de datos: general (172.19.0.25)
--
-- El Dashboard Cargo (menuadm > Funciones > Dashboard Cargo) mide cumplimiento
-- no solo por si el pedido se entrego, sino por si se entrego dentro de esta
-- meta, contada de pedido.fechainsercion a pedido.fecha_entregado. 60 minutos
-- es el numero que hoy se maneja como promedio de entrega.
--
-- Si este parametro no existe todavia, el Dashboard usa 60 por defecto en
-- codigo, asi que correr esto no es bloqueante para desplegar la pantalla,
-- pero es lo que permite cambiar la meta despues sin tocar codigo.
-- ---------------------------------------------------------------------------

INSERT INTO general.parametros (valorparametro, valornumerico)
VALUES ('MINUTOSCUMPLIMIENTOCARGO', 60) AS nuevo
ON DUPLICATE KEY UPDATE valornumerico = nuevo.valornumerico;
