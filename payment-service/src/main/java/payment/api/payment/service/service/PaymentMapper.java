package payment.api.payment.service.service;

import payment.api.payment.service.dto.PaymentResponse;
import payment.api.payment.service.model.Payment;

public interface PaymentMapper {
    PaymentResponse paymentToResponse(Payment payment);
    Payment responseToPayment(PaymentResponse paymentResponse);
}
