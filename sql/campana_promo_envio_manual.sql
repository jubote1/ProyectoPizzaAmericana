-- ---------------------------------------------------------------------------
-- Campanas de promocion: registro del envio MANUAL de la boleta.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Decision de Juan David el 2026-10-20: el premio NO lo manda el sistema. Las
-- boletas de Cinepolis son imagenes que el tiene en su computador, cada una con
-- su propio codigo de barras unico, y las manda por correo a mano.
--
-- Por eso no hay oferta ni codigo emitido: campana_promo.idoferta se queda en
-- cero y el sistema solo MARCA quien la pidio. Lo que si hace falta es saber
-- QUE boleta le toco a cada cliente, porque los codigos de barras son unicos:
-- sin eso no hay forma de saber si una ya se mando, ni a quien, y mandar dos
-- veces la misma deja a un cliente con una boleta que no le van a recibir.
--
-- Aditivo e idempotente. Se puede correr con la tabla ya en uso.
-- ---------------------------------------------------------------------------

-- El patron de siempre para agregar columnas sin reventar si ya existen.
drop procedure if exists agregar_columna_campana_promo;
delimiter //
create procedure agregar_columna_campana_promo(in tabla varchar(64), in columna varchar(64),
                                               in definicion varchar(255))
begin
    if not exists (select 1 from information_schema.columns
                    where table_schema = database() and table_name = tabla
                      and column_name = columna) then
        set @ddl = concat('alter table ', tabla, ' add column ', columna, ' ', definicion);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end //
delimiter ;

-- El codigo de barras de la boleta que se le mando. Vacio mientras no se haya
-- enviado. Es la unica forma de saber cual boleta salio para quien.
call agregar_columna_campana_promo('pedido_campana_promo', 'codigo_boleta',
     'varchar(60) not null default '''' ');

-- Cuando se mando y quien la mando. Van en NULL y no con CURRENT_TIMESTAMP: un
-- default de ahora marcaria como enviadas todas las filas viejas en el momento
-- del ALTER, que es exactamente el error que ya se cometio con
-- pedido_gestion_link y dejo 1.082 gestiones diciendo que se hicieron el mismo
-- dia al mediodia.
call agregar_columna_campana_promo('pedido_campana_promo', 'fecha_envio',
     'timestamp null default null');
call agregar_columna_campana_promo('pedido_campana_promo', 'usuario_envio',
     'varchar(40) not null default '''' ');

drop procedure if exists agregar_columna_campana_promo;

-- Para buscar rapido "a quien le falta por enviar".
create index ix_campana_promo_envio on pedido_campana_promo (idcampana, solicito, estado);

-- ---------------------------------------------------------------------------
-- Como quedo.
-- ---------------------------------------------------------------------------
select column_name, column_type, is_nullable, column_default
  from information_schema.columns
 where table_schema = database() and table_name = 'pedido_campana_promo'
 order by ordinal_position;
