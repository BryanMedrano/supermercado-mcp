# Servidor MCP de Supermercado a Domicilio (`supermercado-mcp`)

Servidor **Model Context Protocol (MCP)** desarrollado en **Java 21** con **Spring Boot 3.4.7**, **Spring AI 1.1.4** y persistencia en **MySQL 8**. Expone 7 herramientas transaccionales (`@Tool`) sobre transporte **Streamable HTTP** (`http://localhost:8088/mcp`) para consultar el catálogo de productos, verificar existencias, registrar pedidos a domicilio con descuento automático de inventario y gestionar cancelaciones desde clientes conversacionales como [`chatbot-supermercado-mcp`](https://github.com/BryanMedrano/chatbot-supermercado-mcp).

---

## Arquitectura y Modelo de Datos (`supermercado_mcp`)

- **Protocolo:** JSON-RPC 2.0 sobre Streamable HTTP (`/mcp`, puerto `8088`) registrado mediante `MethodToolCallbackProvider`.
- **Persistencia:** Spring Data JPA / Hibernate sobre el esquema relacional `supermercado_mcp` compuesto por tres tablas normalizadas:
  - **`producto`:** Catálogo del supermercado con `id`, `nombre`, `categoria`, `precio`, `stock` y `unidad`.
  - **`pedido`:** Cabecera de órdenes a domicilio con `cliente_nombre`, `direccion_entrega`, `telefono`, `estado` (`CONFIRMADO` / `CANCELADO`), `total` y `fecha_creacion`.
  - **`detalle_pedido`:** Líneas de detalle asociadas a cada orden (`pedido_id`, `producto_id`, `cantidad`, `precio_unitario`, `subtotal`).
- **Integridad Transaccional:** Operaciones de creación y cancelación ejecutadas dentro de transacciones atómicas (`@Transactional`) que validan existencias, calculan totales exactos y actualizan el inventario en tiempo real.

---

## Herramientas MCP Expuestas (7 Tools)

| Herramienta | Descripción |
| :--- | :--- |
| `listAllProductosDisponible` | Lista todos los productos disponibles en el supermercado con existencias (`stock > 0`). |
| `findProductos` | Busca productos por nombre o palabra clave (ej. `arroz`, `leche`, `pollo`). |
| `filterProductos` | Filtra productos por categoría (`Despensa`, `Lácteos`, `Carnes`, `Frutas y Verduras`, `Panadería`, `Aseo Hogar`). |
| `crearPedido` | Crea un pedido a domicilio (`@Transactional`), valida stock, calcula el total y descuenta inventario. |
| `findDetallesProducto` | Consulta los detalles, productos y estado de un pedido a domicilio por su ID. |
| `listPedidoCliente` | Lista todos los pedidos realizados por un cliente buscando por su nombre. |
| `cancelarPedido` | Cancela un pedido por su ID y devuelve automáticamente las unidades al stock del supermercado. |

---

## Requisitos Previos

- **Java JDK 21** (con la variable de entorno `JAVA_HOME` configurada y agregada al `PATH`).
- **MySQL Server 8.0+** activo en `localhost:3306` con usuario `root` (o un usuario con permisos de creación y escritura en `supermercado_mcp`).
- **Variable de entorno `PWD_UDEA`** definida en el sistema o sesión de terminal con la contraseña de acceso a MySQL.
- **Puerto `8088` libre** en el equipo local (`localhost:8088`).
- **Acceso a Internet** (en la primera compilación) para descargar las dependencias de Spring Boot `3.4.7` y Spring AI `1.1.4` desde Maven Central y Spring Milestones.
- **Maven Wrapper** incluido en el repositorio (`.\mvnw.cmd` en Windows o `./mvnw` en Linux/macOS).
- **Cliente MCP compatible** para consumir el servidor:
  - **Agente ChatBot [`chatbot-supermercado-mcp`](https://github.com/BryanMedrano/chatbot-supermercado-mcp)** ejecutándose en el puerto `8080`.
  - **Node.js 18+ y `npx`** (opcional, si se desea depurar con **MCP Inspector**).

---

## Configuración y Ejecución

1. **Clonar el repositorio:**
   ```powershell
   git clone https://github.com/BryanMedrano/supermercado-mcp.git
   cd supermercado-mcp
   ```

2. **Crear la base de datos y cargar el catálogo inicial en MySQL:**
   Ejecuta el script `src/main/resources/schema.sql` en MySQL Workbench o desde consola para crear el esquema `supermercado_mcp`, las tablas `producto`, `pedido`, `detalle_pedido` y los 10 productos iniciales:
   ```powershell
   mysql -u root -p < src/main/resources/schema.sql
   ```

3. **Configurar la variable de entorno con la contraseña de MySQL:**
   ```powershell
   $env:PWD_UDEA = "TU_CONTRASENA_MYSQL"
   ```

4. **Ejecutar pruebas unitarias y de contexto (usa base de datos H2 en memoria):**
   ```powershell
   .\mvnw.cmd test
   ```

5. **Iniciar el servidor MCP:**
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   El servidor quedará activo en `http://localhost:8088/mcp` mostrando en consola `Registered tools: 7`.

---
