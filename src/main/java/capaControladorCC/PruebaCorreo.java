package capaControladorCC;

import java.util.ArrayList;

import capaModeloCC.Correo;
import utilidadesCC.ControladorEnvioCorreo;

public class PruebaCorreo {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Correo correo = new Correo();
		correo.setAsunto("PIZZA AMERICANA TE SALUDA ");
		ArrayList correos = new ArrayList();
		String correoEle = "jubote1@gmail.com";
		correos.add(correoEle);
		correo.setContrasena("Pizza-virtual*2020");
		//correo.setContrasena("Pizzaamericana2017");
		correo.setUsuarioCorreo("pagosvirtual@pizzaamericana.co");
		//correo.setUsuarioCorreo("alertaspizzaamericana@gmail.com");
		String mensaje = "<body><a href=\"www.pizzaamericana.co\"><img align=\" center \" src=\"https://pizzaamericana.co/wompi/\"></a></body>";
		correo.setMensaje(mensaje );
		ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correo, correos);
		contro.enviarCorreoInstitucional();
		//contro.enviarCorreo();
	}

}
