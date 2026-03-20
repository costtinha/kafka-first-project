package payment.api.payment.service.service;

import org.springframework.stereotype.Service;
import payment.api.payment.service.dto.PaymentResponse;
import payment.api.payment.service.model.Payment;

@Service
public class PaymentMapper {
    public PaymentResponse paymentToResponse(Payment payment){
        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .processedAt(payment.getProcessedAt())
                .customerEmail(payment.getCustomerEmail())
                .build();
    }
    public Payment responseToPayment(PaymentResponse response){
        return Payment.builder()
                .orderId(response.orderId())
                .customerEmail(response.customerEmail())
                .status(response.status())
                .amount(response.amount())
                .processedAt(response.processedAt())
                .id(response.paymentId())
                .build();
    }
}
