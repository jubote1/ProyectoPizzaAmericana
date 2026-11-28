-- ---------------------------------------------------------------------------
-- Marcar el premio sospechoso en vez de retenerlo.
--
-- POR QUE SE CAMBIA. El vigilante retenia el premio poniendo idofertacliente =
-- -2, que la pantalla de dispersion no puede tocar. Juan David lo planteo mejor
-- el 28-nov-2026: que el premio quede MARCADO y que quien dispersa decida -lo
-- entrega, o se lo reporta a el-.
--
-- Y tiene razon, por una razon concreta: la dispersion YA ES MANUAL, una
-- persona oprime el boton. O sea que la marca alcanza a un humano antes de que
-- se mueva la plata. Retener, en cambio, le quita la decision a esa persona y
-- obliga a meterse a la base para soltarlo. Y hay casos legitimos que caen en
-- la regla -una familia, un pedido corregido varias veces-.
--
-- ASI QUE: el premio sigue PENDIENTE y dispersable, pero la pantalla lo muestra
-- como SOSPECHOSO con el motivo al lado. Retener queda como interruptor
-- (VIGILANTEENCUESTA_RETIENE = 'S'), apagado por defecto.
--
-- Los 5 premios bloqueados el 27-nov con -2 se quedan como estan: esos los
-- mando bloquear el usuario expresamente.
--
-- Idempotente.
-- ---------------------------------------------------------------------------

create table if not exists pizzaamericana.premio_sospechoso (
  idresultado  int          not null comment 'resultado_ruleta.idresultado',
  regla        varchar(20)  not null comment 'TELEFONO o NAVEGADOR',
  motivo       varchar(200) not null comment 'lo que se le muestra a quien dispersa',
  fecha_marca  timestamp    not null default current_timestamp,
  primary key (idresultado),
  key idx_sospechoso_fecha (fecha_marca)
) engine=InnoDB default charset=latin1;

-- Marcar es lo normal; retener es la excepcion.
update general.parametros set valortexto = 'N'
 where valorparametro = 'VIGILANTEENCUESTA_RETIENE' and valortexto = 'S';

select valorparametro, valortexto,
       case when valortexto = 'N' then 'solo marca (quien dispersa decide)'
            else 'ademas retiene' end as que_hace
  from general.parametros where valorparametro = 'VIGILANTEENCUESTA_RETIENE';

select count(*) as premios_marcados from pizzaamericana.premio_sospechoso;
