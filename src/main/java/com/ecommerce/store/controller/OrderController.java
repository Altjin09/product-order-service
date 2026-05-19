package com.ecommerce.store.controller;

import com.ecommerce.store.dto.OrderRequest;
import com.ecommerce.store.model.Order;
import com.ecommerce.store.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @Valid @RequestBody OrderRequest req,
            HttpServletRequest httpReq) {
        String customerId = httpReq.getHeader("X-User-Id");
        Order order = orderService.placeOrder(req, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(HttpServletRequest httpReq) {
        String customerId = httpReq.getHeader("X-User-Id");
        return ResponseEntity.ok(orderService.getByCustomer(customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(
            @PathVariable String id,
            HttpServletRequest httpReq) {
        String userId = httpReq.getHeader("X-User-Id");
        String role = httpReq.getHeader("X-User-Role");
        return orderService.getById(id, userId, role)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable String id,
            @RequestParam String status,
            HttpServletRequest httpReq) {
        String role = httpReq.getHeader("X-User-Role");
        return orderService.updateStatus(id, status, role)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
