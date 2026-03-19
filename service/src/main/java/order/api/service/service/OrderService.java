package order.api.service.service;

import lombok.RequiredArgsConstructor;
import order.api.service.dtos.OrderRecord;
import order.api.service.model.OrderEvent;
import order.api.service.producer.OrderProducer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderProducer orderProducer;
    public String createOrder(OrderRecord record) {
        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(UUID.randomUUID().toString())
                .customerEmail(record.customerEmail())
                .items(record.items())
                .totalAmount(record.totalAmount())
                .createdAt(LocalDateTime.now())
                .build();
        orderProducer.sendOrder(orderEvent);

        return "Order received: " + orderEvent.getOrderId();

    }
}
