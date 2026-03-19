package order.api.service.dtos;

import order.api.service.model.OrderEvent;

import java.math.BigDecimal;
import java.util.List;

public record OrderRecord(String customerEmail,
                          List<OrderEvent.OrderItem> items,
                          BigDecimal totalAmount) {
}
