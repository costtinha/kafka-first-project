package payment.api.payment.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import payment.api.payment.service.model.OrderEvent;
import payment.api.payment.service.service.PaymentService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConsumer {
    private final PaymentService service;

    @KafkaListener(
            topics = "orders",
            groupId = "payment-service-group",
            concurrency = "3"
    )
    public void processPayment(OrderEvent orderEvent){
        log.info("Payment service received order [{}] for customer: {}",
                orderEvent.getOrderId(),
                orderEvent.getCustomerEmail());
        service.processPayment(orderEvent);
    }
}
