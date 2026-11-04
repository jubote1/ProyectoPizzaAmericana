/** auditoriaFidelizacion.js
 *
 * Pantalla AuditoriaFidelizacion.html. Dos vistas sobre los mismos filtros:
 *
 *   Redenciones       -> quien saco producto con puntos, cuando y en que tienda.
 *   Quien acumula     -> quien esta juntando puntos, que es donde empieza el problema.
 *
 * En cualquiera de las dos, al dar clic en una fila sale a la derecha el historial completo
 * de esa persona.
 *
 * Existe para mirar la sospecha de que empleados usen mal los puntos, asi que no se limita a
 * listar: marca senales.
 *
 *   EMPLEADO    -> el correo de la cuenta de puntos esta en la nomina.
 *   AUTO        -> ademas, quien proceso la redencion es esa misma persona.
 *   VARIOS/DIA  -> junto mas de un pedido con puntos en un mismo dia, en 2 o mas dias.
 *   SOSPECHOSO  -> cualquiera de estas tres: concentracion de 70% o mas en una sola PERSONA
 *                  (sobre al menos 5 pedidos), empleado que se auto-redime, o 2+ dias con
 *                  varios pedidos.
 *
 * LO QUE NO ES SENAL: comprar mucho. Un cliente fiel compra mucho y eso es lo que se quiere.
 * La firma es la concentracion: a un frecuente legitimo lo atiende quien este en la caja ese
 * dia, asi que su atencion se reparte.
 *
 * Y OJO con los canales: CONTACT, BOT, APP, TIEVIRTUALKUNO y CAJA aparecen en la columna de
 * quien atendio pero NO son personas. El servidor los deja por fuera del porcentaje; si no,
 * cualquiera que pida siempre por el contact center saldria con 100% de concentracion.
 *
 * Todo viene de ConsultarAuditoriaFidelizacion con accion = SEMANA, ACUMULADORES o CLIENTE.
 *
 * Los textos con tilde van con secuencias \\u para que el archivo quede en ASCII, igual que
 * en las demas pantallas de reporteria.
 */

var afVista = 'red';       //'red' o 'acum'
var afDatos = null;        //la ultima respuesta de redenciones
var afAcum = null;         //la ultima respuesta de acumuladores
var afCorreoSel = '';      //el correo del que se esta viendo el detalle

$(function () {
	afCargarTiendas();
	afRango('semanaPasada');

	$('#afBuscar').click(function () { afConsultar(); });
	$('#afSenal').change(function () { afConsultar(); });
	$('#afTienda').change(function () { afConsultar(); });
	$('#afSoloSospechosos').change(function () { afPintarAcumuladores(); });
	$('#afSemanaPasada').click(function () { afRango('semanaPasada'); afConsultar(); });
	$('#afEstaSemana').click(function () { afRango('estaSemana'); afConsultar(); });
	$('#afMes').click(function () { afRango('mes'); afConsultar(); });
	$('#afTrim').click(function () { afRango('trimestre'); afConsultar(); });

	$('#afTabsPrin a').click(function () {
		afVista = $(this).attr('data-vista');
		$('#afTabsPrin li').removeClass('activo');
		$(this).parent().addClass('activo');
		$('.af-vista').hide();
		$('.af-vista[data-vista="' + afVista + '"]').show();
		//El filtro de senal solo aplica a las redenciones.
		$('#afSenal').closest('div').css('visibility', afVista === 'red' ? 'visible' : 'hidden');
		afConsultar();
	});
});

// ---------------------------------------------------------------------------------------
// Filtros
// ---------------------------------------------------------------------------------------

function afCargarTiendas() {
	var str = '<option value="0">Todas</option>';
	$.getJSON(server + 'GetTiendasFuncionales', function (data) {
		for (var i = 0; i < data.length; i++) {
			str += '<option value="' + data[i].id + '">' + afEsc(data[i].nombre) + '</option>';
		}
		$('#afTienda').html(str);
		afConsultar();
	}).fail(function () {
		$('#afTienda').html(str);
		afConsultar();
	});
}

function afAFecha(f) {
	var m = f.getMonth() + 1, d = f.getDate();
	return f.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (d < 10 ? '0' : '') + d;
}

/**
 * Los rangos rapidos.
 *
 * La semana va de LUNES A DOMINGO, que es el corte de Pizza Americana. getDay() devuelve 0
 * para domingo, asi que hay que convertirlo a 7 antes de restar o el lunes sale corrido una
 * semana entera cada domingo.
 */
function afRango(tipo) {
	var hoy = new Date();
	var diaIso = hoy.getDay() === 0 ? 7 : hoy.getDay();
	var lunesEsta = new Date(hoy);
	lunesEsta.setDate(hoy.getDate() - (diaIso - 1));
	var desde, hasta;

	if (tipo === 'semanaPasada') {
		desde = new Date(lunesEsta); desde.setDate(lunesEsta.getDate() - 7);
		hasta = new Date(lunesEsta); hasta.setDate(lunesEsta.getDate() - 1);
	} else if (tipo === 'estaSemana') {
		desde = lunesEsta;
		hasta = hoy;
	} else if (tipo === 'mes') {
		desde = new Date(hoy); desde.setDate(hoy.getDate() - 29);
		hasta = hoy;
	} else {
		desde = new Date(hoy); desde.setDate(hoy.getDate() - 89);
		hasta = hoy;
	}
	$('#afDesde').val(afAFecha(desde));
	$('#afHasta').val(afAFecha(hasta));
}

function afConsultar() {
	if (afVista === 'acum') {
		afConsultarAcumuladores();
	} else {
		afConsultarRedenciones();
	}
}

// ---------------------------------------------------------------------------------------
// Vista 1: las redenciones
// ---------------------------------------------------------------------------------------

function afConsultarRedenciones() {
	var desde = $('#afDesde').val();
	var hasta = $('#afHasta').val();
	if (!desde || !hasta) { return; }

	$('#afCargando').show();
	$.getJSON(server + 'ConsultarAuditoriaFidelizacion', {
		accion: 'SEMANA',
		fechainicial: desde,
		fechafinal: hasta,
		idtienda: $('#afTienda').val() || '0',
		senal: $('#afSenal').val() || 'TODAS'
	}, function (data) {
		$('#afCargando').hide();
		if (!data || data.resultado !== 'OK') {
			afVacio('#afCuerpo', 8, data && data.mensaje ? data.mensaje : 'No se pudo consultar.');
			$('#afTarjetas').html('');
			return;
		}
		afDatos = data;
		afTarjetasRedenciones(data.totales);
		afPintarRedenciones(data.redenciones);
		$('#afRotuloRango').html('Del ' + afEsc(data.desde) + ' al ' + afEsc(data.hasta)
			+ ' \u00b7 ' + data.redenciones.length + ' redenci\u00f3n(es)');
	}).fail(function () {
		$('#afCargando').hide();
		afVacio('#afCuerpo', 8, 'No se pudo consultar. Revise la conexi\u00f3n y vuelva a intentar.');
	});
}

function afTarjetasRedenciones(t) {
	if (!t) { $('#afTarjetas').html(''); return; }
	var h = '';
	h += afTarjeta('Redenciones', t.redenciones, '', false);
	h += afTarjeta('Personas', t.personas, 'distintas', false);
	h += afTarjeta('Puntos redimidos', afMiles(t.puntos), '', false);
	h += afTarjeta('Cuentas de empleados', t.de_empleados, 'el correo est\u00e1 en la n\u00f3mina', t.de_empleados > 0);
	h += afTarjeta('Auto-redenciones', t.auto_redenciones, 'se la proces\u00f3 \u00e9l mismo', t.auto_redenciones > 0);
	h += afTarjeta('Reversadas', t.reversadas, '', false);
	if (t.sin_cajero > 0) {
		h += afTarjeta('Sin qui\u00e9n atendi\u00f3', t.sin_cajero, 'anteriores a sep-2026', false);
	}
	$('#afTarjetas').html(h);
}

function afPintarRedenciones(filas) {
	if (!filas || filas.length === 0) {
		afVacio('#afCuerpo', 8, 'No hubo redenciones en este per\u00edodo con el filtro escogido.');
		return;
	}
	var h = '';
	for (var i = 0; i < filas.length; i++) {
		var r = filas[i];
		var senales = '';
		if (r.auto_redencion) {
			senales += '<span class="etq etq-auto" title="Qui\u00e9n la proces\u00f3 es el due\u00f1o de la cuenta">AUTO</span> ';
		} else if (r.cliente_es_empleado) {
			senales += '<span class="etq etq-emp" title="' + afEsc(r.cliente_empleado_nombre) + '">EMPLEADO</span> ';
		}
		if (r.estado === 'REVERSADA') {
			senales += '<span class="etq etq-rev">REVERSADA</span>';
		}
		h += '<tr data-correo="' + afEsc(r.correo) + '">'
			+ '<td>' + afEsc(r.fecha) + '</td>'
			+ '<td>' + afEsc(r.correo) + '</td>'
			+ '<td class="num">' + afMiles(r.puntos) + '</td>'
			+ '<td>' + afEsc(r.tienda) + '</td>'
			+ '<td class="num">' + (r.idpedidotienda > 0 ? r.idpedidotienda : '&mdash;') + '</td>'
			+ '<td>' + afQuien(r.usuario_nombre, r.usuario) + '</td>'
			+ '<td class="cen"><span class="etq ' + (r.origen === 'POS' ? 'etq-pos' : 'etq-cc') + '">'
				+ afEsc(r.origen || '?') + '</span></td>'
			+ '<td>' + senales + '</td>'
			+ '</tr>';
	}
	$('#afCuerpo').html(h);
	afEngancharClic('#afCuerpo');
}

// ---------------------------------------------------------------------------------------
// Vista 2: quien acumula puntos
// ---------------------------------------------------------------------------------------

function afConsultarAcumuladores() {
	var desde = $('#afDesde').val();
	var hasta = $('#afHasta').val();
	if (!desde || !hasta) { return; }

	$('#afCargando').show();
	$.getJSON(server + 'ConsultarAuditoriaFidelizacion', {
		accion: 'ACUMULADORES',
		fechainicial: desde,
		fechafinal: hasta,
		idtienda: $('#afTienda').val() || '0'
	}, function (data) {
		$('#afCargando').hide();
		if (!data || data.resultado !== 'OK') {
			afVacio('#afCuerpoAcum', 9, data && data.mensaje ? data.mensaje : 'No se pudo consultar.');
			$('#afTarjetas').html('');
			return;
		}
		afAcum = data;
		afTarjetasAcumuladores(data.totales);
		afPintarAcumuladores();
		$('#afRotuloAcum').html('Del ' + afEsc(data.desde) + ' al ' + afEsc(data.hasta)
			+ ' \u00b7 ' + data.acumuladores.length + ' persona(s) con 3 o m\u00e1s compras'
			+ ', ordenadas por puntos');
	}).fail(function () {
		$('#afCargando').hide();
		afVacio('#afCuerpoAcum', 9, 'No se pudo consultar. Revise la conexi\u00f3n y vuelva a intentar.');
	});
}

function afTarjetasAcumuladores(t) {
	if (!t) { $('#afTarjetas').html(''); return; }
	var h = '';
	h += afTarjeta('Personas', t.personas, 'con 3 o m\u00e1s compras', false);
	h += afTarjeta('Puntos acumulados', afMiles(t.puntos), 'en el per\u00edodo', false);
	h += afTarjeta('Sospechosos', t.sospechosos, 'por cualquiera de las se\u00f1ales', t.sospechosos > 0);
	h += afTarjeta('Cuentas de empleados', t.de_empleados, 'el correo est\u00e1 en la n\u00f3mina', t.de_empleados > 0);
	h += afTarjeta('Varios pedidos/d\u00eda', t.con_dias_cargados,
		t.dias_cargados_umbral + ' d\u00edas o m\u00e1s', t.con_dias_cargados > 0);
	$('#afTarjetas').html(h);
}

function afPintarAcumuladores() {
	if (!afAcum) { return; }
	var solo = $('#afSoloSospechosos').is(':checked');
	var filas = afAcum.acumuladores || [];
	var h = '';
	var mostradas = 0;

	for (var i = 0; i < filas.length; i++) {
		var a = filas[i];
		if (solo && !a.sospechoso) { continue; }
		mostradas++;

		var senales = '';
		if (a.sospechoso) {
			senales += '<span class="etq etq-auto" title="' + afEsc(a.motivo) + '">SOSPECHOSO</span> ';
		}
		if (a.es_empleado) {
			senales += '<span class="etq etq-emp" title="' + afEsc(a.empleado_nombre) + '">EMPLEADO</span> ';
		}
		if (a.auto_redenciones > 0) {
			senales += '<span class="etq etq-auto">' + a.auto_redenciones + ' AUTO</span> ';
		}
		if (a.dias_con_varios >= 2) {
			senales += '<span class="etq etq-emp" title="Junt\u00f3 m\u00e1s de un pedido con puntos en '
				+ a.dias_con_varios + ' d\u00eda(s) distintos'
				+ (a.max_pedidos_dia > 2 ? ', hasta ' + a.max_pedidos_dia + ' en uno solo' : '')
				+ '">VARIOS/D\u00cdA</span>';
		}

		//Dias en que junto mas de un pedido con puntos. Un dia suelto no se resalta:
		//almuerzo y comida el mismo dia pasa y es legitimo. Lo que llama la atencion
		//es que se repita.
		var dias = a.dias_con_varios || 0;
		var diasTxt = dias === 0
			? '<span style="color:#6C7482;">&mdash;</span>'
			: '<span class="' + (dias >= 2 ? 'mal' : '') + '" style="font-weight:'
				+ (dias >= 2 ? '700' : '400') + ';">' + dias + '</span>'
				+ (a.max_pedidos_dia > 2
					? ' <span style="color:#6C7482;font-size:11px;">(hasta ' + a.max_pedidos_dia + ')</span>'
					: '');

		//La concentracion se muestra en gris cuando no hay base suficiente: con 3 pedidos
		//un 100% no significa nada y resaltarlo seria senalar a alguien por ruido.
		var base = a.pedidos_con_persona;
		var fuerte = (a.concentracion >= 70 && base >= 5);
		var pct = base > 0
			? '<span class="' + (fuerte ? 'mal' : '') + '" style="font-weight:' + (fuerte ? '700' : '400') + ';">'
				+ a.concentracion + '%</span> <span style="color:#6C7482;font-size:11px;">('
				+ a.top_pedidos + '/' + base + ')</span>'
			: '<span style="color:#6C7482;">sin dato</span>';

		h += '<tr data-correo="' + afEsc(a.correo) + '"' + (a.sospechoso ? ' style="background:#fff6e0;"' : '') + '>'
			+ '<td>' + (a.nombre ? afEsc(a.nombre) + '<br/><span style="color:#6C7482;font-size:11px;">'
				+ afEsc(a.correo) + '</span>' : afEsc(a.correo)) + '</td>'
			+ '<td class="num">' + a.pedidos + '</td>'
			+ '<td class="num">' + afMiles(a.puntos) + '</td>'
			+ '<td class="num">$' + afMiles(a.valor) + '</td>'
			+ '<td class="cen">' + a.tiendas + '</td>'
			+ '<td>' + (base > 0 ? afEsc(a.top_nombre) : '<span style="color:#6C7482;">sin dato</span>') + '</td>'
			+ '<td class="num">' + pct + '</td>'
			+ '<td class="num">' + diasTxt + '</td>'
			+ '<td>' + senales + '</td>'
			+ '</tr>';
	}

	if (mostradas === 0) {
		afVacio('#afCuerpoAcum', 9, solo
			? 'Ninguna persona qued\u00f3 marcada como sospechosa en este per\u00edodo.'
			: 'No hubo acumulaciones en este per\u00edodo.');
		return;
	}
	$('#afCuerpoAcum').html(h);
	afEngancharClic('#afCuerpoAcum');
}

// ---------------------------------------------------------------------------------------
// El detalle de una persona
// ---------------------------------------------------------------------------------------

function afEngancharClic(selector) {
	$(selector + ' tr').click(function () {
		$(selector + ' tr').removeClass('sel');
		$(this).addClass('sel');
		afVerCliente($(this).attr('data-correo'));
	});
}

function afVerCliente(correo) {
	if (!correo) { return; }
	afCorreoSel = correo;
	$('#afDetalle').html('<div class="af-bloque"><div class="af-vacio">Consultando el historial\u2026</div></div>');

	$.getJSON(server + 'ConsultarAuditoriaFidelizacion', {
		accion: 'CLIENTE',
		correo: correo
	}, function (data) {
		if (!data || data.resultado !== 'OK') {
			$('#afDetalle').html('<div class="af-bloque"><div class="af-vacio">'
				+ afEsc(data && data.mensaje ? data.mensaje : 'No se pudo consultar.') + '</div></div>');
			return;
		}
		afPintarDetalle(data);
	}).fail(function () {
		$('#afDetalle').html('<div class="af-bloque"><div class="af-vacio">No se pudo consultar el historial.</div></div>');
	});
}

function afPintarDetalle(d) {
	var r = d.resumen;
	var s = r.senales || {};
	var h = '';

	h += '<div class="af-bloque">';
	h += '<h4>' + (r.nombre ? afEsc(r.nombre) : afEsc(r.correo)) + '</h4>';
	h += '<div class="nota">' + afEsc(r.correo) + (r.celular ? ' \u00b7 ' + afEsc(r.celular) : '')
		+ (r.vinculado ? ' \u00b7 vinculado el ' + afEsc(r.vinculado) : '') + '</div>';

	//Los avisos van arriba y en orden de gravedad: lo primero que hay que saber es si esta
	//persona es de la casa.
	if (r.es_empleado) {
		h += '<div class="af-aviso af-rojo"><strong>Es empleado.</strong> '
			+ afEsc(r.empleado_nombre) + ' (c\u00e9dula ' + afEsc(r.empleado_cedula) + ')'
			+ (r.empleado_activo ? ', activo en la n\u00f3mina.' : ', ya NO activo en la n\u00f3mina.')
			+ '</div>';
	}
	if (r.auto_redenciones > 0) {
		h += '<div class="af-aviso af-rojo"><strong>' + r.auto_redenciones
			+ ' redenci\u00f3n(es) se las proces\u00f3 \u00e9l mismo.</strong> '
			+ 'Quien atendi\u00f3 el pedido de la redenci\u00f3n es el due\u00f1o de la cuenta.</div>';
	}

	if (r.dias_con_varios >= 2) {
		h += '<div class="af-aviso"><strong>' + r.dias_con_varios
			+ ' d\u00edas con m\u00e1s de un pedido con puntos</strong>'
			+ (r.max_pedidos_dia > 2 ? ', hasta ' + r.max_pedidos_dia + ' en un solo d\u00eda' : '')
			+ '. El detalle est\u00e1 abajo.</div>';
	}

	h += '<div class="af-tarjetas" style="margin-bottom:6px;">';
	h += afTarjeta('Puntos vigentes', afMiles(r.puntos_vigentes), '', false);
	h += afTarjeta('Acumulaciones', r.acumulaciones, afMiles(r.puntos_acumulados) + ' puntos', false);
	h += afTarjeta('Compras', '$' + afMiles(r.valor_acumulado), 'que dieron puntos', false);
	h += afTarjeta('Redenciones', r.redenciones, afMiles(r.puntos_redimidos) + ' puntos', false);
	h += afTarjeta('Tiendas', r.tiendas_distintas, 'donde ha comprado', false);
	if (r.redenciones_reversadas > 0) {
		h += afTarjeta('Reversadas', r.redenciones_reversadas, '', false);
	}
	h += '</div>';
	h += '</div>';

	// --- dias con varios pedidos ------------------------------------------------------
	//Se muestra siempre que haya alguno, aunque sea uno solo: un dia suelto no marca
	//pero el que revisa tiene derecho a verlo.
	var dc = d.dias_cargados || [];
	if (dc.length > 0) {
		h += '<div class="af-bloque">';
		h += '<h4>D\u00edas con m\u00e1s de un pedido con puntos</h4>';
		h += '<div class="nota">' + dc.length + ' d\u00eda(s)'
			+ (r.max_pedidos_dia > 1 ? ', el mayor con ' + r.max_pedidos_dia + ' pedidos' : '')
			+ '. Comprar dos veces en un d\u00eda es normal; lo que vale mirar es que se repita, '
			+ 'y sobre todo si los pedidos del mismo d\u00eda los tom\u00f3 la misma persona.</div>';
		if (r.dias_con_varios >= 2) {
			h += '<div class="af-aviso">Se repite en ' + r.dias_con_varios
				+ ' d\u00edas distintos.</div>';
		}
		h += '<div class="af-scroll" style="max-height:220px;"><table class="af-tabla"><thead><tr>'
			+ '<th>Fecha</th><th class="num">Pedidos</th><th class="num">Puntos</th>'
			+ '<th class="num">Compras</th><th>Tienda(s)</th><th>Qui\u00e9n atendi\u00f3</th>'
			+ '</tr></thead><tbody>';
		for (var k = 0; k < dc.length; k++) {
			var dd = dc[k];
			h += '<tr style="cursor:default;">'
				+ '<td>' + afEsc(dd.fecha) + '</td>'
				+ '<td class="num"><strong>' + dd.pedidos + '</strong></td>'
				+ '<td class="num">' + afMiles(dd.puntos) + '</td>'
				+ '<td class="num">$' + afMiles(dd.valor) + '</td>'
				+ '<td>' + afEsc(dd.tiendas) + '</td>'
				+ '<td>' + (dd.usuarios ? afEsc(dd.usuarios)
					: '<span style="color:#6C7482;">sin registro</span>') + '</td>'
				+ '</tr>';
		}
		h += '</tbody></table></div></div>';
	}

	// --- la concentracion -------------------------------------------------------------
	h += '<div class="af-bloque">';
	h += '<h4>Qui\u00e9n le toma los pedidos</h4>';
	if (!d.atienden || d.atienden.length === 0) {
		h += '<div class="af-vacio">Ninguno de sus pedidos tiene registrado qui\u00e9n atendi\u00f3. '
			+ 'El dato se empez\u00f3 a guardar en septiembre de 2026.</div>';
	} else {
		var base = s.base_concentracion || 0;
		h += '<div class="nota">El porcentaje va sobre ' + base + ' pedido(s) atendidos por una persona'
			+ (s.por_canal > 0 ? ', m\u00e1s ' + s.por_canal + ' por canal (contact center, BOT, app, '
				+ 'tienda virtual), que no cuentan porque no se\u00f1alan a nadie' : '')
			+ '. En total tiene ' + r.acumulaciones + ' acumulaci\u00f3n(es)'
			+ '; las anteriores a septiembre de 2026 no traen qui\u00e9n atendi\u00f3.</div>';
		if (s.concentracion >= 70 && base >= 5) {
			h += '<div class="af-aviso af-rojo">'
				+ '<strong>' + s.concentracion + '% de la atenci\u00f3n la concentra una sola persona</strong> ('
				+ afEsc(s.quien_concentra) + '). A un cliente frecuente leg\u00edtimo lo atiende quien est\u00e9 '
				+ 'en la caja ese d\u00eda, as\u00ed que su atenci\u00f3n se reparte.</div>';
		} else if (s.concentracion >= 50 && base >= 5) {
			h += '<div class="af-aviso">' + s.concentracion + '% de la atenci\u00f3n la concentra '
				+ afEsc(s.quien_concentra) + '. Por debajo del umbral, pero vale mirarlo.</div>';
		}
		for (var i = 0; i < d.atienden.length; i++) {
			var a = d.atienden[i];
			var etq = '';
			if (a.es_el_mismo_cliente) { etq = ' <span class="etq etq-auto">\u00c9L MISMO</span>'; }
			else if (a.es_canal) { etq = ' <span class="etq etq-rev">CANAL</span>'; }
			h += '<div class="af-fila">'
				+ '<div class="et" title="' + afEsc(a.nombre) + '">' + afEsc(a.nombre) + etq + '</div>'
				+ '<div class="bar"><div class="af-barra"><i class="' + (a.porcentaje >= 70 ? 'rojo' : '')
					+ '" style="width:' + Math.max(a.porcentaje, 1) + '%;"></i></div></div>'
				+ '<div class="cif">' + a.pedidos + (a.es_canal ? '' : ' (' + a.porcentaje + '%)') + '</div>'
				+ '</div>';
		}
	}
	h += '</div>';

	// --- las dos tablas en pestanas ---------------------------------------------------
	h += '<div class="af-bloque">';
	h += '<ul class="af-tabs">'
		+ '<li class="activo"><a data-tab="acum">Acumulaciones (' + (d.acumulaciones || []).length + ')</a></li>'
		+ '<li><a data-tab="red">Redenciones (' + (d.redenciones || []).length + ')</a></li>'
		+ '</ul>';
	h += '<div class="af-panel activo" data-panel="acum">' + afTablaAcumulaciones(d.acumulaciones) + '</div>';
	h += '<div class="af-panel" data-panel="red">' + afTablaRedenciones(d.redenciones) + '</div>';
	h += '</div>';

	$('#afDetalle').html(h);

	$('#afDetalle .af-tabs a').click(function () {
		var tab = $(this).attr('data-tab');
		$('#afDetalle .af-tabs li').removeClass('activo');
		$(this).parent().addClass('activo');
		$('#afDetalle .af-panel').removeClass('activo');
		$('#afDetalle .af-panel[data-panel="' + tab + '"]').addClass('activo');
	});
}

function afTablaAcumulaciones(filas) {
	if (!filas || filas.length === 0) {
		return '<div class="af-vacio">Sin acumulaciones.</div>';
	}
	var h = '<div class="af-scroll" style="max-height:340px;"><table class="af-tabla"><thead><tr>'
		+ '<th>Fecha</th><th>Tienda</th><th class="num">Pedido</th><th class="num">Compra</th>'
		+ '<th class="num">Puntos</th><th>Qui\u00e9n atendi\u00f3</th></tr></thead><tbody>';
	for (var i = 0; i < filas.length; i++) {
		var a = filas[i];
		h += '<tr style="cursor:default;">'
			+ '<td>' + afEsc(a.fecha) + '</td>'
			+ '<td>' + afEsc(a.tienda) + '</td>'
			+ '<td class="num">' + a.idpedidotienda + '</td>'
			+ '<td class="num">$' + afMiles(a.valor_neto) + '</td>'
			+ '<td class="num">' + afMiles(a.puntos)
				+ (a.vencidos ? ' <span class="etq etq-rev">VENC</span>' : '') + '</td>'
			+ '<td>' + afQuien(a.usuario_nombre, a.usuario) + '</td>'
			+ '</tr>';
	}
	return h + '</tbody></table></div>';
}

function afTablaRedenciones(filas) {
	if (!filas || filas.length === 0) {
		return '<div class="af-vacio">Sin redenciones.</div>';
	}
	var h = '<div class="af-scroll" style="max-height:340px;"><table class="af-tabla"><thead><tr>'
		+ '<th>Fecha y hora</th><th>Tienda</th><th class="num">Pedido</th><th class="num">Puntos</th>'
		+ '<th>Qui\u00e9n atendi\u00f3</th><th>Estado</th></tr></thead><tbody>';
	for (var i = 0; i < filas.length; i++) {
		var r = filas[i];
		h += '<tr style="cursor:default;">'
			+ '<td>' + afEsc(r.fecha) + '</td>'
			+ '<td>' + afEsc(r.tienda) + '</td>'
			+ '<td class="num">' + (r.idpedidotienda > 0 ? r.idpedidotienda : '&mdash;') + '</td>'
			+ '<td class="num">' + afMiles(r.puntos) + '</td>'
			+ '<td>' + afQuien(r.usuario_nombre, r.usuario)
				+ (r.auto_redencion ? ' <span class="etq etq-auto">AUTO</span>' : '') + '</td>'
			+ '<td>' + afEsc(r.estado) + '</td>'
			+ '</tr>';
	}
	return h + '</tbody></table></div>';
}

// ---------------------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------------------

function afTarjeta(titulo, valor, pie, alerta) {
	return '<div class="af-tarjeta' + (alerta ? ' alerta' : '') + '">'
		+ '<div class="k">' + titulo + '</div>'
		+ '<div class="v' + (alerta ? ' mal' : '') + '">' + valor + '</div>'
		+ (pie ? '<div class="p">' + pie + '</div>' : '')
		+ '</div>';
}

function afVacio(selector, columnas, mensaje) {
	$(selector).html('<tr><td colspan="' + columnas + '" class="af-vacio">' + afEsc(mensaje) + '</td></tr>');
}

/** Quien atendio: el nombre si resolvio, si no el id crudo, y si no hay nada lo dice. */
function afQuien(nombre, usuario) {
	if (nombre) { return afEsc(nombre); }
	if (usuario) { return afEsc(usuario); }
	return '<span style="color:#6C7482;">sin registro</span>';
}

/**
 * Formato colombiano: punto para los miles y coma para los decimales.
 *
 * Los puntos pueden traer decimales -puntos_vigentes es double y se han visto valores como
 * 30,5-, asi que el decimal se muestra solo cuando existe; si no, un entero limpio.
 */
function afMiles(n) {
	var v = Number(n || 0);
	var tieneDecimal = Math.abs(v - Math.round(v)) > 0.001;
	var texto = Math.abs(v).toFixed(tieneDecimal ? 1 : 0);
	var partes = texto.split('.');
	partes[0] = partes[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
	return (v < 0 ? '-' : '') + partes[0] + (partes.length > 1 ? ',' + partes[1] : '');
}

/** Escapa lo que venga de la base: los correos y los nombres los escribe gente. */
function afEsc(texto) {
	if (texto === null || texto === undefined) { return ''; }
	return String(texto)
		.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
