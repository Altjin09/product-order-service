package com.ecommerce.store.controller;

import com.ecommerce.store.dto.ProductRequest;
import com.ecommerce.store.model.Product;
import com.ecommerce.store.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lab06: REST API for Product management
 * Auth is handled by AuthMiddleware -> SOAP ValidateToken
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Public: anyone can browse products
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getAll(category, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Protected: SELLER or ADMIN only (enforced by middleware)
    @PostMapping
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody ProductRequest req,
            HttpServletRequest httpReq) {
        String sellerId = httpReq.getHeader("X-User-Id");
        Product created = productService.create(req, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest req,
            HttpServletRequest httpReq) {
        String sellerId = httpReq.getHeader("X-User-Id");
        String role = httpReq.getHeader("X-User-Role");
        return productService.update(id, req, sellerId, role)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id,
            HttpServletRequest httpReq) {
        String sellerId = httpReq.getHeader("X-User-Id");
        String role = httpReq.getHeader("X-User-Role");
        productService.delete(id, sellerId, role);
        return ResponseEntity.noContent().build();
    }
}
