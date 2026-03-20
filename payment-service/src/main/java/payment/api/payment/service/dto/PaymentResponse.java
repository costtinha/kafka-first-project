package payment.api.payment.service.dto;

import lombok.Builder;
import payment.api.payment.service.model.Payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentResponse(
        String paymentId,
        String orderId,
        String customerEmail,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime processedAt
) {
}
