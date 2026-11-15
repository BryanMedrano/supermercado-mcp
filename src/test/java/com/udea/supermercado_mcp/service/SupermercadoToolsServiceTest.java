package com.udea.supermercado_mcp.service;

import com.udea.supermercado_mcp.model.Pedido;
import com.udea.supermercado_mcp.model.Producto;
import com.udea.supermercado_mcp.repository.PedidoRepository;
import com.udea.supermercado_mcp.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupermercadoToolsServiceTest {

    @Mock
    private ProductoRepository productoRepo;

    @Mock
    private PedidoRepository pedidoRepo;

    @InjectMocks
    private SupermercadoToolsService service;

    private Producto arroz;
    private Producto leche;

    @BeforeEach
    void setUp() {
        arroz = new Producto();
        arroz.setId(1L);
        arroz.setNombre("Arroz Blanco Premium 1kg");
        arroz.setCategoria("Despensa");
        arroz.setPrecio(new BigDecimal("4800.00"));
        arroz.setStock(50);
        arroz.setUnidad("bolsa");

        leche = new Producto();
        leche.setId(2L);
        leche.setNombre("Leche Entera Deslactosada 1L");
        leche.setCategoria("Lácteos");
        leche.setPrecio(new BigDecimal("4200.00"));
        leche.setStock(40);
        leche.setUnidad("caja");
    }

    @Test
    void listarProductosDisponibles_debeRetornarProductosConStockMayorACero() {
        when(productoRepo.findByStockGreaterThan(0)).thenReturn(List.of(arroz, leche));

        List<Producto> resultado = service.listarProductosDisponibles();

        assertEquals(2, resultado.size());
        verify(productoRepo).findByStockGreaterThan(0);
    }

    @Test
    void crearPedidoDomicilio_debeCalcularTotalYDescontarStock() {
        when(productoRepo.findById(1L)).thenReturn(Optional.of(arroz));
        when(productoRepo.findById(2L)).thenReturn(Optional.of(leche));
        when(pedidoRepo.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        Pedido pedido = service.crearPedidoDomicilio(
                "Bryan Medrano",
                "Calle 67 # 53-108",
                "3001234567",
                "1:2,2:3"
        );

        assertEquals(10L, pedido.getId());
        assertEquals("CONFIRMADO", pedido.getEstado());
        // (4800 * 2) + (4200 * 3) = 9600 + 12600 = 22200.00
        assertEquals(new BigDecimal("22200.00"), pedido.getTotal());
        assertEquals(2, pedido.getDetalles().size());
        assertEquals(48, arroz.getStock());
        assertEquals(37, leche.getStock());
    }

    @Test
    void crearPedidoDomicilio_debeRechazarCuandoNoHayStockSuficiente() {
        arroz.setStock(1);
        when(productoRepo.findById(1L)).thenReturn(Optional.of(arroz));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.crearPedidoDomicilio("Bryan Medrano", "Calle 67 # 53-108", "3001234567", "1:5")
        );

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(pedidoRepo, never()).save(any());
    }

    @Test
    void crearPedidoDomicilio_debeRechazarDatosClienteVacios() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearPedidoDomicilio("", "Calle 67 # 53-108", "3001234567", "1:1")
        );
        verify(pedidoRepo, never()).save(any());
    }

    @Test
    void cancelarPedido_debeCambiarEstadoYRestaurarStock() {
        when(productoRepo.findById(1L)).thenReturn(Optional.of(arroz));
        when(pedidoRepo.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(15L);
            return p;
        });

        Pedido pedido = service.crearPedidoDomicilio("Bryan Medrano", "Calle 67", "3001234567", "1:4");
        assertEquals(46, arroz.getStock());

        when(pedidoRepo.findById(15L)).thenReturn(Optional.of(pedido));
        Pedido cancelado = service.cancelarPedido(15L);

        assertEquals("CANCELADO", cancelado.getEstado());
        assertEquals(50, arroz.getStock());
    }
}