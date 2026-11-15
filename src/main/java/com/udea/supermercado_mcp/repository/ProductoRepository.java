package com.udea.supermercado_mcp.repository;


import com.udea.supermercado_mcp.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    List<Producto> findByCategoriaIgnoreCase(String categoria);
    List<Producto> findByStockGreaterThan(Integer stockMinimo);
}
