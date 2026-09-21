-- ---------------------------------------------------------------------------
-- Seguridad por roles: catalogo de roles, pantallas, menus y permisos.
-- Base de datos: pizzaamericana (172.19.0.25)
--
-- Reemplaza el booleano quemado usuario.administrador (N/S/P) por un modelo
-- de roles real: que pantallas ve cada rol, agrupadas en menu y submenu, y
-- una tabla puente (pantalla_servlet) para que un futuro filtro de seguridad
-- pueda verificar en el servidor -no solo ocultar botones en el menu- si el
-- usuario tiene permiso sobre la URL que esta pidiendo.
--
-- Dia 1: nadie pierde ni gana acceso. Los roles se siembran 1:1 desde lo que
-- ya existe (administrador='S' -> Lider Administrativo, ='N' -> Operario,
-- ='P' -> Administrador PQRS/Contact Center), y rol_pantalla se llena
-- exactamente con lo que cada menu estatico de hoy ya muestra
-- (Menu.html, MenuAdm.html, MenuPQRS.html). El usuario podra partir el rol
-- "Administrador PQRS/Contact Center" en roles mas finos despues, desde la
-- pantalla de administracion de roles -es solo dato, no requiere desplegar
-- codigo de nuevo.
--
-- Correr esto ANTES de desplegar el war con SeguridadFilter/GetMenuUsuario.
-- ---------------------------------------------------------------------------

-- PASO 1. Catalogo de roles.
create table if not exists rol (
    idrol       int          not null auto_increment,
    nombre      varchar(60)  not null,
    descripcion varchar(200) not null default '',
    activo      char(1)      not null default 'S',
    primary key (idrol),
    unique key nombre (nombre)
);

insert into rol (nombre, descripcion) values
    ('Operario', 'Perfil operativo de punto de venta / contact center, migrado desde administrador=N'),
    ('Lider Administrativo', 'Perfil administrativo completo, migrado desde administrador=S'),
    ('Administrador PQRS/Contact Center', 'Perfil de gestion de PQRS, migrado desde administrador=P. Partir en roles mas finos si el negocio lo requiere')
on duplicate key update idrol = idrol;

-- PASO 2. Menus y submenus (reemplaza los 3 dropdown de cada HTML estatico).
create table if not exists menu_modulo (
    idmodulo        int         not null auto_increment,
    nombre          varchar(60) not null,
    orden           int         not null default 0,
    idmodulo_padre  int         null,
    activo          char(1)     not null default 'S',
    primary key (idmodulo),
    unique key nombre_padre (nombre, idmodulo_padre)
);

insert into menu_modulo (nombre, orden) values
    ('Funciones', 10),
    ('Parametrizacion', 20),
    ('Monitoreo', 30)
on duplicate key update idmodulo = idmodulo;

-- PASO 3. Pantallas (una fila por pantalla funcional, no por servlet crudo).
create table if not exists pantalla (
    idpantalla int          not null auto_increment,
    nombre     varchar(150) not null,
    idmodulo   int          not null,
    url_html   varchar(150) not null,
    orden      int          not null default 0,
    activo     char(1)      not null default 'S',
    primary key (idpantalla),
    unique key url_html (url_html)
);

-- PASO 4. Que endpoints de servlet respaldan cada pantalla. Se siembra solo
-- lo que ya se conoce con certeza (ejemplo de referencia); el resto se llena
-- durante la fase de SeguridadFilter en modo LOG, revisando que URLs pide
-- cada pantalla en produccion. Una pantalla sin filas aqui NO queda
-- bloqueada en modo LOG (solo se audita), asi que no frena el despliegue.
create table if not exists pantalla_servlet (
    idpantalla int         not null,
    patron_url varchar(150) not null,
    primary key (idpantalla, patron_url)
);

-- PASO 5. La matriz de permisos: que pantallas puede ver cada rol.
create table if not exists rol_pantalla (
    idrol      int not null,
    idpantalla int not null,
    primary key (idrol, idpantalla)
);

-- PASO 6. Que rol(es) tiene cada usuario. N:M aunque hoy sea 1:1, para no
-- tener que rehacer el esquema si algun dia alguien necesita mas de un rol.
create table if not exists usuario_rol (
    idusuario int not null,
    idrol     int not null,
    primary key (idusuario, idrol)
);

-- =============================================================================
-- SIEMBRA: pantallas + a que modulo pertenecen.
-- Fuente: MenuAdm.html (el menu mas completo) para Funciones/Parametrizacion/
-- Monitoreo, mas 2 pantallas que solo existen hoy en MenuPQRS.html.
-- =============================================================================

insert into pantalla (nombre, idmodulo, url_html, orden)
select v.nombre, m.idmodulo, v.url_html, v.orden
from (
    -- Funciones
    select 'Nuevo Pedido - Clasico' nombre, 'Funciones' modulo, 'PedidosClasico.html' url_html, 10 orden union all
    select 'Nuevo Pedido', 'Funciones', 'Pedidos.html', 20 union all
    select 'Consulta Pedidos Registrados', 'Funciones', 'ConsultaPedidos.html', 30 union all
    select 'Consultar Estados Pedido Tienda', 'Funciones', 'EstadosPedidosTienda.html', 40 union all
    select 'Consultar Pedidos En Curso', 'Funciones', 'ConsultaPedidosEnCurso.html', 50 union all
    select 'Consultar Ped TIENDA VIRTUAL', 'Funciones', 'ConsultaPedidosNuevaTienda.html', 60 union all
    select 'Consultar Pedidos APP', 'Funciones', 'ConsultaPedidosAPP.html', 70 union all
    select 'Consulta Plataforma Domicilios', 'Funciones', 'ConsultaPedidosDomicom.html', 80 union all
    select 'Consulta Pedidos CRM BOT', 'Funciones', 'ConsultaPedidosCRMBOT.html', 90 union all
    select 'RAPPI CARGO', 'Funciones', 'ConsultaCARGO.html', 100 union all
    select 'Registrar PQRS', 'Funciones', 'PQRS.html', 110 union all
    select 'Consulta PQRS', 'Funciones', 'ConsultaPQRS.html', 120 union all
    select 'Modificar PQRS', 'Funciones', 'ModificarPQRS.html', 130 union all
    select 'Consulta Tiempos Tiendas', 'Funciones', 'ConsultaTiemposPedidos.html', 140 union all
    select 'Administracion de Clientes', 'Funciones', 'CRMClientes.html', 150 union all
    select 'Validar Codigo Promocional', 'Funciones', 'ValidarCodigo.html', 160 union all
    select 'Consulta Direccion Fuera Zona', 'Funciones', 'ConsultaDireccionFueraZona.html', 170 union all
    select 'Ubicar Pedidos Tomados en Mapa-Zona', 'Funciones', 'ConsultaPedidosMapaEvolutivo.html', 180 union all
    select 'Resultado Encuesta Mejor Empleado', 'Funciones', 'ResultadoEncuesta.html', 190 union all
    select 'Encuestas de Mercadeo (POS)', 'Funciones', 'ConsultaEncuestaMercadeo.html', 200 union all
    select 'Dispersion Premios Ruleta', 'Funciones', 'DispersionPremiosRuleta.html', 210 union all
    select 'Novedades de Biometria', 'Funciones', 'NovedadesBiometria.html', 220 union all
    select 'Revision Devolucion de Puntos', 'Funciones', 'RevisionReversaRedencion.html', 230 union all
    select 'Historial de Estado de un Pedido', 'Funciones', 'EstadosHistorialPedido.html', 240 union all
    select 'Consultar Solicitudes Fact Electronicas', 'Funciones', 'ConsultaSolicitudFacturaElectronica.html', 250 union all
    select 'Consultar Ventas Empresariales', 'Funciones', 'ConsultaVentaEmpresarial.html', 260 union all
    select 'Consultar Estados Datafonos', 'Funciones', 'ConsultarEstadoDatafonos.html', 270 union all
    select 'Registrar Pedido Cumpleanos', 'Funciones', 'RegistroPedidoCumple.html', 280 union all
    select 'Consultar Registro Pedido Cumpleanos', 'Funciones', 'ConsultarRegistroPedidoCumple.html', 290 union all
    -- Parametrizacion
    select 'Especialidades', 'Parametrizacion', 'Especialidad.html', 10 union all
    select 'Excepciones de Precios', 'Parametrizacion', 'ExcepcionPrecio.html', 20 union all
    select 'Administrar Ofertas', 'Parametrizacion', 'Oferta.html', 30 union all
    select 'Estado Pedido', 'Parametrizacion', 'EstadoPedido.html', 40 union all
    select 'Productos', 'Parametrizacion', 'Producto.html', 50 union all
    select 'Sabor por Tipo de Liquido', 'Parametrizacion', 'SaborTipoLiquido.html', 60 union all
    select 'Tiendas', 'Parametrizacion', 'Tienda.html', 70 union all
    select 'Tiendas Bloqueadas', 'Parametrizacion', 'TiendaBloqueada.html', 80 union all
    select 'Tipo Liquido', 'Parametrizacion', 'TipoLiquido.html', 90 union all
    select 'Productos No Existentes', 'Parametrizacion', 'MarcarProductoNoExistente.html', 100 union all
    select 'Asignar Tiempos Pedido Tiendas', 'Parametrizacion', 'AsignarTiemposPedidos.html', 110 union all
    select 'Administracion de Clientes Full', 'Parametrizacion', 'CRMClientesFull.html', 120 union all
    select 'Consultar Ubicacion Domiciliario', 'Parametrizacion', 'mapa.jsp', 130 union all
    select 'Modificacion General Pedido', 'Parametrizacion', 'ModificarGeneralPedido.html', 140 union all
    select 'Roles de Seguridad', 'Parametrizacion', 'Rol.html', 150 union all
    select 'Asignar Pantallas a Rol', 'Parametrizacion', 'AsignarPantallasRol.html', 160 union all
    select 'Asignar Rol a Usuario', 'Parametrizacion', 'AsignarRolUsuario.html', 170 union all
    -- Monitoreo
    select 'Monitoreo Plataformas Domicilios', 'Monitoreo', 'MonitoreoPlataformas.html', 10 union all
    select 'Monitoreo Pagos Virtuales', 'Monitoreo', 'MonitoreoPagosVirtuales.html', 20 union all
    select 'Cancelaciones Plataformas', 'Monitoreo', 'CancelacionesPlataformas.html', 30 union all
    select 'Consulta Puntos Cliente', 'Monitoreo', 'ConsultaPuntosCliente.html', 40 union all
    select 'Segmentacion Cliente', 'Monitoreo', 'segmentacionCliente.html', 50 union all
    select 'Ingresar Solicitud Conciliacion', 'Monitoreo', 'ConciliacionQRDATAFONO.html', 60 union all
    select 'Consultar Solicitudes Conciliacion', 'Monitoreo', 'ConsultarConciliacionQRDATAFONO.html', 70 union all
    select 'Consulta Ventas Asesor', 'Monitoreo', 'ConsultaVentasAsesor.html', 80 union all
    select 'Venta Integral', 'Monitoreo', 'VentaIntegral.html', 90 union all
    select 'Venta Integral - Categorias', 'Monitoreo', 'VentaIntegralCategorias.html', 100
) v
join menu_modulo m on m.nombre = v.modulo
where not exists (select 1 from pantalla p where p.url_html = v.url_html);

-- =============================================================================
-- SIEMBRA: rol_pantalla, clonando exactamente lo que cada menu de hoy muestra.
-- =============================================================================

-- Operario (hoy Menu.html)
insert into rol_pantalla (idrol, idpantalla)
select r.idrol, p.idpantalla
from rol r
join pantalla p on p.url_html in (
    'PedidosClasico.html','Pedidos.html','ConsultaPedidos.html','EstadosPedidosTienda.html',
    'ConsultaPedidosEnCurso.html','ConsultaPedidosNuevaTienda.html','ConsultaPedidosAPP.html',
    'ConsultaPedidosDomicom.html','ConsultaPedidosCRMBOT.html','PQRS.html','ConsultaPQRS.html',
    'ModificarPQRS.html','ConsultaTiemposPedidos.html','CRMClientes.html','ValidarCodigo.html',
    'ModificarGeneralPedido.html','EstadosHistorialPedido.html','ConsultaSolicitudFacturaElectronica.html',
    'ConsultaVentaEmpresarial.html','ConsultarEstadoDatafonos.html','CRMClientesFull.html',
    'MonitoreoPlataformas.html','MonitoreoPagosVirtuales.html','CancelacionesPlataformas.html',
    'ConsultaPuntosCliente.html','ConsultaVentasAsesor.html'
)
where r.nombre = 'Operario'
on duplicate key update idrol = idrol;

-- Lider Administrativo (hoy MenuAdm.html: todas las pantallas sembradas
-- arriba EXCEPTO las 2 exclusivas de PQRS).
insert into rol_pantalla (idrol, idpantalla)
select r.idrol, p.idpantalla
from rol r
join pantalla p on p.url_html not in ('RegistroPedidoCumple.html','ConsultarRegistroPedidoCumple.html')
where r.nombre = 'Lider Administrativo'
on duplicate key update idrol = idrol;

-- Administrador PQRS/Contact Center (hoy MenuPQRS.html)
insert into rol_pantalla (idrol, idpantalla)
select r.idrol, p.idpantalla
from rol r
join pantalla p on p.url_html in (
    'ConsultaPedidos.html','ConsultaSolicitudFacturaElectronica.html','PQRS.html','ConsultaPQRS.html',
    'ValidarCodigo.html','EstadosHistorialPedido.html','RegistroPedidoCumple.html',
    'ConsultarRegistroPedidoCumple.html','CancelacionesPlataformas.html','ConsultarConciliacionQRDATAFONO.html'
)
where r.nombre = 'Administrador PQRS/Contact Center'
on duplicate key update idrol = idrol;

-- =============================================================================
-- SIEMBRA: usuario_rol, desde la columna administrador que ya existe hoy.
-- =============================================================================
insert into usuario_rol (idusuario, idrol)
select u.id, r.idrol
from usuario u
join rol r on (
    (u.administrador = 'N' and r.nombre = 'Operario') or
    (u.administrador = 'S' and r.nombre = 'Lider Administrativo') or
    (u.administrador = 'P' and r.nombre = 'Administrador PQRS/Contact Center')
)
where u.activo = 1
on duplicate key update idusuario = idusuario;

-- PASO 7. Ejemplo de pantalla_servlet ya conocido (Especialidad y Venta
-- Integral, construidos recientemente): referencia de la forma que debe
-- tomar el resto conforme se complete durante la fase LOG.
insert into pantalla_servlet (idpantalla, patron_url)
select p.idpantalla, v.patron_url
from pantalla p
join (
    select 'Especialidad.html' url_html, '/CRUDEspecialidad' patron_url union all
    select 'Especialidad.html', '/GetEspecialidades' union all
    select 'VentaIntegral.html', '/ConsultarVentaIntegral' union all
    select 'VentaIntegral.html', '/GetTiendas' union all
    select 'VentaIntegralCategorias.html', '/CRUDVentaIntegralCategoria' union all
    select 'VentaIntegralCategorias.html', '/GetVentaIntegralCategorias' union all
    select 'Rol.html', '/CRUDRol' union all
    select 'Rol.html', '/GetRoles' union all
    select 'AsignarPantallasRol.html', '/GetRoles' union all
    select 'AsignarPantallasRol.html', '/GetModulosConPantallas' union all
    select 'AsignarPantallasRol.html', '/CRUDRolPantalla' union all
    select 'AsignarRolUsuario.html', '/GetRoles' union all
    select 'AsignarRolUsuario.html', '/GetUsuariosConRoles' union all
    select 'AsignarRolUsuario.html', '/CRUDUsuarioRol'
) v on v.url_html = p.url_html
on duplicate key update idpantalla = idpantalla;

-- PASO 8. Parametro que controla el modo del filtro de seguridad. Arranca en
-- LOG (audita, no bloquea) hasta que pantalla_servlet este razonablemente
-- completo. Vive en general.parametros, igual que los demas parametros del
-- central (NO en pizzaamericana.parametros, esa tabla no existe).
insert into general.parametros (valorparametro, valortexto)
values ('SEGURIDADFILTROMODO', 'LOG')
on duplicate key update valorparametro = valorparametro;

-- PASO 9. Verificacion.
select r.nombre rol, count(*) pantallas_asignadas
from rol r join rol_pantalla rp on rp.idrol = r.idrol
group by r.nombre;

select m.nombre modulo, count(*) pantallas
from menu_modulo m join pantalla p on p.idmodulo = m.idmodulo
group by m.nombre;

select r.nombre rol, count(*) usuarios
from rol r join usuario_rol ur on ur.idrol = r.idrol
group by r.nombre;
