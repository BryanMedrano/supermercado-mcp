package com.udea.supermercado_mcp.service;

import com.udea.supermercado_mcp.model.DetallePedido;
import com.udea.supermercado_mcp.model.Pedido;
import com.udea.supermercado_mcp.model.Producto;
import com.udea.supermercado_mcp.repository.PedidoRepository;
import com.udea.supermercado_mcp.repository.ProductoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupermercadoToolsService {

    private final ProductoRepository productoRepo;
    private final PedidoRepository pedidoRepo;

    public SupermercadoToolsService(ProductoRepository productoRepo, PedidoRepository pedidoRepo) {
        this.productoRepo = productoRepo;
        this.pedidoRepo = pedidoRepo;
    }

    @Tool(name= "listAllProductosDisponible", description = "Lista todos los productos disponibles en el supermercado con su ID, nombre, categoría, precio, unidad y stock disponible.")
    public List<Producto> listarProductosDisponibles() {
        return productoRepo.findByStockGreaterThan(0);
    }

    @Tool(name= "findProductos", description = "Busca productos por nombre o palabra clave (por ejemplo: 'arroz', 'leche', 'pollo').")
    public List<Producto> buscarProductos(
            @ToolParam(description = "Nombre o término de búsqueda del producto") String termino) {
        return productoRepo.findByNombreContainingIgnoreCase(termino.trim());
    }

    @Tool(name= "filterProductos", description = "Filtra productos por categoría (Despensa, Lácteos, Carnes, Frutas y Verduras, Panadería, Aseo Hogar).")
    public List<Producto> listarPorCategoria(
            @ToolParam(description = "Nombre de la categoría") String categoria) {
        return productoRepo.findByCategoriaIgnoreCase(categoria.trim());
    }

    @Tool(name= "crearPedido", description = "Crea un nuevo pedido a domicilio, valida stock, calcula el total y descuenta inventario. El parámetro items debe tener el formato 'productoId:cantidad,productoId:cantidad' (ejemplo: '1:2,3:1').")
    @Transactional
    public Pedido crearPedidoDomicilio(
            @ToolParam(description = "Nombre completo del cliente") String clienteNombre,
            @ToolParam(description = "Dirección completa de entrega a domicilio") String direccionEntrega,
            @ToolParam(description = "Teléfono de contacto del cliente") String telefono,
            @ToolParam(description = "Lista de ítems en formato 'productoId:cantidad' separados por coma, ej. '1:2,5:1'") String items) {

        if (clienteNombre == null || clienteNombre.isBlank() ||
                direccionEntrega == null || direccionEntrega.isBlank() ||
                telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente, la dirección de entrega y el teléfono son obligatorios.");
        }
        if (items == null || items.isBlank()) {
            throw new IllegalArgumentException("Debe incluir al menos un producto en el pedido.");
        }

        Pedido pedido = new Pedido();
        pedido.setClienteNombre(clienteNombre.trim());
        pedido.setDireccionEntrega(direccionEntrega.trim());
        pedido.setTelefono(telefono.trim());
        pedido.setEstado("CONFIRMADO");
        pedido.setFechaCreacion(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        String[] pares = items.split(",");

        for (String par : pares) {
            String[] partes = par.trim().split(":");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Formato de ítem inválido: '" + par + "'. Use 'productoId:cantidad'.");
            }
            Long productoId = Long.parseLong(partes[0].trim());
            int cantidad = Integer.parseInt(partes[1].trim());
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero para el producto ID " + productoId);
            }

            Producto producto = productoRepo.findById(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));

            if (producto.getStock() < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente para '" + producto.getNombre() +
                        "'. Disponible: " + producto.getStock() + ", solicitado: " + cantidad);
            }

            producto.setStock(producto.getStock() - cantidad);
            productoRepo.save(producto);

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            total = total.add(subtotal);

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotal);
            pedido.getDetalles().add(detalle);
        }

        pedido.setTotal(total);
        return pedidoRepo.save(pedido);
    }

    @Tool(name= "findDetallesProducto", description = "Consulta los detalles, productos y estado de un pedido a domicilio por su ID.")
    public Pedido consultarPedidoPorId(
            @ToolParam(description = "Identificador numérico del pedido") Long pedidoId) {
        return pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe un pedido con ID: " + pedidoId));
    }

    @Tool(name= "listPedidoCliente", description = "Lista todos los pedidos realizados por un cliente buscando por su nombre.")
    public List<Pedido> listarPedidosPorCliente(
            @ToolParam(description = "Nombre del cliente a consultar") String clienteNombre) {
        return pedidoRepo.findByClienteNombreContainingIgnoreCase(clienteNombre.trim());
    }

    @Tool(name= "cancelarPedido", description = "Cancela un pedido a domicilio por su ID y devuelve automáticamente las unidades al stock del supermercado.")
    @Transactional
    public Pedido cancelarPedido(
            @ToolParam(description = "ID del pedido a cancelar") Long pedidoId) {
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe un pedido con ID: " + pedidoId));

        if ("CANCELADO".equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalArgumentException("El pedido #" + pedidoId + " ya se encuentra cancelado.");
        }

        for (DetallePedido detalle : pedido.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepo.save(producto);
        }

        pedido.setEstado("CANCELADO");
        return pedidoRepo.save(pedido);
    }
}