package payment.api.payment.service.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import payment.api.payment.service.dto.PaymentResponse;
import payment.api.payment.service.exception.GlobalExceptionHandler;
import payment.api.payment.service.exception.PaymentNotFoundException;
import payment.api.payment.service.model.Payment;
import payment.api.payment.service.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
@ImportAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    private PaymentResponse createPaymentResponse() {
        return PaymentResponse.builder()
                .paymentId("payment-456")
                .orderId("order-123")
                .customerEmail("user@example.com")
                .amount(new BigDecimal("99.90"))
                .status(Payment.PaymentStatus.SUCCESS)
                .processedAt(LocalDateTime.now())
                .build();
    }


    @Test
    @DisplayName("GET payments/{orderId} return 200 with payment when found")
    public void getPaymentByOrderId_return200() throws Exception{
        when(paymentService.getPaymentByOrderId("order-123")).thenReturn(createPaymentResponse());

        mockMvc.perform(get("/payments/order-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(99.90));
    }

    @Test
    @DisplayName("GET /payments/{orderId} returns 404 ProblemDetail when not found")
    public void getPaymentById_returns404() throws Exception{
        when(paymentService.getPaymentByOrderId("invalid")).thenThrow(new PaymentNotFoundException("invalid"));

        mockMvc.perform(get("/payments/invalid").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.instance").value("/payments/invalid"));
    }

    @Test
    @DisplayName("GET /payments returns 200 with all payments")
    public void getAllPayments_returns200() throws Exception{
        when(paymentService.getAllPayments()).thenReturn(List.of(createPaymentResponse()));

        mockMvc.perform(get("/payments").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("order-123"));
    }

    @Test
    @DisplayName("GET /payments returns 200 with empty list when no payments")
    public void getAllPayments_returns200_withEmptyList() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of());
        mockMvc.perform(get("/payments").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
