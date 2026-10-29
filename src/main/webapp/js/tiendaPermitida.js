/**
 * Deja en el combo de tiendas unicamente la que le corresponde al usuario.
 *
 * El administrador de punto de venta ve solo su tienda; los demas siguen viendo
 * las once y esta funcion no les cambia nada.
 *
 * Esto es SOLO la pantalla. La restriccion de verdad la aplica cada servicio de
 * consulta en el servidor: un combo recortado no detiene a nadie que arme la URL
 * a mano, y aqui lo que se busca es que la pantalla no ofrezca lo que despues le
 * van a negar.
 *
 * Uso, despues de haber llenado el combo:
 *
 *   tpRecortarCombo('#idtienda', '#avisoTienda');
 *
 * El segundo parametro es opcional: donde escribir el aviso de que esta viendo
 * una sola tienda.
 */
function tpRecortarCombo(selectorCombo, selectorAviso) {
	$.getJSON(server + 'GetTiendaPermitida', function (d) {
		if (!d || d.restringido !== true) {
			return;
		}
		var combo = $(selectorCombo);
		if (!(d.idtienda > 0)) {
			//Administrador sin tienda deducida: no se le ofrece ninguna.
			combo.html('<option value="0">Sin punto de venta asignado</option>').prop('disabled', true);
			tpAviso(selectorAviso, 'Todav&iacute;a no podemos saber de cu&aacute;l punto de venta es usted: no hay '
				+ 'horas suyas registradas la semana pasada. Av&iacute;sele a Gesti&oacute;n Humana o a Sistemas.');
			return;
		}
		//Se deja la opcion de su tienda y se borran las demas, incluida "Todas".
		combo.find('option').each(function () {
			if (parseInt($(this).val(), 10) !== d.idtienda) {
				$(this).remove();
			}
		});
		if (combo.find('option').length === 0) {
			combo.append($('<option></option>').attr('value', d.idtienda).text(d.tienda || ('Tienda ' + d.idtienda)));
		}
		combo.val(String(d.idtienda)).prop('disabled', true);
		//Deshabilitado no se envia en un form; el servidor igual lo fuerza, pero asi el JS lo sigue leyendo.
		combo.trigger('change');
		tpAviso(selectorAviso, 'Usted ve el punto de venta <b>' + (d.tienda || d.idtienda) + '</b>, que es donde '
			+ 'm&aacute;s trabaj&oacute; la semana pasada.');
	});
}

function tpAviso(selectorAviso, html) {
	if (!selectorAviso || $(selectorAviso).length === 0) {
		return;
	}
	$(selectorAviso).html('<div class="alert alert-info" style="margin-top:8px">' + html + '</div>');
}

/**
 * Para las pantallas que NO tienen combo de tienda -Desempeno de Domiciliarios
 * consulta las once-: solo avisa cual esta viendo.
 */
function tpAvisarTienda(selectorAviso) {
	$.getJSON(server + 'GetTiendaPermitida', function (d) {
		if (!d || d.restringido !== true) {
			return;
		}
		if (!(d.idtienda > 0)) {
			tpAviso(selectorAviso, 'Todav&iacute;a no podemos saber de cu&aacute;l punto de venta es usted: no hay '
				+ 'horas suyas registradas la semana pasada. Av&iacute;sele a Gesti&oacute;n Humana o a Sistemas.');
			return;
		}
		tpAviso(selectorAviso, 'Usted ve solo el punto de venta <b>' + (d.tienda || d.idtienda) + '</b>, que es '
			+ 'donde m&aacute;s trabaj&oacute; la semana pasada.');
	});
}
