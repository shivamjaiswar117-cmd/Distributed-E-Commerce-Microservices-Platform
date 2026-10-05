package com.distributed.ecommerce.product.service;

import com.distributed.ecommerce.product.entity.Product;
import com.distributed.ecommerce.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        System.out.println("Fetching all products from DATABASE (not cache)");
        return productRepository.findAll();
    }

    @Cacheable(value = "product", key = "#id")
    public Optional<Product> getProductById(Long id) {
        System.out.println("Fetching product id=" + id + " from DATABASE (not cache)");
        return productRepository.findById(id);
    }
}