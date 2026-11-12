package capaControladorCC.motor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import capaControladorCC.motor.CatalogoPos.DescuentoGeneral;
import capaControladorCC.motor.CatalogoPos.Horario;
import capaControladorCC.motor.CatalogoPos.Incluido;
import capaControladorCC.motor.CatalogoPos.Pregunta;
import capaControladorCC.motor.CatalogoPos.Producto;
import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;

/**
 * Motor de pedido, version 1: NUCLEO DE LINEAS, PRECIO, HORARIOS Y DESCUENTOS (paquetes 2.2, 2.4 y 2.5 de la etapa 2).
 *
 * Reproduce lo que hace la pantalla de toma de pedidos de la tienda al agregar un producto:
 *  1. Linea maestra con el producto a precio1.
 *  2. Productos incluidos del producto principal (tabla producto_incluido), cada uno con la columna de
 *     precio que indique esa tabla.
 *  3. Por cada opcion elegida: una linea hija ligada a la maestra, cuyo precio es la columna que indica la
 *     pregunta forzada donde esta la opcion (eleccion_forzada.precio) tomada del producto de la opcion, y
 *     los productos incluidos de la opcion, con cantidad multiplicada por la de la opcion.
 *  4. Modificadores CON / SIN: linea con cantidad indicada y precio1, ligada a la maestra y a la linea que
 *     modifican.
 *  5. Si la linea se pide con cantidad mayor a 1 se multiplican las cantidades de la maestra y de las hijas.
 *  6. Domicilio: producto indicado en la solicitud, al precio1 del catalogo.
 *  7. Validacion de horarios por dia y canal (producto_horario y producto_horario_canal).
 *  8. Validacion de dependencias entre productos (id_producto_depende).
 *  9. Validacion y calculo de descuentos generales (automaticos diarios tipo P e I, cupon tipo D), codigos promocionales y puntos.
 *
 * No toca bases de datos: trabaja sobre la copia en memoria del catalogo.
 */
public class MotorPedidoV1 implements MotorPedido {

	public static final String VERSION = "v1-nucleo";

	private final CatalogoPos catalogo;
	private final String version;

	public MotorPedidoV1(CatalogoPos catalogo) {
		this(catalogo, VERSION);
	}

	public MotorPedidoV1(CatalogoPos catalogo, String version) {
		this.catalogo = catalogo;
		this.version = (version == null || version.isEmpty()) ? VERSION : version;
	}

	@Override
	public String version() {
		return version;
	}

	private int ref;

	@Override
	public synchronized MotorResultado cotizar(MotorSolicitud s) {
		MotorResultado r = new MotorResultado();
		r.versionMotor = version;
		try {
			ref = 1;
			if (s == null || s.lineas == null) {
				return MotorResultado.conEstado(MotorResultado.ERROR, VERSION, "SOLICITUD_VACIA", "No llegaron lineas.");
			}

			// 1. Resolver fecha, dia de la semana y hora para validacion de horarios y descuentos
			Calendar cal = Calendar.getInstance();
			String horaHHmm = "";
			int diaCalendar = cal.get(Calendar.DAY_OF_WEEK);
			if (s.contexto != null && s.contexto.fechaHora != null && !s.contexto.fechaHora.trim().isEmpty()) {
				try {
					String fh = s.contexto.fechaHora.trim();
					SimpleDateFormat sdf = new SimpleDateFormat(
							fh.length() > 16 ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd HH:mm");
					Date d = sdf.parse(fh);
					cal.setTime(d);
					diaCalendar = cal.get(Calendar.DAY_OF_WEEK);
					horaHHmm = new SimpleDateFormat("HH:mm").format(d);
				} catch (Exception ignored) {
					horaHHmm = new SimpleDateFormat("HH:mm").format(cal.getTime());
				}
			} else {
				horaHHmm = new SimpleDateFormat("HH:mm").format(cal.getTime());
			}

			// 2. Recopilar todos los ids de producto pedidos para validar dependencias
			Set<Integer> productosPedidos = new HashSet<Integer>();
			for (MotorSolicitud.Linea l : s.lineas) {
				productosPedidos.add(l.idproducto);
				if (l.selecciones != null) {
					for (MotorSolicitud.Seleccion sel : l.selecciones) {
						productosPedidos.add(sel.idproductoOpcion);
					}
				}
			}

			// 3. Procesar lineas solicitadas
			String canal = (s.contexto != null && s.contexto.canal != null) ? s.contexto.canal.trim() : "";
			for (MotorSolicitud.Linea l : s.lineas) {
				Producto p = catalogo.productos.get(l.idproducto);
				if (p == null) {
					return MotorResultado.conEstado(MotorResultado.RECHAZADO, VERSION, "PRODUCTO_DESCONOCIDO",
							"El producto " + l.idproducto + " no existe en el catalogo del POS.");
				}
				if (p.noVenta != 0) {
					r.advertencias.add(new MotorResultado.Advertencia("NO_VENTA", "El producto esta marcado como no venta.", p.id));
				}

				// Validar horario por dia y canal
				Horario h = catalogo.obtenerHorario(canal, p.id);
				if (h != null && !h.estaActivo(diaCalendar, horaHHmm)) {
					r.advertencias.add(new MotorResultado.Advertencia("FUERA_DE_HORARIO",
							"El producto " + p.id + " (" + p.descripcion + ") no esta en horario para el canal "
									+ (canal.isEmpty() ? "GENERAL" : canal) + " a las " + horaHHmm + ".", p.id));
				}

				// Validar dependencia entre productos
				if (p.idProductoDepende > 0 && !productosPedidos.contains(p.idProductoDepende)) {
					r.advertencias.add(new MotorResultado.Advertencia("DEPENDE_DE_PRODUCTO",
							"El producto " + p.id + " (" + p.descripcion + ") requiere que el pedido incluya el producto "
									+ p.idProductoDepende + ".", p.id));
				}

				// Validar puntos
				if (p.precioPuntos > 0) {
					r.advertencias.add(new MotorResultado.Advertencia("REQUIERE_PUNTOS",
							"El producto " + p.id + " requiere " + (int) p.precioPuntos + " puntos de fidelizacion.", p.id));
				}

				agregarLinea(r, p, l);
			}

			// 4. Domicilio
			if (s.domicilio != null && s.domicilio.idproducto > 0) {
				Producto d = catalogo.productos.get(s.domicilio.idproducto);
				if (d == null) {
					return MotorResultado.conEstado(MotorResultado.RECHAZADO, VERSION, "DOMICILIO_DESCONOCIDO",
							"El producto de domicilio " + s.domicilio.idproducto + " no existe.");
				}
				double precio = d.precio("precio1");
				if (Math.abs(precio - s.domicilio.valor) > 0.5) {
					r.advertencias.add(new MotorResultado.Advertencia("DOMICILIO_DISTINTO",
							"El catalogo cobra " + precio + " y la solicitud esperaba " + s.domicilio.valor, d.id));
				}
				r.lineas.add(linea(d.id, 1, precio, 0, 0, "DOMICILIO"));
			}

			// 5. Aplicar descuentos y promociones
			aplicarDescuentos(s, r, horaHHmm);

			r.estado = MotorResultado.OK;
			r.recalcularTotales();
			return r;
		} catch (Throwable t) {
			return MotorResultado.conEstado(MotorResultado.ERROR, VERSION, "EXCEPCION", String.valueOf(t));
		}
	}

	private void aplicarDescuentos(MotorSolicitud s, MotorResultado r, String horaHHmm) {
		double totalDescuento = 0.0;

		// Advertencias de solicitud de descuento explicita y descuento manual
		if (s.descuentos != null) {
			if (s.descuentos.codigoPromocional != null && !s.descuentos.codigoPromocional.trim().isEmpty()) {
				r.advertencias.add(new MotorResultado.Advertencia("CODIGO_PROMO_RECIBIDO",
						"Codigo promocional recibido: " + s.descuentos.codigoPromocional.trim(), 0));
			}
			if (s.descuentos.redimirPuntos) {
				r.advertencias.add(new MotorResultado.Advertencia("REDENCION_PUNTOS_ACTIVA",
						"Solicitud de redencion de puntos activa.", 0));
			}
			if (s.descuentos.descuentoManual > 0) {
				totalDescuento += s.descuentos.descuentoManual;
				String mot = (s.descuentos.motivoDescuento != null && !s.descuentos.motivoDescuento.trim().isEmpty())
						? s.descuentos.motivoDescuento.trim() : "DESCUENTO MANUAL";
				r.advertencias.add(new MotorResultado.Advertencia("DESCUENTO_MANUAL",
						"Descuento manual aplicado: $" + (int) s.descuentos.descuentoManual + " (" + mot + ")", 0));
			}
		}

		// Descuentos generales configurados en pos_central
		if (catalogo.descuentosGenerales != null && !catalogo.descuentosGenerales.isEmpty()) {
			for (DescuentoGeneral dg : catalogo.descuentosGenerales) {
				if (!dg.enHorario(horaHHmm)) {
					continue;
				}
				if ("P".equalsIgnoreCase(dg.tipoDescuento) || "N".equalsIgnoreCase(dg.tipoDescuento)) {
					for (MotorResultado.Linea ml : r.lineas) {
						if (ml.refMaster == 0 && ml.idproducto == dg.idProducto) {
							double base = ml.valorTotal + totalHijas(r, ml.ref);
							double desc = 0.0;
							if (dg.valorPesos > 0) {
								desc = dg.valorPesos * ml.cantidad;
							} else if (dg.valorPorcentaje > 0) {
								desc = base * (dg.valorPorcentaje / 100.0);
							}
							if (desc > 0) {
								totalDescuento += desc;
								r.advertencias.add(new MotorResultado.Advertencia("DESCUENTO_APLICADO",
										"Descuento tipo " + dg.tipoDescuento + " de " + desc + " en producto " + ml.idproducto, ml.idproducto));
							}
						}
					}
				} else if ("I".equalsIgnoreCase(dg.tipoDescuento)) {
					for (MotorResultado.Linea ml : r.lineas) {
						if (ml.refMaster == 0 && ml.idproducto == dg.idProducto && dg.valorPesos > 0) {
							double totalAdi = totalAdiciones(r, ml.ref);
							double desc = (totalAdi > dg.valorPesos) ? dg.valorPesos : totalAdi;
							if (desc > 0) {
								totalDescuento += desc;
								r.advertencias.add(new MotorResultado.Advertencia("DESCUENTO_ADICION",
										"Descuento en adiciones de " + desc + " para producto " + ml.idproducto, ml.idproducto));
							}
						}
					}
				} else if ("D".equalsIgnoreCase(dg.tipoDescuento)) {
					for (MotorResultado.Linea ml : r.lineas) {
						if (ml.refMaster == 0 && ml.idproducto == dg.idProducto) {
							double base = ml.valorTotal + totalHijas(r, ml.ref);
							double valorCupon = base * (dg.valorPorcentaje / 100.0);
							r.advertencias.add(new MotorResultado.Advertencia("CUPON_FUTURO_D",
									"Genera cupon futuro de " + (int) valorCupon + " (" + dg.valorPorcentaje + "%) para producto "
											+ dg.idProducto, dg.idProducto));
						}
					}
				}
			}
		}
		r.descuento = totalDescuento;
	}

	private double totalHijas(MotorResultado r, int refMaster) {
		double suma = 0;
		for (MotorResultado.Linea h : r.lineas) {
			if (h.refMaster == refMaster) {
				suma += h.valorTotal;
			}
		}
		return suma;
	}

	private double totalAdiciones(MotorResultado r, int refMaster) {
		double suma = 0;
		for (MotorResultado.Linea h : r.lineas) {
			if (h.refMaster == refMaster && "OPCION".equals(h.origen)) {
				Producto hp = catalogo.productos.get(h.idproducto);
				if (hp != null && ("ADICION".equalsIgnoreCase(hp.tipo) || "ADICIONALES".equalsIgnoreCase(hp.tipo))) {
					suma += h.valorTotal;
				}
			}
		}
		return suma;
	}

	private MotorResultado.Linea linea(int idProducto, double cantidad, double unitario, int refMaster,
			int refModificador, String origen) {
		MotorResultado.Linea x = new MotorResultado.Linea();
		x.ref = ref++;
		x.idproducto = idProducto;
		Producto prod = catalogo != null && catalogo.productos != null ? catalogo.productos.get(idProducto) : null;
		if (prod != null && prod.descripcion != null) {
			x.descripcion = prod.descripcion;
		}
		x.cantidad = cantidad;
		x.valorUnitario = unitario;
		x.valorTotal = cantidad * unitario;
		x.refMaster = refMaster;
		x.refModificador = refModificador;
		x.origen = origen;
		return x;
	}

	private void agregarLinea(MotorResultado r, Producto p, MotorSolicitud.Linea l) {
		double q = l.cantidad <= 0 ? 1 : l.cantidad;
		MotorResultado.Linea maestra = linea(p.id, q, p.precio("precio1"), 0, 0, "SOLICITADA");
		r.lineas.add(maestra);
		// productos incluidos del producto principal
		agregarIncluidos(r, p.id, q, maestra.ref);
		// opciones elegidas
		Map<Integer, Integer> contadorPorPregunta = new HashMap<Integer, Integer>();
		Map<Integer, MotorResultado.Linea> lineaDeProducto = new HashMap<Integer, MotorResultado.Linea>();
		lineaDeProducto.put(p.id, maestra);
		for (MotorSolicitud.Seleccion sel : l.selecciones) {
			Producto o = catalogo.productos.get(sel.idproductoOpcion);
			if (o == null) {
				r.advertencias.add(new MotorResultado.Advertencia("OPCION_DESCONOCIDA",
						"La opcion " + sel.idproductoOpcion + " no existe en el catalogo.", sel.idproductoOpcion));
				continue;
			}
			int idPregunta = sel.idpregunta > 0 ? sel.idpregunta : preguntaDeOpcion(p, o.id);
			String columna = "precio1";
			if (idPregunta > 0) {
				Pregunta pq = catalogo.preguntas.get(idPregunta);
				if (pq != null && pq.opciones.containsKey(o.id)) {
					columna = pq.opciones.get(o.id);
				}
				contadorPorPregunta.merge(idPregunta, 1, Integer::sum);
			} else {
				r.advertencias.add(new MotorResultado.Advertencia("OPCION_FUERA_DE_PREGUNTA",
						"La opcion no esta en ninguna pregunta del producto " + p.id + "; se cobra precio1.", o.id));
			}
			double cant = sel.cantidad * q;
			MotorResultado.Linea hija = linea(o.id, cant, o.precio(columna), maestra.ref, 0, "OPCION");
			r.lineas.add(hija);
			if (!lineaDeProducto.containsKey(o.id)) {
				lineaDeProducto.put(o.id, hija);
			}
			agregarIncluidos(r, o.id, cant, maestra.ref);
		}
		// modificadores
		for (MotorSolicitud.Modificador m : l.modificadores) {
			Producto mp = catalogo.productos.get(m.idproducto);
			if (mp == null) {
				r.advertencias.add(new MotorResultado.Advertencia("MODIFICADOR_DESCONOCIDO",
						"El modificador " + m.idproducto + " no existe en el catalogo.", m.idproducto));
				continue;
			}
			MotorResultado.Linea sobre = lineaDeProducto.get(m.sobreProducto);
			int refSobre = sobre != null ? sobre.ref : maestra.ref;
			r.lineas.add(linea(mp.id, m.cantidad * q, mp.precio("precio1"), maestra.ref, refSobre, "MODIFICADOR"));
		}
		// Minimo de elecciones: asi lo exige la pantalla de la tienda. OJO: "obliga_eleccion" por si solo NO
		// obliga a nada; la pantalla solo bloquea cuando el minimo es mayor que lo elegido. Con minimo 0 (el
		// caso de casi todas las preguntas de bebida y de pizza) se puede dejar sin responder. Si la pregunta
		// permite dividir, el minimo se exige en cada mitad. En esta version solo se advierte.
		for (int idPregunta : p.preguntas) {
			if (idPregunta <= 0) {
				continue;
			}
			Pregunta pq = catalogo.preguntas.get(idPregunta);
			if (pq == null || pq.obliga == 0 || pq.minimo <= 0) {
				continue;
			}
			int necesarias = pq.minimo * (pq.permiteDividir >= 2 ? pq.permiteDividir : 1);
			int elegidas = contadorPorPregunta.containsKey(idPregunta) ? contadorPorPregunta.get(idPregunta) : 0;
			if (elegidas < necesarias) {
				r.advertencias.add(new MotorResultado.Advertencia("PREGUNTA_INCOMPLETA",
						"La pregunta " + idPregunta + " (" + pq.titulo + ") pide minimo " + necesarias
								+ " eleccion(es) y trae " + elegidas + ".", p.id));
			}
		}
	}

	/** Primera pregunta del producto cuyas opciones incluyen el producto dado; 0 si ninguna. */
	private int preguntaDeOpcion(Producto p, int idOpcion) {
		for (int idPregunta : p.preguntas) {
			if (idPregunta <= 0) {
				continue;
			}
			Pregunta pq = catalogo.preguntas.get(idPregunta);
			if (pq != null && pq.opciones.containsKey(idOpcion)) {
				return idPregunta;
			}
		}
		return 0;
	}

	private void agregarIncluidos(MotorResultado r, int idVendido, double cantidadVendida, int refMaster) {
		List<Incluido> lista = catalogo.incluidos.get(idVendido);
		if (lista == null) {
			return;
		}
		for (Incluido inc : lista) {
			Producto ip = catalogo.productos.get(inc.idProductoQueSeAgrega);
			if (ip == null) {
				continue;
			}
			r.lineas.add(linea(ip.id, inc.cantidad * cantidadVendida, ip.precio(inc.columnaPrecio), refMaster, 0, "INCLUIDO"));
		}
	}
}
