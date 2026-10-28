/**
 * Pinta el menu (modulo -> submodulo -> pantallas) que GetMenuUsuario devuelve
 * para el usuario logueado, segun sus roles. Es lo que usan Menu.html,
 * MenuAdm.html y MenuPQRS.html: los tres son ahora el mismo fragmento, porque
 * el contenido sale de la base de datos (administracion de permisos) y no del
 * HTML.
 *
 * Si el servicio no responde o devuelve el menu vacio (por ejemplo, la
 * migracion sql/seguridad_menus_reorganizacion.sql aun no se ha corrido),
 * se cae al menu estatico de siempre -MenuLegacy.html, MenuAdmLegacy.html o
 * MenuPQRSLegacy.html, segun lo que declare cada fragmento en
 * window.MENU_LEGADO-: nadie se queda sin navegacion por un problema del menu
 * dinamico.
 */
(function() {
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	var server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));
	var legado = window.MENU_LEGADO;
	window.MENU_LEGADO = null;

	function enlace(pantalla) {
		return $('<a class="dropdown-item"></a>').attr('href', pantalla.url_html).text(pantalla.nombre);
	}

	function pintar(modulos) {
		var crm = $('#opcion-crm');
		$('#menu-dinamico-modulos > li.menu-dinamico-item').remove();
		var items = [];
		items.push($('<li class="nav-item active menu-dinamico-item"><a class="nav-link" href="http://www.pizzaamericana.co">Pagina Web<span class="sr-only">(current)</span></a></li>'));
		for (var m = 0; m < modulos.length; m++) {
			var modulo = modulos[m];
			var idDropdown = 'navbarDropdownDinamico' + modulo.idmodulo;
			var li = $('<li class="nav-item dropdown menu-dinamico-item"></li>');
			li.append($('<a class="nav-link dropdown-toggle" href="#" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"></a>')
				.attr('id', idDropdown).text(modulo.nombre));
			// Un modulo con muchos submodulos pasa de alto de pantalla: que se desplace.
			var dropdown = $('<div class="dropdown-menu" style="max-height:80vh; overflow-y:auto;"></div>').attr('aria-labelledby', idDropdown);
			var pantallas = modulo.pantallas || [];
			var submodulos = modulo.submodulos || [];
			for (var p = 0; p < pantallas.length; p++) {
				dropdown.append(enlace(pantallas[p]));
			}
			for (var s = 0; s < submodulos.length; s++) {
				if (dropdown.children().length > 0) {
					dropdown.append('<div class="dropdown-divider"></div>');
				}
				dropdown.append($('<h6 class="dropdown-header"></h6>').text(submodulos[s].nombre));
				var pantallasSub = submodulos[s].pantallas || [];
				for (var q = 0; q < pantallasSub.length; q++) {
					dropdown.append(enlace(pantallasSub[q]));
				}
			}
			li.append(dropdown);
			items.push(li);
		}
		for (var i = 0; i < items.length; i++) {
			if (crm.length > 0) {
				items[i].insertBefore(crm);
			} else {
				$('#menu-dinamico-modulos').append(items[i]);
			}
		}
	}

	function usarMenuEstatico() {
		if (!legado) {
			return;
		}
		$.ajax({
			url: server + legado,
			dataType: 'html',
			async: false,
			cache: false,
			success: function(html) {
				$('#menuprincipal').replaceWith(html);
			}
		});
	}

	$.ajax({
		url: server + 'GetMenuUsuario',
		dataType: 'json',
		type: 'post',
		async: false,
		success: function(modulos) {
			if (modulos && modulos.length > 0) {
				pintar(modulos);
			} else {
				usarMenuEstatico();
			}
		},
		error: function() {
			usarMenuEstatico();
		}
	});
})();
