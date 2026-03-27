package email.api.email.service.consumer;

import email.api.email.service.model.OrderEvent;
import email.api.email.service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumer {
    private final EmailService service;

    @KafkaListener(
            topics = "orders",
            groupId = "email-service-group",
            concurrency = "3"

    )
    public void consume(OrderEvent orderEvent){
        log.info("Email service received order [{}] from client {}",
                orderEvent.getOrderId(),
                orderEvent.getCustomerEmail());
        sendOrderConfirmationEmail(orderEvent);

    }

    private void sendOrderConfirmationEmail(OrderEvent orderEvent){
        log.info("Sending order confirmation email to {}",orderEvent.getCustomerEmail());
        log.info("Order summary: Total: {}, Items: {}, ID: {}",
                orderEvent.getTotalAmount(),
                orderEvent.getItems(),
                orderEvent.getOrderId());
        service.sendOrderConfirmation(orderEvent);


    }
}
