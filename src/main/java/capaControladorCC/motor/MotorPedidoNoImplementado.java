package capaControladorCC.motor;

import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;

/**
 * Motor vacio: reconoce el contrato y responde NO_IMPLEMENTADO. Sirve para probar el gancho de sombra y el
 * arnes de repeticion antes de que exista la logica real. Sera reemplazado paquete a paquete.
 */
public class MotorPedidoNoImplementado implements MotorPedido {

	@Override
	public String version() {
		return "v0-no-implementado";
	}

	@Override
	public MotorResultado cotizar(MotorSolicitud solicitud) {
		return MotorResultado.conEstado(MotorResultado.NO_IMPLEMENTADO, version(), "SIN_LOGICA",
				"El motor aun no calcula pedidos.");
	}
}
