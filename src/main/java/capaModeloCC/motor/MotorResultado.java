package capaModeloCC.motor;

import java.util.ArrayList;
import java.util.List;

/**
 * CONTRATO DE SALIDA del motor de pedido. Lo que el motor devuelve es exactamente lo que se mandaria a la
 * tienda: la lista de lineas, con sus relaciones de maestra e hija, y los valores calculados.
 *
 * Las lineas llevan una referencia temporal ("ref") en vez del id definitivo, porque el id real lo asigna
 * la tienda al insertar. refMaster y refModificador apuntan a otra linea por su ref (0 = ninguna).
 */
public class MotorResultado {

	public static final String OK = "OK";
	/** El pedido no es valido en el catalogo (producto fuera de horario, pregunta obligatoria sin respuesta...). */
	public static final String RECHAZADO = "RECHAZADO";
	public static final String NO_IMPLEMENTADO = "NO_IMPLEMENTADO";
	public static final String ERROR = "ERROR";

	public String estado = ERROR;
	/** Version del motor que produjo el resultado (para comparar corridas entre versiones). */
	public String versionMotor = "";
	public List<Linea> lineas = new ArrayList<Linea>();
	/** Suma de los valores totales de las lineas, ANTES de descuentos. */
	public double subtotal;
	public double descuento;
	public double total;
	public List<Advertencia> advertencias = new ArrayList<Advertencia>();

	public static class Linea {
		public int ref;
		public int idproducto;
		public String descripcion = "";
		public double cantidad;
		public double valorUnitario;
		public double valorTotal;
		public int refMaster;
		public int refModificador;
		/** SOLICITADA, OPCION, INCLUIDO, MODIFICADOR o DOMICILIO. */
		public String origen = "";
	}

	public static class Advertencia {
		public String codigo = "";
		public String mensaje = "";
		public int idproducto;

		public Advertencia() {
		}

		public Advertencia(String codigo, String mensaje, int idproducto) {
			this.codigo = codigo;
			this.mensaje = mensaje;
			this.idproducto = idproducto;
		}
	}

	public static MotorResultado conEstado(String estado, String versionMotor, String codigo, String mensaje) {
		MotorResultado r = new MotorResultado();
		r.estado = estado;
		r.versionMotor = versionMotor;
		r.advertencias.add(new Advertencia(codigo, mensaje, 0));
		return r;
	}

	/** Recalcula subtotal y total a partir de las lineas. */
	public void recalcularTotales() {
		double suma = 0;
		for (Linea l : lineas) {
			suma += l.valorTotal;
		}
		this.subtotal = suma;
		this.total = suma - descuento;
	}
}
