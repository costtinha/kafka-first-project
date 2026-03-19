package order.api.service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order.api.service.config.KafkaTopicConfig;
import order.api.service.model.OrderEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void sendOrder(OrderEvent orderEvent){
        CompletableFuture<SendResult<String,OrderEvent>> future =
                kafkaTemplate.send(KafkaTopicConfig.ORDERS_TOPIC,
                        orderEvent.getOrderId(),
                        orderEvent);

        future.whenComplete((result, exception) -> {
            if(exception != null){
                log.error("Failed to send order [{}]: {} ",orderEvent.getOrderId(),
                        exception.getMessage());
            } else {
                log.info("Order send successfully [{}] -> partition: {}, offset: {}",
                        orderEvent.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
