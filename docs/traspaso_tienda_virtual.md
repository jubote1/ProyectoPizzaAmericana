# Traspaso: reemplazo de la tienda virtual (Gloria Food / Kuno)

Documento de continuidad. Resume todo lo investigado hasta 2026-10-28 en la sesión con Claude Code sobre el proyecto `ProyectoPizzaAmericana`, para continuar en otra herramienta o con el equipo de desarrollo.

**Convención de confianza** usada en todo el documento:
- **[Verificado]** leído directamente en el código o en el esquema de la base.
- **[Capturas]** visto en capturas de pantalla de Gloria Food / tienda virtual, aportadas por el usuario.
- **[Deducido]** inferido, no confirmado.
- **[Pendiente]** no se sabe aún.

No se han creado skills ni se ha modificado código. Este documento es el único entregable de la sesión.

---

## 1. Objetivo

Pizza Americana usa hoy una tienda virtual de **Gloria Food** (en el código aparece como **Kuno**; el panel es `mirestauranteenlinea.com`). Gloria Food / Oracle **se retira el 30 de abril de 2027** [Capturas]. Hay que construir una **tienda virtual propia** que:

1. Conserve en lo esencial el flujo de toma de pedidos que hoy funciona.
2. Mejore la experiencia y elimine lo tedioso de la tienda actual.
3. Use **el mismo menú y reglas** que los demás canales, sin configurar menús aparte por tienda y por canal.

Entregable final buscado por el usuario: un conjunto de **skills** (documentación operativa) que explique cada proceso, para pasarlo a un equipo de desarrollo.

## 2. Requisitos y decisiones expresadas por el usuario

- **Un solo punto de entrada.** Hoy en WordPress hay un botón "Ordenar" que abre un selector con **un botón por tienda** (11 tiendas). No quieren eso. Quieren un único módulo, con selector de tienda o, mejor, que la tienda se deduzca de la dirección.
- **Mantener el mapa de dirección** (funciona bien): el cliente escribe la dirección, el sistema sugiere la tienda más cercana, el cliente confirma. Hoy sale "Tu dirección está cerca de la tienda de: Bello. ¿Es correcto?" [Capturas]. Añadir selector manual de tienda como respaldo.
- **Menú más simple e interactivo**, pero completo. El actual se considera enredado.
- **Un solo menú de marca con herencia por tienda.** Hoy hay que configurar 11 menús en Gloria Food. Se quiere un menú base compartido y que cada tienda solo guarde **excepciones** (ocultar, cambiar precio, cambiar horario, agregar algo propio).
- **Fuente de verdad del menú: el POS de las tiendas** (según el usuario es el modelo "más acertado y completo"). El menú del contact center es "un remate" del POS y no está bien hecho. El modelo de tablas del menú del central "no es bueno".
- **Gloria Food está aislado del proyecto**: zonas, cobertura, productos y menú se configuraban allá por separado. Al central solo llega el pedido. No hay conexión de vuelta.
- **Disponibilidad y bloqueos** (hoy se hace a mano en Gloria Food):
  - Bloquear una tienda o un producto, para un canal específico (solo tienda virtual) o masivamente (todas las tiendas).
  - Disponibilidad por **día de la semana y horario** (ejemplo: un combo solo lunes, martes y jueves, de cierta hora a cierta hora).
  - Por tienda, por canal, o para todos.
  - Motivo: manejo de demanda; a veces se acaba o queda poco de un producto y hay que limitarlo en ciertos canales.
- **Hoy los bloqueos del central no llegan a la tienda virtual**: se deben repetir a mano en Gloria Food.
- El usuario quiere que las skills queden completas, claras para un equipo de desarrollo, sin hacerlas "a medias".

## 3. Vocabulario del negocio

- **Tamaños de pizza:** pizzeta (pequeña, 4 porciones), mediana (6), grande (8), extragrande (12) [Capturas].
- **"Con":** agregar un ingrediente al producto (con tocineta, con queso). **"Sin":** el producto sin un ingrediente. En el código del pedido virtual solo se ven "con" y adiciones; "sin" no apareció [Pendiente: verificar en el POS].
- **Mitad y mitad:** una pizza con dos sabores. Cada sabor puede tener recargo según el tamaño.
- **Especialidad:** el sabor de pizza (Hawaiana Artesanal, Carnis, Peperoni y Queso...).
- **Excepción de precio:** en la base, es el mecanismo de **promociones y combos** (`excepcion_precio`).
- **Pedido programado:** pedido para más tarde; la tienda permite programar cuando está cerrada [Capturas].
- **Pixel / POS PM:** los dos tipos de punto de venta de las tiendas (campo `tienda.pos`: 1 = POS propio de Pizza Americana, 2 = Pixel).
- **Central / contact center:** el sistema propio (este repositorio) donde se registran los pedidos.

## 4. Sistemas y proyectos

| Pieza | Qué es |
|---|---|
| `ProyectoPizzaAmericana` (este repo) | Central / contact center. Java (servlets), MySQL, front en HTML/JS. Capas: `capaServicioCC` (servlets), `capaControladorCC`, `capaDAOCC`, `capaModeloCC`. |
| Gloria Food / Kuno | Tienda virtual actual. Envía pedidos al central por POST. Configuración de menú, zonas y horarios vive allá. |
| WordPress (`pizzaamericana.co`) | Sitio web con el botón de la tienda, banners y horarios. |
| POS de cada tienda | Base de datos propia por tienda. Dos tipos: POS propio (MySQL, deducido) y Pixel (SQL Anywhere por DSN, según `ConexionBaseDatos`). |
| Wompi | Pasarela de pagos por link. |
| ArcGIS | Cobertura y geocodificación (zonas por tienda). |
| Kommo / Brevo / Ultramsg | CRM, correo/WhatsApp y mensajería. |
| Proyecto **Servicios** | Procesos automáticos (reenvío de pedidos a tienda, avisos de pago). **Aún no se revisó. [Pendiente: ruta]**. |

---

## 5. Flujo actual de un pedido de tienda virtual [Verificado]

Archivo principal: `src/main/java/capaControladorCC/PedidoCtrl.java`.

### 5.1 Entrada

- Servlet `/InsertarPedidoTiendaVirtualKuno` (`capaServicioCC/InsertarPedidoTiendaVirtualKuno.java`) recibe el JSON y delega en `PedidoCtrl.insertarPedidoTiendaVirtualKuno` (línea ~2364).
- Siempre responde 200 con cuerpo vacío; los errores se manejan por correo y por estado en base.
- Existen variantes: `/InsertarPedidoTiendaVirtual` (camino anterior, `PedidoCtrl.insertarPedidoTiendaVirtual`) y `/TratarPedidoMrTamalKuno` (marca hermana, ver 5.9).

### 5.2 Seguridad y registro

1. Se guarda el JSON crudo y el header en `log_pedido_virtual_kuno` (`LogPedidoVirtualKunoDAO`).
2. Se valida el header `authorization` contra la tabla `auth_kuno` (`AutenticacionCtrl.validarAuthKuno`, `AuthKunoDAO`). El valor literal `PRUEBA` salta la validación. Si no es válido: correo de alerta y respuesta vacía.

### 5.3 Estados y programados

- El JSON trae `orders[]`. Para cada orden: `id`, `total_price`, `for_later`, `status`, `fulfill_at`, `type`, `source`, `payment`, `restaurant_token`, `items[]`, datos del cliente.
- Si `status = pending` y no es programado, se **ignora**. Se procesan los `accepted` y los programados.
- Anti-duplicados: `pedido_tienda_virtual` (`PedidoTiendaVirtualDAO.insertarPedidoTiendaVirtual`) con `idordencomercio` único. Estados: `PEN` → `PRO` (procesado) o `ERR`.
  - Duplicado no programado: correo de alerta.
  - Duplicado programado que llega `accepted`: `PedidoDAO.actualizarProgramadoFinalizado` pasa el pedido a estado 2.
- Programado: estado de pedido 5, `programado='S'`, hora local calculada desde `fulfill_at` (llega en UTC / Zulu).

### 5.4 Cliente y dirección

- Teléfono: se quita `+57`. Si tiene 7 dígitos se trata como fijo y se antepone `604`; si no, se guarda también como celular.
- Dirección: usa `client_address`; si hay `client_address_parts.more_address`, se compone `street + city` y lo adicional va a observaciones.
- Coordenadas: `latitude`/`longitude` llegan como número cuando existen y como texto vacío cuando no. Se leen con `toString()`. Si ambas son 0 se geocodifica con `UbicacionCtrl.ubicarDireccionEnTiendaBatch`.
- Tienda y origen: `restaurant_token` → `homologacion_tienda_token` (token, idtienda, origen).
- Cliente (`ClienteCtrl.validarClienteTiendaVirtualKuno`, línea ~636): busca por teléfono + tienda; si no, por celular. Si existe, actualiza y deja una alerta con las diferencias de dirección (`prepararActClienteTiendaVirtual`) en `observacion_virtual`. Si no, lo crea. La dirección **no se normaliza**: se guarda con `idnomenclatura=1` y `idmunicipio=1`.
- `politica_datos` se marca en `S`.

### 5.5 Productos y detalle del pedido

`PedidoCtrl.ingresarPedidoTiendaVirtualKuno` (línea ~3459). Crea el encabezado (`PedidoDAO.InsertarEncabezadoPedidoTiendaVirtualKuno`, línea ~513, estado 1) y luego inserta el detalle.

- **Traducción por texto.** Cada ítem trae un `name` (SKU en texto) y `options[]` con `group_name` y `name`. Se traduce con `homologacion_producto_pedidovirtual (sku, tipo, idinterno)`; tipos: producto, `E` especialidad, `PR` promoción, `EX` excepción de precio, líquido. La tabla es **global** (no por tienda).
- **Tamaño:** del grupo de opciones "Tamaño" o "Size".
- **Reglas por nombre de grupo** (strings fijos en código): "Adicionar...", "...bebida...", "Condimentos", "Producto Adicional", "Elige hasta 3 ingredientes" (con variantes PZ, MD, GD, XL y reemplazos de "Elige 1 ingrediente..."), "Elige uno o dos sabor de tu pizza", "Elige uno o dos sabores para tu promoción", "Selecciona la especialidad 1/2", "Envío (obligatorio)", "Quieres el balon edicion limitada?".
- **Promociones:** si el SKU es de tipo `PR` solo aporta la cantidad. Si es excepción de precio (`EX`), se usa el precio de `excepcion_precio`. Reglas especiales por nombre: "Super Combo Medianas", "Combo 2 Medianas", "Pizzeta 2 x 1" (se insertan dos líneas, una por pizza).
- **Mitad y mitad:** `idespecialidad1` e `idespecialidad2`; el recargo por especialidad (`especialidad_excepcion`) se divide por 2 en cada mitad.
- **Adiciones, modificadores "con" y gaseosa** se insertan como líneas y se ligan al detalle padre (`adicion_detalle_pedido`, `modificador_detalle_pedido`). Productos incluidos: `producto_incluido`.
- **Descuentos y cupones:** líneas de tipo `promo_item`, `promo_cart`, `promo_cart_item` suman un descuento. Hay una lista **fija en el código** de cupones que se ignoran: FESTIVAL, 40000GRACIAS, DIADEPIZZA20, DOMIGRATIS, DEDITOS2024, Deditos.
- **Domicilio:** ver sección 9.

### 5.6 Cierre del pedido

- Descuento diario (`obtenerDescuentoDiarioPedidoVal`, tabla `descuentos_generales`); si no hay, se usan los cupones.
- Tiempo de entrega: `TiempoPedidoDAO.retornarTiempoPedidoTienda` (`tiempo_pedido_tienda`). Si la tienda está en `tienda_bloqueada` devuelve **100 minutos**.
- Total: `PedidoDAO.calcularTotalNetoPedido` menos el descuento. Se compara con `total_price` que mandó la tienda virtual; si no coincide (y no hay "varios domicilios") se envía correo de alerta.
- `PedidoDAO.finalizarPedidoPOSPMTiendaVirtual` (línea ~1487): calcula bruto = total / 1,08, impuesto 8%, fija `idestadopedido`, `tiempopedido`, `descuento`, `programado`, `hora_programado`, e inserta `pedido_forma_pago`. Si la forma de pago es **virtual**, deja `enviadopixel = 2` (pendiente de pago).
- Forma de pago: `homologacion_forpag_pedidovirtual` (texto de la tienda virtual → `idforma_pago`). **`idFormaPago == 4` es Wompi** (hardcoded).
- Cualquier excepción: `pedido_tienda_virtual` pasa a `ERR` y se envía correo.

### 5.7 Pago con Wompi (forma de pago 4)

- Creación del link en el servidor: `PedidoCtrl.verificarEnvioLinkPagosParametrico` (línea ~4381). Monto en centavos, impuesto de consumo 8%, `single_use=false`, vence al día siguiente 23:00 UTC, `sku = idPedido`.
- Notificación al cliente: `notificarLinkPago` (línea ~1687): correo siempre; WhatsApp según canal y parámetros (directo por Ultramsg, o por Kommo / Brevo). El SMS está desactivado. Se registra en `pedido_pago_virtual`; el link se guarda en `pedido.idlink` y `pedido_idlink`.
- Webhook de pago: servlet `/ObtenerRespuestaWompi` → `PedidoCtrl.capturarEventoPagoWompi` (línea ~2228). Registra en `log_evento_wompi`. Con `transaction.updated` + `APPROVED` marca `pedido.fechapagovirtual` por `idlink` (`PedidoDAO.actualizarFechaPagoVirtual`, línea ~4372). Si el link no pertenece a ningún pedido, alerta por correo.
- **El pedido no se envía solo a la tienda al pagarse.** El comentario del código lo deja como "debería"; hoy lo hace una persona o un proceso automático (proyecto Servicios).
- Pantalla de apoyo: reenviar notificación de link, y **recrear link** (hoy lo crea el navegador usando la clave privada de Wompi descargada al navegador — riesgo).
- Variantes duplicadas: `verificarEnvioLinkPagos`, `verificarEnvioLinkPagosProcesoWompi`, y una versión en `PedidoMrTamalCtrl`.

### 5.8 Envío a la tienda (POS) y monitoreo

- Pantalla: `src/main/webapp/ConsultaPedidosNuevaTienda.html` + `js/consultapedidosnuevatienda.js`. Consulta por rango de máximo 3 días (`ConsultaIntegradaPedidosNuevaTienda`).
- Reenvío (`enviarPedidoTienda`, línea ~697 del JS): valida que la fecha del pedido sea hoy y que no esté ya enviado; muestra confirmación con cliente, dirección, total, tienda y tiempo; llama `FinalizarPedidoReenvio` (`PedidoCtrl`, línea ~794), que arma el pedido para el POS. **El navegador del asesor llama directo a la URL del POS de la tienda** (`FinalizarPedidoPixel`) y recibe el número de factura de la tienda; luego `ActualizarNumeroPedidoPixel` lo guarda en `pedido.numposheader` y `enviadopixel = 1`. Número -1 = la tienda no ha abierto el día de facturación.
- "Marcar como enviado" sin enviar: deja `numposheader = 0` (queda como enviado pero no llegó a la tienda).
- **Control de duplicados:** `capaDAOCC/EnvioTiendaDAO.java` (`tomarTurno`). Un solo `UPDATE` condicional; el turno vence al minuto. Existe porque el 2026-09-21 salieron pedidos duplicados en Envigado. Es obligatorio para 8 pantallas con reenvío y 3 procesos automáticos. "Ya está en la tienda" = `enviadopixel = 1` **y** `numposheader > 0`.
- Traducción al POS: `PedidoPOSPMDAO.InsertarPedidoPOSPM` (línea ~70) — por cada detalle genera líneas maestra, pizza, adición y modificador usando `homologacion_producto` (idproductoint, idtienda, idespecialidadint, idsabortipoliquidoint, idexcepcion → idproductoext). Mitad y mitad se parte en dos mitades de 0,5 con un producto separador (código interno 10000). Hay un comentario sobre `precio_pila` en la tienda.
- Cancelación: `CancelarPedido` → `PedidoDAO.cancelarPedido` (línea ~1000) solo cambia `idestadopedido = 4`. No avisa a la tienda ni a Gloria Food. No se encontró ningún mensaje de vuelta hacia Gloria Food: **la integración es solo de entrada**.
- Cuando se bloquea una tienda desde su pantalla, el navegador llama a `BloquearTienda` / `DesbloquearTienda` en la URL del POS (ver sección 8).

### 5.9 Marca Mr Tamal (franquicia)

`/TratarPedidoMrTamalKuno` → `PedidoMrTamalCtrl.tratarPedidoMrTamalKuno`. Usa el mismo formato JSON de Kuno, pero **no crea pedido** en el central: solo genera un link Wompi con la llave privada de cada tienda/franquicia (`tienda_franquicia_wompi`), lo envía por SMS y correo, y avisa por correo a la tienda al pagarse. Registra en `pedido_link_wompi`. Contiene **credenciales de correo escritas en el código** (ver sección 11; no se reproducen aquí).

---

## 6. Estados y códigos [Verificado en código]

**`pedido.idestadopedido`:** 1 = encabezado recién creado; 2 = finalizado; 4 = cancelado; 5 = programado. (Tabla `estado_pedido` con las descripciones.)

**`pedido.enviadopixel`:** 0 = no enviado; 2 = pendiente de pago virtual; 1 + `numposheader > 0` = enviado a la tienda; 1 + `numposheader = 0` = marcado enviado sin llegar a tienda.

**`pedido_tienda_virtual.estado`:** `PEN`, `PRO`, `ERR`.

**`tipo_pedido`:** 1 = domicilio (`delivery`), 2 = recoger (`pickup`).

**`pedido.origen`:** código de origen que viene de `homologacion_tienda_token.origen` (3 caracteres). **`usuariopedido`:** "TIENDA VIRTUAL" si `source` contiene "web", si no "APP" (Gloria Food también tiene app móvil — ver sección 12).

---

## 7. Modelo de datos del central relevante [Verificado en el esquema]

(Esquema exportado por el usuario; son ~300 tablas, la mayoría de otros módulos: CRM, domiciliarios, PQRS, bonos, ruleta...)

**Pedido y cliente:** `pedido`, `detalle_pedido`, `adicion_detalle_pedido`, `modificador_detalle_pedido`, `pedido_forma_pago`, `pedido_descuento`, `cliente`, `cliente_log`, `cliente_alerta`, `marcacion_pedido` (marca de plataforma/descuentos).

**Catálogo del central** (el usuario lo considera mal modelado):
- `producto` (idproducto, idreceta, nombre, tipo, preciogeneral, precio_puntos, preciooferta, incluye_liquido, idtipo_liquido, habilitado, controla_especialidades, producto_plataforma, manejacantidad, producto_asocia_adicion, impuesto, domicilio_tercerizado).
- `especialidad`, `especialidad_excepcion` (recargo por sabor y producto), `excepcion_especialidad`, `controla_especialidades`.
- `excepcion_precio` (promociones/combos): precio, precio_puntos, incluye_liquido, idtipoliquido, controla_cantidad_ingredientes, cantidad_ingredientes, controla_especialidades, partiradiciones, **habilitado, hora_inicial, hora_final, lunes..domingo**, oferta_abierta, maneja_oferta, id_producto_depende.
- `sabor_x_tipo_liquido`, `tipo_liquido`, `producto_incluido`, `producto_asesor`.

**Traducciones (puentes):**
- `homologacion_producto_pedidovirtual` (sku+tipo → idinterno) — Gloria Food, global.
- `homologacion_forpag_pedidovirtual`, `homologacion_tienda_pedidovirtual`, `homologacion_tienda_token`, `auth_kuno` (clave por tienda).
- `homologacion_producto` (producto central → producto del POS por tienda); `homologacion_forma_pago`.
- `homologacion_producto_rappi` y similares para otras plataformas.

**Tienda y operación:** `tienda` (nombre, dsn, url, pos, hosbd, funcional, grupo_virtual, maneja_zonas, latitud, longitud, cantidad_hornos, domicilio_tercerizado), `tiempo_pedido_tienda`, `horario_dia` (**sin idtienda**: horario global por día), `tienda_bloqueada`, `log_bloqueo_tienda`, `producto_no_existente`, `log_bloqueo_producto_tienda`, `rango_domicilio_tercerizado`.

**Ofertas, cupones, fidelización:** `oferta` (con `tiendas` como lista, `controla_hora`, `aplica_a`, `contact`, `monto_minimo`, `tope_descuento`), `oferta_cliente`, `descuentos_generales` (con `canal`, `T` = tienda virtual), `fidelizacion_*`, `cliente_fidelizacion`, `bono_*`, `campana_promo*`, `campana_15min_*`, `resultado_ruleta`.

**Pago:** `pedido_idlink`, `pedido_pago_virtual`, `pedido_pago_virtual_consolidado`, `log_evento_wompi`, `tienda_franquicia_wompi`, `pedido_link_wompi`.

**Logs de plataformas:** `log_pedido_virtual_kuno`, `log_pedido_virtual`, `log_pedido_rappi`, `log_pedido_didi`, `log_pedido_crmbot`.

---

## 8. Disponibilidad, bloqueos y horarios hoy [Verificado]

**Bloqueo de tienda**
- Tablas: `tienda_bloqueada` (idtienda, comentario, fecha_bloqueo), `log_bloqueo_tienda` (accion `BLOQUEO`/`DESBLOQUEO`, motivo, observacion, `debloqueo_en` = texto informativo, `aprobado`).
- Código: `TiendaBloqueadaDAO`, `TiendaBloqueadaCtrl`, servlet `CRUDTiendaBloqueada`, pantalla `TiendaBloqueada.html` + `js/tiendabloqueada.js`. Hay flujo de aprobación posterior: `AprobarBloqueoTienda`, `ConsultarBloqueosTienda`, `ConsultarBloqueosAprobados`.
- Al bloquear, el **navegador** llama a `<urlTienda>BloquearTienda` / `DesbloquearTienda` en el POS de la tienda (por eso las tiendas físicas se enteran).
- Efectos en el central: la cobertura (`UbicacionCtrl`, línea ~757) devuelve estado `BLOQUEADO`/`DISPONIBLE` (lo usa el bot); el tiempo de entrega pasa a 100 minutos.
- **No se encontró validación de tienda bloqueada en el flujo de pedidos de Gloria Food**: si la tienda está bloqueada en el central y no en Gloria Food, el pedido entra igual.
- "Desbloqueo en" no desbloquea automáticamente.

**Bloqueo de producto por tienda**
- `producto_no_existente (idtienda, idproducto)` + `log_bloqueo_producto_tienda`. Sin fecha, sin canal, sin motivo.
- Servlet `CRUDProductoNoExistente`, pantalla `MarcarProductoNoExistente.html`. Los listados de productos para tomar pedidos lo filtran (`ProductoDAO`, `PedidoDAO` para gaseosas y sabores).
- No hay conexión con el catálogo de Gloria Food.

**Horarios y días**
- Promociones (`excepcion_precio`): días y rango horario. **La regla se evalúa en el navegador** (`js/pedidos.js` ~1762 y `js/pedidosfinal.js` ~1323), no en el servidor.
- `horario_dia`: horario global por día, sin tienda. Los horarios por tienda que muestra la tienda virtual solo existen en Gloria Food.
- Otros mecanismos con horario propio: `oferta` (controla_hora), `campana_15min_horario_dia`, `campana_promo_dia`.

**Lo que NO existe hoy:** bloqueo por canal; bloqueo masivo; bloqueo programado con inicio/fin; fechas por producto; fuente única que lean todos los canales; validación en servidor al recibir un pedido.

### Requisito de diseño para la tienda nueva (módulo de disponibilidad)
- **Alcance:** una, varias o todas las tiendas; uno, varios o todos los canales.
- **Qué se bloquea:** tienda completa, producto, categoría, promoción u opción (ingrediente, tamaño, gaseosa).
- **Cuándo:** inmediato o programado (inicio y fin), días de la semana, franjas horarias, desbloqueo automático.
- **Control:** motivo, quién lo hizo, aprobación, log (se conserva el actual).
- **Fuente única** que leen tienda propia, contact center, bot y plataformas; validación **también en servidor**.
- **Efecto al cliente:** producto agotado o mensaje de tienda cerrada; manejo de demanda ampliando el tiempo en vez de bloquear.
- Se reutiliza la idea de "regla general con sobrescritura por tienda" (misma herencia del menú).

---

## 9. Domicilio [Verificado]

El domicilio es **un producto** ("Valor del domicilio", `producto.preciogeneral`), no un cargo separado. Dónde aparece en `PedidoCtrl.java`:
- Línea ~3616 (camino real de Kuno): línea `DELIVERY_FEE` del JSON; usa el precio que manda la tienda virtual.
- Línea ~4024: cuando un ítem trae el grupo "Envío (obligatorio)", fija **2.000** en el código (mecanismo viejo).
- Línea ~3446: camino anterior `ingresarPedidoTiendaVirtual`, fija **1.500**.
- Línea ~7421: bot/CRM, toma `preciogeneral` del producto.
- Líneas ~10430 y ~10962: plataformas, producto "Valor del domicilio plataforma".

[Capturas] En Bello la tienda virtual muestra pedido mínimo 17.000 y costo de envío 5.000. **[Pendiente]** De qué depende el valor real (tienda, zona, distancia) y cuál valor manda cuando difieren.

---

## 10. Experiencia actual de la tienda virtual [Capturas]

- Web: menú superior (Inicio, Quiénes somos, Ordena tu pizza, Promociones, Menú, Únete, Contacto), barra con horarios (Lunes–Jueves 4:00–10:00 p.m., Viernes 4:00–11:00 p.m., Sábado 12:00–11:00 p.m., Domingo 12:00–10:00 p.m.) y teléfonos (604) 444 4553 / 444 2525, banners de promociones.
- "Ordenar" abre un diálogo "Ingresa tu dirección" con mapa (Leaflet / OpenStreetMap), campo de dirección, botón Buscar, y "¿Es correcto?" con la tienda sugerida. Luego "MENU - BELLO" y botón ORDENAR.
- El menú se abre en una ventana con tres pestañas: **Menú, Información y Carrito**. Si la tienda está cerrada: "Estamos cerrados ahora pero puedes programar tu pedido".
- Secciones: PROMOCIÓN EXCLUSIVA ("Promoción de cantidad limitada solo disponible por canales digitales"), PRODUCTOS PREMIUM, PROMOCIONES (Insuperable Mediana 47.900, Grande 57.900, Extra Grande 67.900, Promo mediana MAX 21.990), CARTA MENÚ (pizzas desde 21.000, mitad y mitad, arma tu pizza...).
- **Producto con opciones** (Pizza Mitad y Mitad): Tamaño obligatorio (Pizzeta, Mediana +18.000, Grande +27.000, Extragrande +36.000), "Elige uno o dos sabor de tu pizza GD" (recargos por sabor, ej. Peperoni y Queso +1.750 en grande, +1.250 en mediana), bebida (ej. Sprite 1,5 L +6.000). Ejemplo: grande mitad y mitad 21.000 + 27.000 + 6.000 = 54.000.
- **Carrito:** Contacto, Tipo de pedido, Elegir hora disponible (Fijar hora), Método de pago, Comentarios, Cupón de descuento, botón "Confirmar pedido a domicilio". Avisa de políticas de Oracle eStore.
- **Información de la tienda:** mapa con polígono de cobertura, coste de envío (mínimo 17.000, costo 5.000), horario de atención y de entrega a domicilio por día, idiomas, métodos de pago (efectivo, tarjeta, transferencia bancaria — todos "entrega a domicilio"), dirección.
- **Panel de administración de Gloria Food (editor de menú):** categorías con imagen; productos con variantes de tamaño y **grupos de opciones reutilizables** que se acoplan a productos y tamaños (lista lateral "Opcionales y agregados": Elige uno o dos sabor de tu pizza, Producto Adicional Inseparable, Selecciona la especialidad 1/2, Elige 1 ingrediente Primera/Segunda Pizza, Adicionar a tu Mediana 1/2, Condimentos, Selecciona tu bebida Pet400...). El menú se configura por ubicación (11 ubicaciones, una por tienda, con seis iconos de "servicios").
- Lo que explica el código frágil: los sufijos PZ/MD/GD/XL existen porque cada tamaño tiene su propio grupo de sabores.

---

## 11. Hallazgos y riesgos

1. **Credenciales de correo escritas en el código** en `PedidoMrTamalCtrl` (dos métodos). Moverlas a configuración segura y **rotarlas**.
2. **`tienda_franquicia_wompi`** guarda llaves privadas de Wompi en texto en la base.
3. **`usuario.password` es `varchar(20)`**; probablemente sin cifrar [Deducido].
4. **El navegador usa la clave privada de Wompi** para recrear links (`consultapedidosnuevatienda.js`).
5. **Servicios sin validación propia** (muchos servlets: "Acceso: Sin validacion propia"); el de Kuno solo valida el header contra `auth_kuno`.
6. **La regla de horarios/días de promociones se valida solo en el navegador.**
7. **Cupones y tarifas fijas en el código** (cupones ignorados, 2.000 / 1.500 de domicilio, 8% de impuesto, `idFormaPago == 4` para Wompi).
8. **Correo personal fijo** como destinatario de alertas en varios puntos del flujo de pedidos.
9. **SQL armado por concatenación de texto** en los DAOs (inyección posible).
10. **Duplicación de lógica** (cuatro variantes de "crear link de pago", varios caminos de inserción de pedidos).
11. **El envío al POS depende del navegador del asesor**: si no alcanza la IP de la tienda, no se envía. Un cambio reciente ya eliminó llamadas bloqueantes a IPs locales en otra pantalla.
12. **Sin validación de tienda bloqueada** al recibir pedidos (sección 8).
13. **`/GetTiendasFuncionales`** devuelve la URL interna de cada tienda y se corta en silencio si `alertarpedidos` es nulo.
14. **Pedidos programados** hechos entre 0:00 y 10:00 piden revisión manual antes de enviarse (aviso en la pantalla).

---

## 12. Preguntas abiertas

**Catálogo y POS**
- Estructura real del menú del POS de la tienda (productos, tamaños, "con", "sin", combos, especialidades, precios, horarios). Necesario para diseñar el catálogo nuevo. **[Pendiente: esquema de una base de tienda con POS propio.]**
- ¿El precio oficial es el del central (`producto.preciogeneral`) o el de la tienda (`precio_pila`)? El usuario indica que la fuente buena es el POS.
- Cómo se identifica el "sin" (quitar ingrediente) en el POS y en el pedido.

**Reglas de negocio**
- Domicilio: ¿varía por tienda, zona, distancia o demanda? Pedido mínimo.
- Horarios **por tienda** (hoy `horario_dia` es global).
- Zonas de cobertura: ¿las de Gloria Food equivalen a las de ArcGIS del central?
- Pago en línea con Wompi dentro del checkout (en vez de link por correo/WhatsApp). ¿Qué métodos de pago irán? (Hoy: efectivo, tarjeta y transferencia a la entrega.)
- Pedidos programados: anticipación, franjas, reglas.
- Canales: Gloria Food tiene **web y app móvil** (el código distingue `source`); ¿se reemplazan ambos?
- Alcance de la v1: cuentas de usuario, puntos de fidelización ("Puntos Pizza"), cupones y ofertas, ruleta, campaña de 15 minutos.
- Qué debe recibir el cliente (confirmación, seguimiento, correos).
- Qué proceso hace hoy el reenvío automático (proyecto **Servicios**).

**Datos que faltan para completar**
- Filas de `homologacion_producto_pedidovirtual`, `homologacion_forpag_pedidovirtual`, `homologacion_tienda_token`, `excepcion_precio`, `especialidad_excepcion`, `descuentos_generales`, `tienda`.
- Dos o tres JSON reales de `log_pedido_virtual_kuno` (mitad y mitad, combo, pedido programado).
- De Gloria Food: pantalla de un producto con horarios por día; "Horarios y servicios"; "Tomar pedidos"; "Payment methods & taxes"; cómo se definen zonas y envío.

---

## 13. Cómo conectar a las bases de datos (pendiente)

- **Central (MySQL):** crear un usuario de **solo lectura** (idealmente sobre una réplica), con permisos solo sobre tablas de catálogo y operación (sin `cliente`, `usuario`, `integracion_crm`, `tienda_franquicia_wompi`). Guardar credenciales en variables de entorno del equipo (no en el chat ni en el repositorio). Consultar con Python 3 y `pymysql`.
- **POS de tienda:** el tipo de POS está en `tienda.pos` (1 = propio / MySQL; 2 = Pixel / SQL Anywhere vía DSN). Se prefiere una tienda con POS propio. Requiere usuario de solo lectura en el MySQL de esa tienda y que el PC de la tienda sea alcanzable desde la máquina que consulta (red interna o VPN, puerto 3306).
- Consulta usada para sacar el esquema (sirve para cualquier base):
  ```sql
  SELECT table_name, column_name, column_type, is_nullable, column_key, column_default
  FROM information_schema.columns
  WHERE table_schema = 'NOMBRE_BASE'
  ORDER BY table_name, ordinal_position;
  ```
- Lista de tiendas y tipo de POS:
  ```sql
  SELECT idtienda, nombre, pos, url, hosbd, dsn, funcional FROM tienda ORDER BY idtienda;
  ```

---

## 14. Skills propuestas

A crear cuando haya más información; cada una con secciones "cómo funciona hoy", "reglas de negocio", "datos/tablas", "mejoras para la tienda nueva" y marcas de verificado/pendiente.

1. Recepción del pedido (autenticación, log, anti-duplicados).
2. Estados del pedido y pedidos programados.
3. Cliente y dirección.
4. Catálogo y menú (POS como fuente; herencia base + tienda; combos y opciones).
5. Promociones, cupones y descuentos.
6. Cálculo y cierre del pedido (domicilio, impuestos, totales).
7. Pago con Wompi (links, webhook, conciliación).
8. Envío a tienda (POS), turnos y monitoreo.
9. Disponibilidad: bloqueos, horarios, canales y masivos.
10. (Posible) Experiencia del cliente: flujo web, cobertura, selección de tienda, programación.

Ubicación sugerida: nivel de usuario (`~/.claude/skills`), para usarlas desde el proyecto de la tienda nueva; opcionalmente una copia en `.claude/skills/` de este repositorio.

---

## 15. Instrucciones para quien continúe

1. Leer este documento completo antes de actuar.
2. Los archivos de código citados están en `src/main/java/...` y `src/main/webapp/...` de este repositorio (las líneas son aproximadas y pueden moverse).
3. **No leer ni copiar credenciales** del código. Si se necesitan, pedirlas al usuario por canal seguro.
4. Antes de escribir skills, completar los **pendientes** de la sección 12 (sobre todo el esquema del POS de una tienda).
5. Marcar siempre qué es verificado, qué es deducido y qué falta.
6. El usuario quiere resultados claros para un equipo de desarrollo; evitar suposiciones sin marcarlas.
7. Idioma de trabajo: español.
