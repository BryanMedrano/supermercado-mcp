CREATE DATABASE IF NOT EXISTS supermercado_mcp
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE supermercado_mcp;

CREATE TABLE IF NOT EXISTS producto (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        nombre VARCHAR(120) NOT NULL,
    categoria VARCHAR(60) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    unidad VARCHAR(30) NOT NULL
    );

CREATE TABLE IF NOT EXISTS pedido (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      cliente_nombre VARCHAR(120) NOT NULL,
    direccion_entrega VARCHAR(200) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    fecha_creacion DATETIME NOT NULL
    );

CREATE TABLE IF NOT EXISTS detalle_pedido (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              pedido_id BIGINT NOT NULL,
                                              producto_id BIGINT NOT NULL,
                                              cantidad INT NOT NULL,
                                              precio_unitario DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
    );

INSERT INTO producto (nombre, categoria, precio, stock, unidad) VALUES
                                                                    ('Arroz Blanco Premium 1kg', 'Despensa', 4800.00, 50, 'bolsa'),
                                                                    ('Leche Entera Deslactosada 1L', 'Lácteos', 4200.00, 40, 'caja'),
                                                                    ('Huevos Rojos AA x 30', 'Lácteos y Huevos', 16500.00, 25, 'panal'),
                                                                    ('Aceite Vegetal 900ml', 'Despensa', 9200.00, 30, 'botella'),
                                                                    ('Pechuga de Pollo Fresca 1kg', 'Carnes', 15800.00, 20, 'bandeja'),
                                                                    ('Banano Criollo 1kg', 'Frutas y Verduras', 3200.00, 35, 'kg'),
                                                                    ('Tomate Chonto 1kg', 'Frutas y Verduras', 4500.00, 30, 'kg'),
                                                                    ('Café Molido Tradicional 500g', 'Despensa', 14900.00, 22, 'paquete'),
                                                                    ('Pan Tajado Integral 600g', 'Panadería', 6400.00, 18, 'bolsa'),
                                                                    ('Detergente Líquido 2L', 'Aseo Hogar', 18500.00, 15, 'botella');