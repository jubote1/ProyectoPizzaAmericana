-- ---------------------------------------------------------------------------
-- A quien le llega la alerta de fraude en encuestas.
--
-- Pedido de Juan David el 28-nov-2026: tecnologia, direccion general y lideres
-- de zona. Reemplaza la lista provisional, que se habia copiado de
-- RESULTADORULETA para no inventar correos.
--
-- LOS CORREOS NO SE ESCRIBEN AQUI. Se toman de las listas que ya existen en
-- parametros_correo buscando por el nombre de la cuenta. Asi este script no
-- guarda direcciones de nadie y, si una cuenta cambia, cambia en un solo lugar.
--
-- FALTAN LOS LIDERES DE ZONA. En parametros_correo no hay ninguna cuenta que
-- corresponda a ese cargo: no existe un 'liderzona@' ni nada parecido, y las
-- que mas se acercan -supervisoroperacion@, auxiliaroperacion@- son Lider de
-- Operacion, que es otro cargo. No se agregan a la adivina: este correo senala
-- empleados por nombre y mandarselo a quien no es seria peor que no mandarlo.
-- Cuando se sepan las cuentas, se agregan a ALERTAFRAUDEENCUESTA y listo; el
-- proceso lee la lista cada vez que corre, no hay que redesplegar nada.
--
-- Idempotente.
-- ---------------------------------------------------------------------------

-- Fuera la lista provisional.
delete from general.parametros_correo where valorparametro = 'ALERTAFRAUDEENCUESTA';

-- Tecnologia.
insert into general.parametros_correo (valorparametro, correo)
select distinct 'ALERTAFRAUDEENCUESTA', c.correo
  from general.parametros_correo c
 where c.correo like 'tecnologia@%'
   and not exists (select 1 from general.parametros_correo d
                    where d.valorparametro = 'ALERTAFRAUDEENCUESTA' and d.correo = c.correo);

-- Direccion general.
insert into general.parametros_correo (valorparametro, correo)
select distinct 'ALERTAFRAUDEENCUESTA', c.correo
  from general.parametros_correo c
 where c.correo like 'direcciongeneral@%'
   and not exists (select 1 from general.parametros_correo d
                    where d.valorparametro = 'ALERTAFRAUDEENCUESTA' and d.correo = c.correo);

-- Lideres de zona: pendiente. Cuando se sepan las cuentas:
--   insert into general.parametros_correo (valorparametro, correo)
--   values ('ALERTAFRAUDEENCUESTA', '<la cuenta>');

select valorparametro, correo from general.parametros_correo
 where valorparametro = 'ALERTAFRAUDEENCUESTA' order by correo;
