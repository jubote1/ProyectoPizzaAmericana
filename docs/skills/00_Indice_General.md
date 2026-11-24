---
title: "Índice General y Arquitectura - Reemplazo Tienda Virtual"
tags:
  - pizza-americana
  - tienda-virtual
  - arquitectura
  - gloria-food
  - obsidian-vault
date: 2026-10-28
status: Completado / Verificado
---

# 🍕 Reemplazo Tienda Virtual Pizza Americana (Gloria Food / Kuno)

## 1. Visión y Objetivo del Proyecto
Pizza Americana opera actualmente su tienda virtual a través de la plataforma de terceros **Gloria Food** (conocida internamente en el código como **Kuno** / `mirestauranteenlinea.com`). Oracle ha anunciado el **retiro definitivo del servicio para el 30 de abril de 2027** `[Capturas]`.

El objetivo fundamental es **construir una tienda virtual propia, moderna, interactiva y robusta** que:
1. **Conserve el flujo funcional validado:** Conservar la toma de pedidos, asignación por cobertura geográfica y despacho hacia las tiendas físicas.
2. **Elimine la fricción operativa actual:** En Gloria Food se debían configurar y mantener **11 menús separados a mano**, además de duplicar bloqueos de productos y horarios.
3. **Establezca una fuente de verdad unificada:** Tomar el modelo del **POS de las tiendas** (`tiendaamericana`) como el estándar más maduro y completo, implementando un **menú base de marca con excepciones por tienda**.
4. **Centralice la disponibilidad:** Diseñar un motor de bloqueos (por tienda, producto, categoría y canal) con validación estricta en el servidor.

---

## 2. Convención de Confianza de la Documentación
En todas las especificaciones y skills de este repositorio se aplica la convención de rigor técnico:
* **`[Verificado]`**: Constatado directamente en código Java (`ProyectoPizzaAmericana`, `Servicios`, `ProyectoTiendaAmericana`) o esquemas y consultas en bases de datos reales (`pizzaamericana`, `tiendaamericana`, `geolocalizacion`).
* **`[Capturas]`**: Validado mediante capturas de pantalla de la configuración real y operación en Gloria Food aportadas por el equipo.
* **`[Deducido]`**: Inferencia arquitectónica de ingeniería explicada con fundamento técnico.
* **`[Pendiente]`**: Asunto pendiente de decisión final de producto o negocio.

---

## 3. Mapa de Arquitectura del Ecosistema

```mermaid
graph TD
    subgraph Cliente ["Canales de Entrada"]
        Web["Tienda Virtual Propia (Web / PWA)"]
        WP["WordPress (pizzaamericana.co)"]
        WP -->|Redirección Única| Web
    end

    subgraph ServiciosExternos ["Servicios y Proveedores"]
        ArcGIS["ArcGIS Online (FeatureServer Polígonos)"]
        Wompi["Pasarela Wompi (Checkout / Webhooks)"]
        Brevo["Brevo / Ultramsg (WhatsApp & Mail)"]
    end

    subgraph Central ["Servidor Central (172.19.0.25)"]
        API["Backend Central (Java Servlets / REST)"]
        BDPizza[("BD MySQL: pizzaamericana")]
        BDGeoloc[("BD MySQL: geolocalizacion")]
        BDServicios["Daemons: Proyecto Servicios"]
        
        API --> BDPizza
        API --> BDGeoloc
        BDServicios --> BDPizza
    end

    subgraph Tiendas ["11 Tiendas Físicas (Red Local / VPN)"]
        T1["Tienda Manrique (172.19.1.80)"]
        T2["Tienda Bello (192.168.2.20)"]
        T3["Tienda América (192.168.3.38)"]
        T4["Tienda Calasanz (192.168.4.10)"]
        T5["Tienda Itagüí (192.168.5.3)"]
        T7["Tienda La Mota (192.168.7.19)"]
        T8["Tienda Envigado (192.168.8.29)"]
        T9["Tienda Pilarica (192.168.9.39)"]
        T10["Tienda San Antonio (192.168.10.49)"]
        T11["Tienda Manrique Piloto (172.19.11.69)"]
        T13["Tienda Niquía (172.19.12.19)"]
        
        POSTienda[("BD MySQL POS: tiendaamericana")]
    end

    Web -->|1. Cobertura & Polígonos| ArcGIS
    Web -->|2. Creación Pedido| API
    Web -->|3. Pago Directo| Wompi
    Wompi -->|Webhook de Aprobación| API
    API -->|Notificaciones| Brevo
    API -->|Inyección de Pedido (Turnos Atómicos)| POSTienda
    BDServicios -->|Monitoreo y Reintentos| POSTienda
```

---

## 4. Mapa de Navegación de las Skills (Estructura Vault)

Cada documento constituye una guía técnica y operativa autosuficiente para el equipo de desarrollo:

| # | Skill / Documento | Archivo Obsidian | Descripción Central |
|---|---|---|---|
| **00** | **Índice General y Arquitectura** | `[[00_Indice_General]]` | Mapa general, glosario, convenciones y visión integral. |
| **01** | **Catálogo y Menú POS** | `[[01_Catalogo_y_Menu_POS]]` | Modelado del catálogo desde el POS de tienda (`tiendaamericana`). Jerarquías, tipos `P`, `D`, `A`, `O`, modificadores `MC` ("Con") y `MS` ("Sin"), recargos en `precio10`, Mitad y Mitad (`permite_dividir = 2`) y herencia por tienda. |
| **02** | **Cobertura, Zonas y Domicilio** | `[[02_Cobertura_Zonas_y_Domicilio]]` | Entrada única sin selector inicial de tienda. Georreferenciación ArcGIS, polígonos por tienda, tarifa plana de domicilio ($5.000) y pedido mínimo ($17.000). |
| **03** | **Disponibilidad, Horarios y Bloqueos** | `[[03_Disponibilidad_Horarios_y_Bloqueos]]` | Matriz de disponibilidad. Desfase de apertura de 30 min para cocina/domicilio, excepciones de fecha, bloqueos inmediatos/programados por canal y validación de seguridad en servidor. |
| **04** | **Pedidos Programados y Ciclo de Vida** | `[[04_Pedidos_Programados_y_Ciclo_de_Vida]]` | Reglas de pedidos programados (mismo día, mínimo 1h antelación, franjas de 30 min), pedidos fuera de horario y máquina de estados (`PEN`, `PRO`, `ERR`). |
| **05** | **Checkout, Pagos y Wompi** | `[[05_Checkout_Pagos_y_Wompi]]` | Métodos activos (Efectivo a domicilio, Wompi directo, PayU en franquicias). Integración de pasarela embebida, webhook de respuesta y conciliación financiera. |
| **06** | **Integración y Envío a Tienda (POS)** | `[[06_Integracion_Envio_a_Tienda_POS]]` | Protocolo de inyección al POS local, turnos atómicos anti-duplicados (`tomarTurno`), líneas maestras, separadores mitad y mitad (`10000`) y contingencias. |

---

## 5. Glosario Unificado del Negocio

* **Pizzeta (PZ):** Tamaño individual / 4 porciones (`idpregunta = 6`, base $21.000).
* **Mediana (MD):** Tamaño mediano / 6 porciones (`idpregunta = 5`, base $39.000).
* **Grande (GD):** Tamaño familiar / 8 porciones (`idpregunta = 2`, base $48.000).
* **Extragrande (XL):** Tamaño gigante / 12 porciones (`idpregunta = 4`, base $57.000).
* **Especialidad (`tipo_producto = D`):** Sabor de la pizza (Hawaiana, Carnis, Peperoni y Queso, Pollo BBQ, Piñaronni, etc.).
* **Mitad y Mitad:** Pizza configurada con dos especialidades. Se activa cuando `pregunta.permite_dividir = 2`. Cada mitad adopta la mitad del recargo de especialidad (`precio10 / 2`).
* **Modificador "CON" (`tipo_producto = MC`):** Adición de ingrediente extra dentro de la receta (ej. "Con Tocineta XG"), mapeado en `producto_modificador_con`.
* **Modificador "SIN" (`tipo_producto = MS`):** Retiro explícito de un ingrediente base (ej. "Sin Cebolla XG", "Sin Piña XG"), mapeado en `producto_modificador_sin`.
* **Excepción de Precio (`excepcion_precio`):** Mecanismo de promociones, ofertas especiales y combos (ej. Insuperable, 2x1, Mediana MAX).
* **Pedido Programado:** Orden solicitada para una hora futura del mismo día (`for_later = true`), cumpliendo mínimo 1 hora de antelación.
* **Turno de Envío (`tomarTurno`):** Candado transaccional en `capaDAOCC.EnvioTiendaDAO` para impedir que una orden se facture más de una vez en la tienda.
