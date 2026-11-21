package utilidadesCC;

import java.awt.geom.Path2D;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import capaDAOCC.TiendaDAO;

/**
 * Caché local en memoria de los 11 polígonos de cobertura de tiendas de Pizza Americana.
 *
 * Permite evaluar la cobertura (punto dentro de polígono) en 0 ms en memoria local,
 * sin depender de llamadas HTTP a ArcGIS (services1.arcgis.com).
 * Si ArcGIS se cae, tiene microcortes o tiempos de espera, los pedidos no se pierden
 * y se asignan a su tienda inmediatamente.
 */
public final class ZonasCoberturaCache {

	public static final class Zona {
		private final String nombre;
		private final Path2D.Double path;
		private volatile int idTienda;

		public Zona(String nombre, Path2D.Double path, int idTienda) {
			this.nombre = nombre;
			this.path = path;
			this.idTienda = idTienda;
		}

		public String getNombre() {
			return nombre;
		}

		public int getIdTienda() {
			if (idTienda <= 0) {
				try {
					idTienda = TiendaDAO.obteneridTienda(nombre);
				} catch (Exception e) {
					System.err.println("ZonasCoberturaCache: Error al resolver idTienda para " + nombre + ": " + e.getMessage());
				}
			}
			return idTienda;
		}

		public boolean contiene(double longitud, double latitud) {
			return path != null && path.contains(longitud, latitud);
		}
	}

	private static volatile List<Zona> zonas = Collections.emptyList();
	private static volatile boolean inicializado = false;

	static {
		inicializar();
	}

	private ZonasCoberturaCache() {
	}

	public static synchronized void inicializar() {
		if (inicializado && !zonas.isEmpty()) {
			return;
		}
		try (InputStream is = ZonasCoberturaCache.class.getClassLoader().getResourceAsStream("zonas_cobertura_arcgis.json")) {
			if (is != null) {
				JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
				List<Zona> cargadas = parsearFeatures(json);
				if (!cargadas.isEmpty()) {
					zonas = Collections.unmodifiableList(cargadas);
					inicializado = true;
					System.out.println("ZonasCoberturaCache: " + zonas.size() + " polígonos de tiendas cargados exitosamente desde archivo local.");
					return;
				}
			}
			System.err.println("ZonasCoberturaCache: No se encontró zonas_cobertura_arcgis.json en el classpath.");
		} catch (Exception e) {
			System.err.println("ZonasCoberturaCache: Error al inicializar caché de polígonos: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Parsea un JsonObject de ArcGIS (con array 'features') a una lista de objetos Zona con Path2D.
	 */
	public static List<Zona> parsearFeatures(JsonObject json) {
		List<Zona> resultado = new ArrayList<>();
		if (json == null || !json.has("features")) {
			return resultado;
		}
		JsonArray features = json.getAsJsonArray("features");
		for (JsonElement elem : features) {
			try {
				JsonObject feat = elem.getAsJsonObject();
				if (!feat.has("attributes") || !feat.has("geometry")) {
					continue;
				}
				JsonObject attr = feat.getAsJsonObject("attributes");
				String nombre = attr.has("nombre") && !attr.get("nombre").isJsonNull()
						? attr.get("nombre").getAsString().trim()
						: "";
				if (nombre.isEmpty()) {
					continue;
				}

				JsonObject geom = feat.getAsJsonObject("geometry");
				if (!geom.has("rings")) {
					continue;
				}
				JsonArray rings = geom.getAsJsonArray("rings");
				Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);

				for (JsonElement rElem : rings) {
					JsonArray ring = rElem.getAsJsonArray();
					if (ring.isEmpty()) {
						continue;
					}
					boolean first = true;
					for (JsonElement pElem : ring) {
						JsonArray pt = pElem.getAsJsonArray();
						double x = pt.get(0).getAsDouble(); // lng
						double y = pt.get(1).getAsDouble(); // lat
						if (first) {
							path.moveTo(x, y);
							first = false;
						} else {
							path.lineTo(x, y);
						}
					}
					path.closePath();
				}

				int idTienda = 0;
				try {
					idTienda = TiendaDAO.obteneridTienda(nombre);
				} catch (Exception ignored) {
				}

				resultado.add(new Zona(nombre, path, idTienda));
			} catch (Exception e) {
				System.err.println("ZonasCoberturaCache: Error al procesar feature: " + e.getMessage());
			}
		}
		return resultado;
	}

	/**
	 * Busca la zona que contiene el punto (longitud, latitud).
	 * Si idTiendaEsperada viene indicado y coincide con alguna zona que contenga el punto,
	 * se prioriza esa tienda.
	 */
	public static Zona buscarZona(double longitud, double latitud, Integer idTiendaEsperada) {
		if (!inicializado || zonas.isEmpty()) {
			inicializar();
		}
		List<Zona> lista = zonas;
		if (lista.isEmpty()) {
			return null;
		}

		Zona primeraCoincidencia = null;
		for (Zona z : lista) {
			if (z.contiene(longitud, latitud)) {
				if (idTiendaEsperada != null && idTiendaEsperada > 0 && z.getIdTienda() == idTiendaEsperada) {
					return z; // Coincidencia exacta con la tienda esperada
				}
				if (primeraCoincidencia == null) {
					primeraCoincidencia = z;
				}
			}
		}
		return primeraCoincidencia;
	}

	public static int cantidadZonas() {
		return zonas.size();
	}
}
