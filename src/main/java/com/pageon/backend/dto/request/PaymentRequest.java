package com.pageon.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PaymentRequest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ready {
        private Integer amount;
        private Integer point;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Confirm {
        private Integer amount;
        private String paymentKey;
        private String orderId;
    }
}
