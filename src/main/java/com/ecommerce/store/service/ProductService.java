package com.ecommerce.store.service;

import com.ecommerce.store.dto.ProductRequest;
import com.ecommerce.store.model.Product;
import com.ecommerce.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAll(String category, int page, int size) {
        var pageable = PageRequest.of(page, size);
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryAndActiveTrue(category, pageable).getContent();
        }
        return productRepository.findByActiveTrue(pageable).getContent();
    }

    public Optional<Product> getById(String id) {
        return productRepository.findById(id).filter(Product::isActive);
    }

    public Product create(ProductRequest req, String sellerId) {
        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .category(req.getCategory())
                .imageUrl(req.getImageUrl())
                .sellerId(sellerId)
                .build();
        log.info("Product created: {} by seller {}", req.getName(), sellerId);
        return productRepository.save(product);
    }

    public Optional<Product> update(String id, ProductRequest req, String sellerId, String role) {
        return productRepository.findById(id).map(product -> {
            // RBAC: sellers can only update their own products
            if ("SELLER".equals(role) && !product.getSellerId().equals(sellerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another seller's product");
            }
            product.setName(req.getName());
            product.setDescription(req.getDescription());
            product.setPrice(req.getPrice());
            product.setStock(req.getStock());
            product.setCategory(req.getCategory());
            if (req.getImageUrl() != null) product.setImageUrl(req.getImageUrl());
            return productRepository.save(product);
        });
    }

    public void delete(String id, String sellerId, String role) {
        productRepository.findById(id).ifPresent(product -> {
            if ("SELLER".equals(role) && !product.getSellerId().equals(sellerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another seller's product");
            }
            product.setActive(false); // soft delete
            productRepository.save(product);
        });
    }
}
