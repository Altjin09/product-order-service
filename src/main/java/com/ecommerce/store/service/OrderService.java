package com.ecommerce.store.service;

import com.ecommerce.store.dto.OrderRequest;
import com.ecommerce.store.model.*;
import com.ecommerce.store.repository.OrderRepository;
import com.ecommerce.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order placeOrder(OrderRequest req, String customerId) {
        List<OrderItem> items = req.getItems().stream().map(itemReq -> {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product not found: " + itemReq.getProductId()));

            if (product.getStock() < itemReq.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for: " + product.getName());
            }

            // Deduct stock
            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            return OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build();
        }).toList();

        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customerId(customerId)
                .items(items)
                .totalAmount(total)
                .shippingAddress(req.getShippingAddress())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order placed: {} by customer {}, total: {}", saved.getId(), customerId, total);
        return saved;
    }

    public List<Order> getByCustomer(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Optional<Order> getById(String id, String userId, String role) {
        return orderRepository.findById(id).filter(order ->
                "ADMIN".equals(role) || "SELLER".equals(role)
                        || order.getCustomerId().equals(userId)
        );
    }

    public Optional<Order> updateStatus(String id, String status, String role) {
        if (!"ADMIN".equals(role) && !"SELLER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN or SELLER can update order status");
        }
        return orderRepository.findById(id).map(order -> {
            order.setStatus(Order.Status.valueOf(status.toUpperCase()));
            return orderRepository.save(order);
        });
    }
}
