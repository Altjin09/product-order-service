package com.ecommerce.store.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    @NotEmpty
    private List<OrderItemRequest> items;

    @NotBlank
    private String shippingAddress;

    @Data
    public static class OrderItemRequest {
        @NotBlank
        private String productId;

        @Min(1)
        private Integer quantity;
    }
}
