package payment.api.payment.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import payment.api.payment.service.dto.PaymentResponse;
import payment.api.payment.service.service.PaymentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable("orderId") String orderId){
        log.info("Payment status request from order: {}",orderId);
        return ResponseEntity.ok(service.getPaymentByOrderId(orderId));

    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(){
        return ResponseEntity.ok(service.getAllPayments());
    }
}
