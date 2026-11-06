package capaControladorCC.motor;

import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;

/**
 * Motor de pedido: recibe lo que el cliente eligio (en terminos del catalogo del POS) y devuelve las
 * lineas y los valores que se mandarian a la tienda.
 *
 * Reglas de la interfaz:
 * - No escribe en ninguna base ni envia nada a ninguna tienda. Solo lee el catalogo (pos_central).
 * - Nunca lanza excepciones hacia afuera: ante cualquier problema devuelve estado ERROR con la causa en
 *   las advertencias, para que quien lo llama en sombra no vea afectado su propio flujo.
 * - Es determinista: la misma solicitud con el mismo catalogo produce el mismo resultado.
 */
public interface MotorPedido {

	/** Identificador de la version del motor, por ejemplo "v0-nada" o "v1". Se guarda con cada resultado. */
	String version();

	MotorResultado cotizar(MotorSolicitud solicitud);
}
