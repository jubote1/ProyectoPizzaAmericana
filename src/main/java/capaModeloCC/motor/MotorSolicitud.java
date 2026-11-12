package capaModeloCC.motor;

import java.util.ArrayList;
import java.util.List;

/**
 * CONTRATO DE ENTRADA del motor de pedido (etapa 2 de la migracion del central al modelo del POS).
 *
 * Describe lo que el cliente ELIGIO, en terminos del catalogo del POS (esquema pos_central), no del
 * catalogo viejo del central. El motor valida las elecciones contra el catalogo, arma las lineas como las
 * armaria la pantalla de la tienda y calcula los precios. Quien llama (el BOT, una pantalla futura, el
 * arnes de repeticion) es el que traduce sus propios datos a esta forma.
 *
 * Todos los ids de producto son ids del POS (pos_central.producto), NUNCA del catalogo viejo del central:
 * el mismo numero en los dos lados suele ser otro producto.
 *
 * Se serializa con Gson. Todos los campos son publicos a proposito: es un objeto de transporte.
 */
public class MotorSolicitud {

	/** Version del contrato. Subirla cuando cambie la forma de este objeto. */
	public static final String VERSION_CONTRATO = "1";

	public String version = VERSION_CONTRATO;
	public Contexto contexto = new Contexto();
	public List<Linea> lineas = new ArrayList<Linea>();
	/** Costo de domicilio como producto del POS (por ejemplo 411 o 412). Nulo si no hay. */
	public Domicilio domicilio;
	/** Descuentos que el cliente quiere aplicar. Nulo si no hay. */
	public Descuentos descuentos;
	/**
	 * Solo informativo: lo que quien armo la solicitud (por ejemplo el traductor del BOT) no pudo traducir al
	 * catalogo del POS. El motor no lo lee; sirve para explicar diferencias y para medir cuanto falla la traduccion.
	 */
	public List<String> avisosTraduccion = new ArrayList<String>();

	/** Datos del pedido que no son productos. */
	public static class Contexto {
		/** De donde viene: BOT, CONTACT, TIENDA (arnes), etc. Decide el horario por canal. */
		public String canal = "";
		public int idtienda;
		public int idtipopedido;
		public int idcliente;
		public boolean esDomicilio;
		public boolean programado;
		/** Hora programada en formato HH:mm, vacio si es para ahora. */
		public String horaProgramado = "";
		/** Momento al que se refiere la cotizacion (para horarios y descuentos por hora): yyyy-MM-dd HH:mm:ss. */
		public String fechaHora = "";
	}

	/** Un producto principal pedido, con sus elecciones. */
	public static class Linea {
		/** Id del producto principal en el POS (una pizza, un combo, una bebida suelta, una promocion). */
		public int idproducto;
		public double cantidad = 1;
		/** Opciones elegidas en las preguntas forzadas del producto (sabores, bebida, acompanante...). */
		public List<Seleccion> selecciones = new ArrayList<Seleccion>();
		/** Modificadores CON y SIN pedidos sobre algun producto de esta linea. */
		public List<Modificador> modificadores = new ArrayList<Modificador>();
		public String observacion = "";
		/**
		 * Solo para el arnes de repeticion: el valor total que la tienda registro para esta linea. El motor
		 * NO debe leerlo para calcular nada; sirve para explicar diferencias.
		 */
		public Double valorReferencia;
	}

	/** Una opcion elegida dentro de una pregunta forzada. */
	public static class Seleccion {
		/** Id de la pregunta, si quien llama lo sabe; si es 0 el motor la deduce de las opciones del producto. */
		public int idpregunta;
		/** 0 = pregunta sin division, 1 = primera mitad, 2 = segunda mitad. */
		public int mitad;
		/** Producto del POS que se eligio como opcion (un sabor tipo D, una gaseosa, una adicion...). */
		public int idproductoOpcion;
		/** 1 en preguntas sin division; 0,5 por mitad cuando la pregunta permite dividir en dos. */
		public double cantidad = 1;
	}

	/** Un modificador CON o SIN. */
	public static class Modificador {
		/** CON o SIN. */
		public String tipo = "";
		public int idproducto;
		public double cantidad = 0.5;
		/** Producto de la linea sobre el que aplica (por ejemplo la especialidad de una mitad). */
		public int sobreProducto;
	}

	public static class Domicilio {
		public int idproducto;
		/** Valor que se espera cobrar; el motor lo compara con el precio del catalogo y advierte si difieren. */
		public double valor;
	}

	public static class Descuentos {
		public String codigoPromocional = "";
		public boolean redimirPuntos;
		public double descuentoManual = 0.0;
		public String motivoDescuento = "";
	}
}
