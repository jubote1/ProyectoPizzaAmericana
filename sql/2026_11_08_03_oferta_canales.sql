-- ============================================================================
-- OFERTAS POR CANAL: TIENDA VIRTUAL Y CONTACT CENTER
-- 2026-11-08
--
-- QUE RESUELVE
--
-- Hasta hoy una oferta se podia limitar por tienda (columna `tiendas`) y por
-- mostrador/domicilio (columna `aplica_a`), pero NO por el canal de donde viene
-- el pedido. Una oferta pensada para recuperar clientes de la tienda virtual se
-- podia redimir igual por el contact center, y al reves.
--
-- La columna `contact` ya existia pero era solo informativa: NUNCA se valido.
-- Cuatro ofertas la tienen en 'S' (45, 49, 60, 61) y eso no cambiaba nada.
--
-- LA REGLA, decidida por Juan David el 2026-11-08
--
--   canales = ''    la oferta vale en CUALQUIER canal (como hasta hoy)
--   canales = 'V'   tienda virtual   + mostrador
--   canales = 'C'   contact center   + mostrador
--   canales = 'VC'  los dos          + mostrador
--
-- EL MOSTRADOR SIEMPRE PUEDE. Decision explicita: si el cliente se aparece en
-- el punto de venta con su codigo, no se le puede decir que no. Lo que la
-- restriccion deja por fuera son las PLATAFORMAS (DiDi, Rappi) y el canal que
-- no se haya marcado.
--
-- Para limitar ademas por mostrador/domicilio esta `aplica_a`, que es otro eje
-- y sigue funcionando igual.
--
-- DE DONDE SALE EL CANAL. De `pedido.usuariopedido`. Medido en octubre de 2026:
--   TIENDA VIRTUAL   4.291 pedidos    la tienda virtual (GloriaFood/Kuno)
--   DIDI             6.940            plataforma
--   RAPPI            2.657            plataforma
--   lmperez, Mgallo, sloperaf, ...    los agentes del contact center
-- El mostrador no pasa por ahi: lo digita la caja en el POS.
--
-- Idempotente.
-- ============================================================================

USE pizzaamericana;

SET @hay := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = 'pizzaamericana' AND table_name = 'oferta'
               AND column_name = 'canales');
SET @sql := IF(@hay = 0,
  'ALTER TABLE oferta
     ADD COLUMN canales VARCHAR(10) NOT NULL DEFAULT ''''
       COMMENT ''Vacio = cualquier canal. V = tienda virtual, C = contact center. El mostrador siempre puede.''
     AFTER contact',
  'SELECT ''la columna canales ya existia'' AS aviso');
PREPARE p FROM @sql; EXECUTE p; DEALLOCATE PREPARE p;

-- ----------------------------------------------------------------------------
-- Las que hoy tienen contact = 'S' NO se migran a canales = 'C'.
--
-- Ese flag nunca se valido, asi que esas cuatro ofertas se vienen redimiendo
-- por cualquier canal. Ponerles 'C' de un dia para otro les quitaria la tienda
-- virtual sin que nadie lo haya pedido. Arrancan todas en '' -cualquier canal,
-- igual que hoy- y quien quiera restringir una lo hace a mano.
-- ----------------------------------------------------------------------------

-- ============================================================================
-- COMPROBACION
-- ============================================================================
SELECT column_name, column_type, column_default
  FROM information_schema.columns
 WHERE table_schema = 'pizzaamericana' AND table_name = 'oferta' AND column_name = 'canales';

SELECT canales, COUNT(*) AS ofertas FROM oferta GROUP BY canales;
