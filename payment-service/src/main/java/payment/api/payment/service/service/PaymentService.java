package payment.api.payment.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import payment.api.payment.service.model.OrderEvent;
import payment.api.payment.service.model.Payment;
import payment.api.payment.service.persistance.PaymentRepository;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;

    public void processPayment(OrderEvent orderEvent){
        if (repository.findByOrderId(orderEvent.getOrderId()).isPresent()){
            log.warn("Payment already exists for order [{}] - Skipping",orderEvent.getOrderId());
            return;
        }
        if(orderEvent.getCustomerEmail().contains("fail")){
            throw new RuntimeException("Simulated payment failure");
        }
        Payment payment = Payment.builder()
                .processedAt(LocalDateTime.now())
                .amount(orderEvent.getTotalAmount())
                .customerEmail(orderEvent.getCustomerEmail())
                .orderId(orderEvent.getOrderId())
                .status(Payment.PaymentStatus.SUCCESS)
                .build();
        repository.save(payment);

        log.info("Payment processed successfully for order [{}], amount: {}, customer: {}",
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCustomerEmail());
    }
}
