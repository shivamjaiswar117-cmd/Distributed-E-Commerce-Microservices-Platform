package com.distributed.ecommerce.product.service;

import com.distributed.ecommerce.product.entity.Product;
import com.distributed.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_shouldSaveAndReturnProduct() {
        Product inputProduct = new Product("Keyboard", "Mechanical keyboard", new BigDecimal("2500.00"), 20);
        Product savedProduct = new Product("Keyboard", "Mechanical keyboard", new BigDecimal("2500.00"), 20);
        savedProduct.setId(5L);

        when(productRepository.save(inputProduct)).thenReturn(savedProduct);

        Product result = productService.createProduct(inputProduct);

        assertEquals(5L, result.getId());
        assertEquals("Keyboard", result.getName());
        verify(productRepository, times(1)).save(inputProduct);
    }

    @Test
    void getAllProducts_shouldReturnAllProductsFromRepository() {
        Product product1 = new Product("Laptop", "Gaming laptop", new BigDecimal("75000.00"), 10);
        Product product2 = new Product("Mouse", "Wireless mouse", new BigDecimal("999.00"), 50);

        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        List<Product> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals("Laptop", result.get(0).getName());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_shouldReturnProduct_whenProductExists() {
        Product product = new Product("Monitor", "27-inch monitor", new BigDecimal("15000.00"), 8);
        product.setId(3L);

        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        Optional<Product> result = productService.getProductById(3L);

        assertTrue(result.isPresent());
        assertEquals("Monitor", result.get().getName());
    }

    @Test
    void getProductById_shouldReturnEmpty_whenProductDoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProductById(999L);

        assertFalse(result.isPresent());
    }
}