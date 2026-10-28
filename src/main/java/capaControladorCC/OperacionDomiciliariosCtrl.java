package capaControladorCC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.OperacionDomiciliariosDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * Arma el reporte de operacion de domiciliarios diaria: que domiciliarios
 * trabajaron en una tienda un dia, cuantos pedidos llevaron y como les fue.
 *
 * Las definiciones -que es el tiempo en ruta, que es el regreso, que se juzga y
 * que no, quien es externo- estan en capaDAOCC.OperacionDomiciliariosDAO y se
 * resumen en la nota de la pantalla. Aqui solo se suma.
 *
 * INTERNOS Y EXTERNOS
 *
 * Interno es el domiciliario de la nomina de Pizza Americana; externo es el
 * temporal que manda una empresa (RED, Domiciliarios Unidos, MESSENGER) y entra
 * con un cupo "Domiciliario NN". Todo lo que importa se parte en los dos: en
 * especial los pedidos que NO cumplieron la promesa, que es lo que se quiere
 * poder atribuir a unos u otros. Un pedido tarde se cuenta UNA sola vez, en el
 * grupo de quien lo llevo.
 *
 * LO QUE NO SE PROMEDIA
 *
 * El regreso viene sucio -sin marcar, de dias, negativo-. Solo entran al
 * promedio los regresos validos (estado OK) y los demas se cuentan aparte,
 * igual que en el desempeno de un domiciliario, para que las dos pantallas no
 * den distinto. Lo mismo con el tiempo contra la promesa: un pedido programado
 * o sin promesa se entrega pero no se juzga.
 */
public class OperacionDomiciliariosCtrl {

	/** El dia operativo arranca hacia las 10 de la manana y termina de madrugada. */
	private static final int[] ORDEN_HORAS = { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4,
			5, 6, 7, 8, 9 };

	/** Lo que se acumula por domiciliario mientras se recorren las salidas. */
	private static class Acumulado {
		int id;
		String nombre = "";
		boolean externo;
		String empresa = "";
		String ingreso = "";
		String salidaTurno = "";
		int minutosTurno = -1;
		int salidas;
		int pedidos;
		int entregados;
		int medibles;
		int aTiempo;
		long sumaRuta;
		int conRuta;
		long sumaTotal;
		long sumaRegreso;
		int regresosValidos;
		int regresosDemorados;
		int regresosSinDato;
		String primeraSalida = "";
		String ultimaSalida = "";
	}

	/** Las mismas cifras de todo el dia, pero de un solo grupo: los internos o los externos. */
	private static class Grupo {
		int domiciliarios;
		int conPedidos;
		int salidas;
		int pedidos;
		int medibles;
		int aTiempo;
		long sumaRuta;
		int conRuta;
		long sumaRegreso;
		int regresosValidos;
		int regresosDemorados;
		int regresosSinDato;

		JSONObject aJSON() {
			final JSONObject o = new JSONObject();
			o.put("domiciliarios", domiciliarios);
			o.put("con_pedidos", conPedidos);
			o.put("salidas", salidas);
			o.put("pedidos", pedidos);
			o.put("medibles", medibles);
			o.put("a_tiempo", aTiempo);
			o.put("demorados", medibles - aTiempo);
			o.put("porcentaje", medibles > 0 ? Double.valueOf(redondear(aTiempo * 100.0 / medibles)) : null);
			o.put("prom_ruta", conRuta > 0 ? Double.valueOf(redondear(sumaRuta / (double) conRuta)) : null);
			o.put("prom_regreso",
					regresosValidos > 0 ? Double.valueOf(redondear(sumaRegreso / (double) regresosValidos)) : null);
			o.put("regresos_demorados", regresosDemorados);
			o.put("regresos_sin_dato", regresosSinDato);
			return (o);
		}
	}

	@SuppressWarnings("unchecked")
	public static String consultar(final int idTienda, final String fecha) {
		final JSONObject respuesta = new JSONObject();
		respuesta.put("fecha", fecha);

		if (idTienda <= 0 || fecha == null || fecha.length() == 0) {
			respuesta.put("error", "Falta la tienda o la fecha.");
			return (respuesta.toJSONString());
		}
		Tienda tienda = null;
		for (final Tienda t : TiendaDAO.obtenerTiendas()) {
			if (t.getIdTienda() == idTienda) {
				tienda = t;
				break;
			}
		}
		if (tienda == null) {
			respuesta.put("error", "No se encontro la tienda " + idTienda + ".");
			return (respuesta.toJSONString());
		}
		respuesta.put("idtienda", idTienda);
		respuesta.put("tienda", tienda.getNombreTienda());

		final OperacionDomiciliariosDAO.Resultado res = OperacionDomiciliariosDAO.consultarDia(idTienda,
				tienda.getNombreTienda(), tienda.getHosbd(), fecha);
		respuesta.put("fuente", res.fuente);
		respuesta.put("datos_hasta", res.datosHasta);
		if (!res.conecto) {
			respuesta.put("error", res.error);
			return (respuesta.toJSONString());
		}
		respuesta.put("alerta_regreso", OperacionDomiciliariosDAO.MINUTOS_REGRESO_ALERTA);
		respuesta.put("tope_regreso", OperacionDomiciliariosDAO.MINUTOS_REGRESO_MAXIMO);

		//Quien aparece: el que salio a repartir, el interno que solo marco ingreso y
		//el externo que vino y no llevo nada.
		final LinkedHashMap<Integer, Acumulado> porDomi = new LinkedHashMap<Integer, Acumulado>();
		for (final Map.Entry<Integer, OperacionDomiciliariosDAO.Turno> e : res.turnos.entrySet()) {
			final Acumulado a = new Acumulado();
			a.id = e.getKey().intValue();
			a.externo = false;
			a.ingreso = e.getValue().ingreso;
			a.salidaTurno = e.getValue().salida;
			a.minutosTurno = e.getValue().minutos;
			porDomi.put(e.getKey(), a);
		}
		for (final Map.Entry<Integer, OperacionDomiciliariosDAO.Externo> e : res.externos.entrySet()) {
			final Acumulado a = new Acumulado();
			a.id = e.getKey().intValue();
			a.externo = true;
			a.empresa = e.getValue().empresas;
			a.ingreso = e.getValue().ingreso;
			a.salidaTurno = e.getValue().salida;
			a.minutosTurno = e.getValue().minutos;
			porDomi.put(e.getKey(), a);
		}

		//Por hora: pedidos que salieron, salidas y a tiempo de lo medible, y los
		//que llegaron tarde partidos entre internos y externos.
		final int[] horaPedidos = new int[24];
		final int[] horaSalidas = new int[24];
		final int[] horaMedibles = new int[24];
		final int[] horaATiempo = new int[24];
		final int[] horaTardeInt = new int[24];
		final int[] horaTardeExt = new int[24];
		final long[] horaSumaRuta = new long[24];
		final int[] horaConRuta = new int[24];
		final ArrayList<java.util.HashSet<Integer>> horaDomis = new ArrayList<java.util.HashSet<Integer>>();
		for (int h = 0; h < 24; h++) {
			horaDomis.add(new java.util.HashSet<Integer>());
		}

		final Grupo gInterno = new Grupo();
		final Grupo gExterno = new Grupo();
		int totalPedidos = 0;
		int totalEntregados = 0;
		int totalMedibles = 0;
		int totalATiempo = 0;
		long totalSumaRuta = 0;
		int totalConRuta = 0;
		long totalSumaRegreso = 0;
		int totalRegresosValidos = 0;
		int totalRegresosDemorados = 0;
		int totalSinDato = 0;
		int totalProgramados = 0;
		int totalSinEntrega = 0;

		final JSONArray salidasJSON = new JSONArray();
		//Los pedidos que llegaron tarde, con quien los llevo: es la lista que se quiere poder leer.
		final ArrayList<JSONObject> tardes = new ArrayList<JSONObject>();
		for (final OperacionDomiciliariosDAO.Salida s : res.salidas) {
			final Integer idD = Integer.valueOf(s.idDomiciliario);
			Acumulado a = porDomi.get(idD);
			if (a == null) {
				a = new Acumulado();
				a.id = s.idDomiciliario;
				a.externo = res.esExterno(s.idDomiciliario);
				porDomi.put(idD, a);
			}
			final Grupo g = a.externo ? gExterno : gInterno;
			a.salidas++;
			g.salidas++;
			if (a.primeraSalida.length() == 0) {
				a.primeraSalida = s.horaSalida;
			}
			a.ultimaSalida = s.horaSalida;

			if ("OK".equals(s.estadoRegreso)) {
				a.regresosValidos++;
				a.sumaRegreso += s.minutosRegreso;
				totalRegresosValidos++;
				totalSumaRegreso += s.minutosRegreso;
				g.regresosValidos++;
				g.sumaRegreso += s.minutosRegreso;
				if (s.minutosRegreso > OperacionDomiciliariosDAO.MINUTOS_REGRESO_ALERTA) {
					a.regresosDemorados++;
					totalRegresosDemorados++;
					g.regresosDemorados++;
				}
			} else {
				a.regresosSinDato++;
				totalSinDato++;
				g.regresosSinDato++;
			}

			final int h = s.horaDelDia;
			if (h >= 0 && h < 24) {
				horaSalidas[h]++;
				horaDomis.get(h).add(idD);
			}

			final JSONArray pedidosJSON = new JSONArray();
			for (final OperacionDomiciliariosDAO.Pedido p : s.pedidos) {
				a.pedidos++;
				g.pedidos++;
				totalPedidos++;
				if (h >= 0 && h < 24) {
					horaPedidos[h]++;
				}
				if (p.entregado) {
					a.entregados++;
					totalEntregados++;
				} else {
					totalSinEntrega++;
				}
				if (p.programado) {
					totalProgramados++;
				}
				if (p.minutosRuta >= 0) {
					a.conRuta++;
					a.sumaRuta += p.minutosRuta;
					totalConRuta++;
					totalSumaRuta += p.minutosRuta;
					g.conRuta++;
					g.sumaRuta += p.minutosRuta;
					if (h >= 0 && h < 24) {
						horaConRuta[h]++;
						horaSumaRuta[h] += p.minutosRuta;
					}
				}
				boolean tarde = false;
				if (p.medible) {
					a.medibles++;
					a.sumaTotal += p.minutosTotal;
					totalMedibles++;
					g.medibles++;
					if (h >= 0 && h < 24) {
						horaMedibles[h]++;
					}
					if (p.minutosTotal <= p.prometido) {
						a.aTiempo++;
						totalATiempo++;
						g.aTiempo++;
						if (h >= 0 && h < 24) {
							horaATiempo[h]++;
						}
					} else {
						tarde = true;
						if (h >= 0 && h < 24) {
							if (a.externo) {
								horaTardeExt[h]++;
							} else {
								horaTardeInt[h]++;
							}
						}
					}
				}
				final JSONObject po = new JSONObject();
				po.put("id_pedido", p.idPedido);
				po.put("orden", p.orden);
				po.put("hora_entrada", p.horaEntrada);
				po.put("hora_entrega", p.horaEntrega);
				po.put("prometido", p.prometido);
				po.put("min_total", p.minutosTotal >= 0 ? Integer.valueOf(p.minutosTotal) : null);
				po.put("min_ruta", p.minutosRuta >= 0 ? Integer.valueOf(p.minutosRuta) : null);
				po.put("min_espera", p.minutosEspera >= 0 ? Integer.valueOf(p.minutosEspera) : null);
				po.put("programado", p.programado);
				po.put("entregado", p.entregado);
				po.put("medible", p.medible);
				po.put("a_tiempo", p.medible && p.minutosTotal <= p.prometido);
				pedidosJSON.add(po);
				if (tarde) {
					//Se agrega aparte, con el domiciliario y su tipo, para la lista de pedidos tarde.
					final JSONObject to = new JSONObject();
					to.put("id_pedido", p.idPedido);
					to.put("id_domiciliario", s.idDomiciliario);
					to.put("externo", a.externo);
					to.put("hora_entrada", p.horaEntrada);
					to.put("hora_salida", s.horaSalida);
					to.put("hora_entrega", p.horaEntrega);
					to.put("prometido", p.prometido);
					to.put("min_total", p.minutosTotal);
					to.put("exceso", p.minutosTotal - p.prometido);
					to.put("min_ruta", p.minutosRuta >= 0 ? Integer.valueOf(p.minutosRuta) : null);
					to.put("min_espera", p.minutosEspera >= 0 ? Integer.valueOf(p.minutosEspera) : null);
					tardes.add(to);
				}
			}

			final JSONObject so = new JSONObject();
			so.put("id", s.id);
			so.put("id_domiciliario", s.idDomiciliario);
			so.put("externo", a.externo);
			so.put("hora_salida", s.horaSalida);
			so.put("hora_del_dia", s.horaDelDia);
			so.put("hora_regreso", s.horaRegreso);
			so.put("origen", s.origen);
			so.put("estado_regreso", s.estadoRegreso);
			so.put("min_regreso", "OK".equals(s.estadoRegreso) ? Integer.valueOf(s.minutosRegreso) : null);
			so.put("min_hasta_ultima", s.minutosHastaUltima >= 0 ? Integer.valueOf(s.minutosHastaUltima) : null);
			so.put("pedidos", pedidosJSON);
			salidasJSON.add(so);
		}
		respuesta.put("salidas", salidasJSON);

		//Los nombres: el interno sale de general.empleado; el externo, de la persona que uso el cupo ese dia.
		final ArrayList<Acumulado> lista = new ArrayList<Acumulado>(porDomi.values());
		for (final Acumulado a : lista) {
			final String nombreCuenta = res.nombres.get(Integer.valueOf(a.id));
			if (a.externo) {
				final OperacionDomiciliariosDAO.Externo ext = res.externos.get(Integer.valueOf(a.id));
				final String persona = (ext == null) ? "" : ext.nombres;
				final String cupo = (nombreCuenta == null || nombreCuenta.length() == 0) ? ("Cupo " + a.id)
						: nombreCuenta;
				a.nombre = (persona.length() > 0) ? aTitulo(persona) : ("Externo sin nombre (" + cupo + ")");
			} else {
				a.nombre = (nombreCuenta == null || nombreCuenta.length() == 0) ? ("Empleado " + a.id) : nombreCuenta;
			}
		}
		//Del que mas pedidos llevo al que menos; el que solo marco ingreso queda al final.
		Collections.sort(lista, new Comparator<Acumulado>() {
			public int compare(final Acumulado x, final Acumulado y) {
				if (x.pedidos != y.pedidos) {
					return (y.pedidos - x.pedidos);
				}
				return (x.nombre.compareToIgnoreCase(y.nombre));
			}
		});

		int conPedidos = 0;
		long totalMinutosTurno = 0;
		final JSONArray domisJSON = new JSONArray();
		for (final Acumulado a : lista) {
			final Grupo g = a.externo ? gExterno : gInterno;
			g.domiciliarios++;
			if (a.pedidos > 0) {
				conPedidos++;
				g.conPedidos++;
			}
			final JSONObject o = new JSONObject();
			o.put("id", a.id);
			o.put("nombre", a.nombre);
			o.put("tipo", a.externo ? "EXTERNO" : "INTERNO");
			o.put("empresa", a.empresa);
			o.put("ingreso", a.ingreso);
			o.put("salida", a.salidaTurno);
			final int minTurno = a.minutosTurno;
			o.put("minutos_turno", minTurno >= 0 ? Integer.valueOf(minTurno) : null);
			if (minTurno > 0) {
				totalMinutosTurno += minTurno;
			}
			o.put("salidas", a.salidas);
			o.put("pedidos", a.pedidos);
			o.put("entregados", a.entregados);
			o.put("medibles", a.medibles);
			o.put("a_tiempo", a.aTiempo);
			o.put("demorados", a.medibles - a.aTiempo);
			o.put("porcentaje", a.medibles > 0 ? Double.valueOf(redondear(a.aTiempo * 100.0 / a.medibles)) : null);
			o.put("prom_ruta", a.conRuta > 0 ? Double.valueOf(redondear(a.sumaRuta / (double) a.conRuta)) : null);
			o.put("prom_total", a.medibles > 0 ? Double.valueOf(redondear(a.sumaTotal / (double) a.medibles)) : null);
			o.put("prom_regreso",
					a.regresosValidos > 0 ? Double.valueOf(redondear(a.sumaRegreso / (double) a.regresosValidos)) : null);
			o.put("regresos_validos", a.regresosValidos);
			o.put("regresos_demorados", a.regresosDemorados);
			o.put("regresos_sin_dato", a.regresosSinDato);
			o.put("pedidos_por_salida", a.salidas > 0 ? Double.valueOf(redondear(a.pedidos / (double) a.salidas)) : null);
			o.put("primera_salida", a.primeraSalida);
			o.put("ultima_salida", a.ultimaSalida);
			//Pedidos por hora en la tienda: solo si se sabe cuanto estuvo y fue mas de media hora; con menos
			//tiempo la division da cifras que no significan nada.
			o.put("pedidos_por_hora", (minTurno >= 30 && a.pedidos > 0)
					? Double.valueOf(redondear(a.pedidos * 60.0 / minTurno)) : null);
			domisJSON.add(o);
		}
		respuesta.put("domiciliarios", domisJSON);

		//Los pedidos tarde, de los mas pasados a los menos; con quien los llevo ya resuelto.
		Collections.sort(tardes, new Comparator<JSONObject>() {
			public int compare(final JSONObject x, final JSONObject y) {
				return (((Integer) y.get("exceso")).intValue() - ((Integer) x.get("exceso")).intValue());
			}
		});
		final JSONArray tardesJSON = new JSONArray();
		for (final JSONObject t : tardes) {
			final Acumulado a = porDomi.get(t.get("id_domiciliario"));
			t.put("domiciliario", a == null ? "" : a.nombre);
			t.put("empresa", a == null ? "" : a.empresa);
			tardesJSON.add(t);
		}
		respuesta.put("pedidos_tarde", tardesJSON);

		//Por hora, en el orden del dia operativo.
		final JSONArray horasJSON = new JSONArray();
		for (final int h : ORDEN_HORAS) {
			final JSONObject o = new JSONObject();
			o.put("hora", h);
			o.put("pedidos", horaPedidos[h]);
			o.put("salidas", horaSalidas[h]);
			o.put("domiciliarios", horaDomis.get(h).size());
			o.put("prom_ruta", horaConRuta[h] > 0 ? Double.valueOf(redondear(horaSumaRuta[h] / (double) horaConRuta[h])) : null);
			o.put("medibles", horaMedibles[h]);
			o.put("a_tiempo", horaATiempo[h]);
			o.put("tarde_internos", horaTardeInt[h]);
			o.put("tarde_externos", horaTardeExt[h]);
			horasJSON.add(o);
		}
		respuesta.put("por_hora", horasJSON);

		final JSONObject resumen = new JSONObject();
		resumen.put("domiciliarios", lista.size());
		resumen.put("con_pedidos", conPedidos);
		resumen.put("sin_pedidos", lista.size() - conPedidos);
		resumen.put("pedidos", totalPedidos);
		resumen.put("entregados", totalEntregados);
		resumen.put("sin_entrega", totalSinEntrega);
		resumen.put("programados", totalProgramados);
		resumen.put("salidas", res.salidas.size());
		resumen.put("pedidos_por_salida",
				res.salidas.size() > 0 ? Double.valueOf(redondear(totalPedidos / (double) res.salidas.size())) : null);
		resumen.put("prom_ruta", totalConRuta > 0 ? Double.valueOf(redondear(totalSumaRuta / (double) totalConRuta)) : null);
		resumen.put("prom_regreso",
				totalRegresosValidos > 0 ? Double.valueOf(redondear(totalSumaRegreso / (double) totalRegresosValidos)) : null);
		resumen.put("regresos_validos", totalRegresosValidos);
		resumen.put("regresos_demorados", totalRegresosDemorados);
		resumen.put("regresos_sin_dato", totalSinDato);
		resumen.put("medibles", totalMedibles);
		resumen.put("a_tiempo", totalATiempo);
		resumen.put("demorados", totalMedibles - totalATiempo);
		resumen.put("porcentaje", totalMedibles > 0 ? Double.valueOf(redondear(totalATiempo * 100.0 / totalMedibles)) : null);
		resumen.put("horas_en_tienda", Double.valueOf(redondear(totalMinutosTurno / 60.0)));
		resumen.put("internos", gInterno.aJSON());
		resumen.put("externos", gExterno.aJSON());
		respuesta.put("resumen", resumen);
		return (respuesta.toJSONString());
	}

	private static double redondear(final double valor) {
		return (Math.round(valor * 10.0) / 10.0);
	}

	/** "hernan dario vallejo duque" -> "Hernan Dario Vallejo Duque". Los externos se digitan en minuscula. */
	private static String aTitulo(final String texto) {
		final StringBuilder sb = new StringBuilder();
		for (final String palabra : texto.trim().split("\\s+")) {
			if (palabra.length() == 0) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1).toLowerCase());
		}
		return (sb.toString());
	}
}
