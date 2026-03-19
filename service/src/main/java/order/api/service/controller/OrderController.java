package order.api.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import order.api.service.dtos.OrderRecord;
import order.api.service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService service;

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody OrderRecord record){
        log.info("Received order request from customer: {}",record.customerEmail());
        String returnedMessage = service.createOrder(record);
        return ResponseEntity.accepted().body(returnedMessage);
    }
}
