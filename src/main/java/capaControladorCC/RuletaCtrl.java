package capaControladorCC;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaDAOCC.ClienteDAO;
import capaDAOCC.OfertaClienteDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.RuletaDAO;
import capaModeloCC.Cliente;
import capaModeloCC.Correo;
import capaModeloCC.OfertaCliente;
import utilidadesCC.ControladorEnvioCorreo;
import utilidadesCC.PlantillaCorreoPremio;

/**
 * Logica de la dispersion de premios de la ruleta de encuestas.
 *
 * Contexto: cuando un cliente contesta la encuesta de servicio juega en la ruleta
 * de la pagina web. Si gana, hasta hoy alguien tenia que buscarlo a mano en la
 * administracion de clientes, agregarle la oferta que corresponde al premio y
 * avisarle uno por uno. Esta clase hace eso por rango de fechas desde la pantalla
 * de dispersion.
 *
 * @author Pizza Americana
 */
public class RuletaCtrl {

	/** Estados que calcula el DAO. */
	private static final String PENDIENTE = "PENDIENTE";
	private static final String DISPERSADO = "DISPERSADO";
	private static final String ENTREGADO_A_MANO = "ENTREGADO A MANO";
	private static final String SIN_CORREO = "SIN CORREO";
	private static final String SIN_OFERTA = "SIN OFERTA";

	/**
	 * Tope de premios por tanda. La dispersion corre dentro de la peticion HTTP y
	 * cada correo lleva una pausa, asi que un represado de cientos dejaria al
	 * navegador esperando hasta que se corte la conexion. Con el tope se dispersa
	 * por tandas y la pantalla dice cuantos quedan.
	 */
	private static final int MAXIMO_POR_TANDA = 60;

	/**
	 * Pausa entre correos. No es lo que evita que los tomen por envio masivo -a
	 * seis ganadores diarios eso no es un riesgo-, sino que evita pegarle seguido
	 * al servidor de correo cuando se despacha un represado.
	 */
	private static final long PAUSA_ENVIO_MS = 800;

	/** Marca en usuario_ingreso de la oferta, para distinguirla de las manuales. */
	private static final String ORIGEN_RULETA = "RULETA";

	/**
	 * Premios de un rango de fechas, en dos vistas: el detalle fila por fila para
	 * la tabla y un resumen con los conteos por estado.
	 *
	 * Se devuelve todo junto en una sola llamada a proposito: si la pantalla
	 * pidiera resumen y detalle por separado, entre las dos llamadas alguien
	 * podria dispersar y los numeros de arriba no cuadrarian con la tabla.
	 *
	 * @param fechaDesde fecha inicial inclusive, formato aaaa-mm-dd
	 * @param fechaHasta fecha final inclusive, formato aaaa-mm-dd
	 * @return JSON con resultado, resumen y detalle
	 */
	@SuppressWarnings("unchecked")
	public String consultarPremios(final String fechaDesde, final String fechaHasta) {

		final JSONObject respuesta = new JSONObject();
		final JSONArray detalle = new JSONArray();

		int pendientes = 0;
		int dispersados = 0;
		int sinCorreo = 0;
		int sinOferta = 0;

		final List<JSONObject> premios = RuletaDAO.obtenerPremiosParaDispersion(fechaDesde, fechaHasta);
		for (final JSONObject premio : premios) {
			final String estado = texto(premio.get("estado"));
			if (PENDIENTE.equals(estado)) {
				pendientes++;
			} else if (DISPERSADO.equals(estado) || ENTREGADO_A_MANO.equals(estado)) {
				// Los entregados a mano se cuentan como dispersados: para el contador
				// lo que importa es que no estan pendientes. En la tabla si se
				// distinguen, con su propia etiqueta.
				dispersados++;
			} else if (SIN_CORREO.equals(estado)) {
				sinCorreo++;
			} else if (SIN_OFERTA.equals(estado)) {
				sinOferta++;
			}
			detalle.add(premio);
		}

		final JSONObject resumen = new JSONObject();
		resumen.put("total", premios.size());
		resumen.put("pendientes", pendientes);
		resumen.put("dispersados", dispersados);
		resumen.put("sin_correo", sinCorreo);
		resumen.put("sin_oferta", sinOferta);

		respuesta.put("resultado", "OK");
		respuesta.put("fechadesde", fechaDesde);
		respuesta.put("fechahasta", fechaHasta);
		respuesta.put("resumen", resumen);
		respuesta.put("detalle", detalle);

		return (respuesta.toJSONString());
	}

	/**
	 * Dispersa los premios pendientes de un rango: a cada ganador le asigna la
	 * oferta que corresponde a su premio, genera el codigo y le envia el correo.
	 *
	 * El orden de los pasos no es casual. Se marca la dispersion ANTES de enviar
	 * el correo, porque de los dos errores posibles el mas barato es que el
	 * cliente tenga un codigo del que no se enteró -queda visible en la pantalla
	 * con la columna Avisado vacia y alguien lo retoma- y el mas caro es que se le
	 * asignen dos ofertas con dos codigos por haber corrido la dispersion dos
	 * veces.
	 *
	 * @param fechaDesde fecha inicial inclusive, formato aaaa-mm-dd
	 * @param fechaHasta fecha final inclusive, formato aaaa-mm-dd
	 * @param usuario    quien ejecuta la dispersion
	 * @return JSON con los conteos y las novedades
	 */
	@SuppressWarnings("unchecked")
	public String dispersarPremios(final String fechaDesde, final String fechaHasta,
			final String usuario) {

		final JSONObject respuesta = new JSONObject();
		final JSONArray novedades = new JSONArray();

		int dispersados = 0;
		int correosEnviados = 0;
		int clientesCreados = 0;
		int conError = 0;
		int pendientesSinProcesar = 0;

		final PromocionesCtrl promoCtrl = new PromocionesCtrl();
		final List<JSONObject> premios = RuletaDAO.obtenerPremiosParaDispersion(fechaDesde, fechaHasta);

		for (final JSONObject premio : premios) {

			if (!PENDIENTE.equals(texto(premio.get("estado")))) {
				continue;
			}
			if (dispersados + conError >= MAXIMO_POR_TANDA) {
				pendientesSinProcesar++;
				continue;
			}

			final int idResultado = numero(premio.get("idresultado"));
			final String premioTitulo = texto(premio.get("premio"));

			try {
				// 1. A que cliente se le asigna la oferta.
				int idCliente = numero(premio.get("idcliente"));
				if (idCliente == 0) {
					idCliente = RuletaDAO.buscarClientePorTelefono(texto(premio.get("telefono")));
				}
				if (idCliente == 0) {
					idCliente = crearCliente(premio);
					if (idCliente > 0) {
						clientesCreados++;
					}
				}
				if (idCliente == 0) {
					novedades.add("Pedido " + texto(premio.get("idpedido")) + ": no se pudo "
							+ "identificar ni crear el cliente.");
					conError++;
					continue;
				}

				// 2. Asignar la oferta. Se reusa insertarOfertaCliente para que el codigo,
				// el saldo y la caducidad salgan del mismo lugar que en la pantalla manual.
				// Mismos argumentos que usa CRUDOfertaCliente, que es la pantalla manual.
				final String observacion = recortar("Premio ruleta encuesta: " + premioTitulo
						+ " - pedido " + texto(premio.get("idpedido"))
						+ " tienda " + texto(premio.get("tienda")), 200);
				final OfertaCliente oferta = new OfertaCliente(0, numero(premio.get("idoferta")),
						idCliente, "", 0, "", "", observacion, ORIGEN_RULETA);

				final int idOfertaCliente = extraerIdOfertaCliente(
						promoCtrl.insertarOfertaCliente(oferta));
				if (idOfertaCliente == 0) {
					novedades.add("Pedido " + texto(premio.get("idpedido"))
							+ ": no se pudo asignar la oferta.");
					conError++;
					continue;
				}

				// 3. Marcar. Si otra dispersion se adelanto, aqui se detecta y no se
				// envia el correo: el UPDATE lleva la condicion idofertacliente = 0.
				if (!RuletaDAO.marcarDispersion(idResultado, idOfertaCliente, usuario)) {
					novedades.add("Pedido " + texto(premio.get("idpedido"))
							+ ": ya habia sido dispersado por otra ejecucion.");
					conError++;
					continue;
				}
				dispersados++;

				// 4. Avisar al cliente con el codigo que quedo generado.
				final OfertaCliente asignada = OfertaClienteDAO.retornarOfertaCliente(idOfertaCliente);
				final String codigo = (asignada == null) ? "" : texto(asignada.getCodigoPromocion());
				final String vence = (asignada == null) ? "" : texto(asignada.getFechaCaducidad());

				if (codigo.length() == 0) {
					novedades.add("Pedido " + texto(premio.get("idpedido")) + ": la oferta quedo "
							+ "asignada pero sin codigo promocional, no se envio correo.");
					conError++;
					continue;
				}

				if (enviarCorreo(texto(premio.get("correo")), texto(premio.get("nombre_cliente")),
						premioTitulo, codigo, vence)) {
					OfertaClienteDAO.actualizarMensajeOferta(idOfertaCliente);
					correosEnviados++;
				} else {
					novedades.add("Pedido " + texto(premio.get("idpedido")) + ": oferta asignada "
							+ "con codigo " + codigo + " pero el correo no salio.");
					conError++;
				}

				pausar();

			} catch (final Exception e) {
				System.out.println("Error dispersando el resultado " + idResultado + ": " + e.toString());
				novedades.add("Pedido " + texto(premio.get("idpedido")) + ": " + e.getMessage());
				conError++;
			}
		}

		respuesta.put("resultado", "OK");
		respuesta.put("dispersados", dispersados);
		respuesta.put("correos_enviados", correosEnviados);
		respuesta.put("clientes_creados", clientesCreados);
		respuesta.put("con_error", conError);
		respuesta.put("pendientes_sin_procesar", pendientesSinProcesar);
		respuesta.put("detalle_errores", novedades);

		return (respuesta.toJSONString());
	}

	/**
	 * Crea el cliente con lo poco que dejo el formulario de la ruleta: nombre,
	 * telefono y correo.
	 *
	 * Los demas campos van con los mismos valores que usa la web cuando crea un
	 * cliente desde el pedido virtual (idmunicipio 0, idnomenclatura 12,
	 * politica_datos N, direccion y zona vacias). No se inventan valores: es el
	 * patron que ya tienen cientos de clientes recientes de la tabla.
	 *
	 * @param premio fila del premio con los datos del ganador
	 * @return el idcliente creado, o 0 si no se pudo
	 */
	private int crearCliente(final JSONObject premio) {

		final String telefono = texto(premio.get("telefono"));
		// El telefono es obligatorio en la tabla y es lo unico que despues permite
		// volver a encontrar a este cliente. Sin el no vale la pena crear el registro.
		if (telefono.length() < 7) {
			return (0);
		}

		final String nombreCompleto = texto(premio.get("nombre_cliente"));
		final int espacio = nombreCompleto.indexOf(' ');

		final Cliente cliente = new Cliente();
		cliente.setIdtienda(numero(premio.get("idtienda")));
		cliente.setNombres(recortar(espacio > 0 ? nombreCompleto.substring(0, espacio) : nombreCompleto, 50));
		cliente.setApellidos(recortar(espacio > 0 ? nombreCompleto.substring(espacio + 1) : "", 50));
		cliente.setNombreCompania("");
		cliente.setDireccion("");
		cliente.setZonaDireccion("");
		cliente.setTelefono(recortar(telefono, 50));
		cliente.setTelefonoCelular(recortar(telefono, 50));
		cliente.setEmail(recortar(texto(premio.get("correo")), 50));
		cliente.setObservacion(recortar("Creado por dispersion de premios de ruleta, pedido "
				+ texto(premio.get("idpedido")), 200));
		cliente.setIdMunicipio(0);
		cliente.setIdnomenclatura(12);
		cliente.setPoliticaDatos("N");
		cliente.setLatitud(0);
		cliente.setLontitud(0);
		cliente.setDistanciaTienda(0);
		cliente.setNumNomenclatura("");
		cliente.setMemcode(0);
		cliente.setZonaTienda("");
		cliente.setFechaNacimiento("");
		cliente.setClienteSinIden("");
		cliente.setEmailFacturacion("");
		cliente.setIdTipoPersona(0);
		cliente.setIdentificacion("");

		try {
			return (ClienteDAO.insertarCliente(cliente));
		} catch (final Exception e) {
			System.out.println("Error creando cliente para premio de ruleta: " + e.toString());
			return (0);
		}
	}

	/**
	 * Envia el correo del premio.
	 *
	 * Sale por enviarCorreo(), que es la ruta de Gmail: el correo corporativo esta
	 * en Google Workspace y la clave del parametro es una clave de aplicacion de
	 * Gmail. NO se usa enviarCorreoInstitucional(), que apunta al servidor
	 * mail.pizzaamericana.co.
	 *
	 * La cuenta sale de parametros y no del codigo: se usa CUENTACORREOWOMPI, que
	 * apunta a tecnologia@pizzaamericana.com.co y es la misma con la que ya salen
	 * los bonos regalo a clientes. Antes se consulta la pareja
	 * CUENTACORREOPREMIOS / CLAVECORREOPREMIOS, que no existe todavia: el dia que
	 * quieran un remitente mas presentable para el cliente basta con crear esos
	 * dos parametros, sin volver a desplegar.
	 *
	 * @param correo        destinatario
	 * @param nombreCliente nombre para el saludo
	 * @param premio        premio como lo vio en la ruleta
	 * @param codigo        codigo promocional
	 * @param vence         fecha de vencimiento aaaa-mm-dd
	 * @return true si el envio no reporto error
	 */
	@SuppressWarnings("unchecked")
	private boolean enviarCorreo(final String correo, final String nombreCliente,
			final String premio, final String codigo, final String vence) {

		if (!RuletaDAO.correoValido(correo)) {
			return (false);
		}

		try {
			String cuenta = texto(ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOPREMIOS"));
			String clave = texto(ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOPREMIOS"));
			if (cuenta.length() == 0 || clave.length() == 0) {
				cuenta = texto(ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI"));
				clave = texto(ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI"));
			}
			if (cuenta.length() == 0 || clave.length() == 0) {
				System.out.println("Dispersion de premios: no hay cuenta de correo configurada.");
				return (false);
			}

			// URL publica del logo. Va por parametro y no quemada porque el logo se
			// aloja en el sitio publico, no en el central: el cliente no alcanza la
			// IP interna. Si el parametro no existe el correo sale igual, con el
			// nombre en texto sobre la franja azul.
			final String urlLogo = texto(ParametrosDAO.retornarValorAlfanumerico("IMAGENLOGOCORREO"));

			final Correo mensaje = new Correo();
			mensaje.setUsuarioCorreo(cuenta);
			mensaje.setContrasena(clave);
			mensaje.setAsunto(PlantillaCorreoPremio.asunto());
			mensaje.setMensaje(PlantillaCorreoPremio.cuerpo(nombreCliente, premio, codigo,
					vence, urlLogo));

			final ArrayList<String> destinatarios = new ArrayList<String>();
			destinatarios.add(correo);

			return (new ControladorEnvioCorreo(mensaje, destinatarios).enviarCorreo());

		} catch (final Exception e) {
			System.out.println("Error enviando correo de premio a " + correo + ": " + e.toString());
			return (false);
		}
	}

	/**
	 * Saca el idofertacliente de la respuesta de insertarOfertaCliente, que llega
	 * como [{"idofertacliente":N}].
	 *
	 * @param json respuesta del controlador de promociones
	 * @return el id, o 0 si no vino
	 */
	private int extraerIdOfertaCliente(final String json) {
		try {
			final Object parseado = new JSONParser().parse(json);
			if (parseado instanceof JSONArray) {
				final JSONArray arreglo = (JSONArray) parseado;
				if (!arreglo.isEmpty()) {
					return (numero(((JSONObject) arreglo.get(0)).get("idofertacliente")));
				}
			}
		} catch (final Exception e) {
			System.out.println("No se pudo leer el idofertacliente de: " + json);
		}
		return (0);
	}

	/**
	 * Pausa entre envios. Si el hilo es interrumpido se sigue de largo: una pausa
	 * que no se cumplio no es razon para abortar una dispersion en curso.
	 */
	private void pausar() {
		try {
			Thread.sleep(PAUSA_ENVIO_MS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Convierte a texto sin devolver null nunca.
	 *
	 * @param valor valor crudo
	 * @return el texto, nunca null
	 */
	private String texto(final Object valor) {
		return (valor == null ? "" : String.valueOf(valor).trim());
	}

	/**
	 * Convierte a entero sin lanzar excepcion.
	 *
	 * @param valor valor crudo
	 * @return el entero, o 0
	 */
	private int numero(final Object valor) {
		if (valor == null) {
			return (0);
		}
		if (valor instanceof Number) {
			return (((Number) valor).intValue());
		}
		try {
			return (Integer.parseInt(String.valueOf(valor).trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	/**
	 * Recorta al largo de la columna. Los nombres los escribe el cliente en el
	 * formulario y pueden venir mas largos que el campo, lo que abortaria el
	 * insert completo por un dato secundario.
	 *
	 * @param texto texto a recortar
	 * @param largo largo maximo
	 * @return el texto recortado
	 */
	private String recortar(final String texto, final int largo) {
		if (texto == null) {
			return ("");
		}
		final String t = texto.trim();
		return (t.length() <= largo ? t : t.substring(0, largo));
	}
}
