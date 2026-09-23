/**
 * Pinta el menu (modulo -> pantallas) que GetMenuUsuario devuelve para el
 * usuario logueado, segun sus roles. Reemplaza los 3 archivos
 * Menu.html/MenuAdm.html/MenuPQRS.html estaticos: el mismo fragmento sirve
 * para cualquier rol, porque el contenido sale de la base de datos y no del
 * HTML.
 *
 * Este archivo todavia NO esta enlazado desde ninguna pantalla existente
 * -las ~90 pantallas actuales siguen cargando Menu.html/MenuAdm.html/
 * MenuPQRS.html como siempre-. Es intencional: cambiar esa referencia en
 * todas las pantallas a la vez es un cambio mecanico pero de alto impacto
 * (rompe la navegacion para todo el mundo si algo falla), y se hace aparte
 * una vez este validado con datos reales.
 */
(function() {
	var loc = window.location;
	var pathName = loc.pathname.substring(0, loc.pathname.lastIndexOf('/') + 1);
	var server = loc.href.substring(0, loc.href.length - ((loc.pathname + loc.search + loc.hash).length - pathName.length));

	$.ajax({
		url: server + 'GetMenuUsuario',
		dataType: 'json',
		type: 'post',
		async: false,
		success: function(modulos) {
			var contenedor = $('#menu-dinamico-modulos');
			contenedor.empty();
			contenedor.append('<li class="nav-item active"><a class="nav-link" href="http://www.pizzaamericana.co">Pagina Web<span class="sr-only">(current)</span></a></li>');
			for (var m = 0; m < modulos.length; m++) {
				var modulo = modulos[m];
				var idDropdown = 'navbarDropdownDinamico' + modulo.idmodulo;
				var li = $('<li class="nav-item dropdown"></li>');
				li.append('<a class="nav-link dropdown-toggle" href="#" id="' + idDropdown + '" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">' + modulo.nombre + '</a>');
				var dropdown = $('<div class="dropdown-menu" aria-labelledby="' + idDropdown + '"></div>');
				for (var p = 0; p < modulo.pantallas.length; p++) {
					var pantalla = modulo.pantallas[p];
					dropdown.append('<a class="dropdown-item" href="' + pantalla.url_html + '">' + pantalla.nombre + '</a>');
				}
				li.append(dropdown);
				contenedor.append(li);
			}
		}
	});
})();
