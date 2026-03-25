package payment.api.payment.service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import payment.api.payment.service.dto.PaymentResponse;
import payment.api.payment.service.exception.PaymentNotFoundException;
import payment.api.payment.service.model.OrderEvent;
import payment.api.payment.service.model.Payment;
import payment.api.payment.service.persistance.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private PaymentRepository repository;
    @Mock
    private PaymentMapper mapper;

    @InjectMocks
    private PaymentService service;

    private OrderEvent orderEvent;
    private Payment payment;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp(){
        orderEvent = OrderEvent.builder()
                .orderId("order-123")
                .customerEmail("user@example.com")
                .totalAmount(new BigDecimal("99.90"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();
        payment = Payment.builder()
                .id("payment-456")
                .orderId("order-123")
                .customerEmail("user@example.com")
                .amount(new BigDecimal("99.90"))
                .status(Payment.PaymentStatus.SUCCESS)
                .processedAt(LocalDateTime.now())
                .build();

        paymentResponse = PaymentResponse.builder()
                .paymentId("payment-456")
                .orderId("order-123")
                .customerEmail("user@example.com")
                .amount(new BigDecimal("99.90"))
                .status(Payment.PaymentStatus.SUCCESS)
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment{
        @Test
        @DisplayName("Should save payment when order has not been processed before")
        void shouldSavePayment_WhenOrderIsNew(){
            when(repository.findByOrderId("order-123")).thenReturn(Optional.empty());

            service.processPayment(orderEvent);

            verify(repository,times(1)).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should skip payment when order already exists")
        void shoudSkipPayment_WhenOrderAlreadyExists(){
            when(repository.findByOrderId("order-123")).thenReturn(Optional.of(payment));

            service.processPayment(orderEvent);

            verify(repository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should throw an illegal argument when email contais fail")
        void shouldThrowException_WhenEmailContainsFail(){
            orderEvent = OrderEvent.builder()
                    .orderId("order-123")
                    .customerEmail("fail@example.com")
                    .totalAmount(new BigDecimal("99.90"))
                    .createdAt(LocalDateTime.now())
                    .items(List.of())
                    .build();

            when(repository.findByOrderId("order-123")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processPayment(orderEvent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Simulated payment failure");

            verify(repository,never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class getPaymentByOrderId {
        @Test
        @DisplayName("Should return payment response when order exists")
        void shouldReturnPayment_WhenOrderExists(){
            when(repository.findByOrderId("order-123"))
                    .thenReturn(Optional.of(payment));

            when(mapper.paymentToResponse(payment)).thenReturn(paymentResponse);

            PaymentResponse result = service.getPaymentByOrderId("order-123");

            assertThat(result).isNotNull();
            assertThat(result.orderId()).isEqualTo("order-123");
            assertThat(result.status()).isEqualTo(Payment.PaymentStatus.SUCCESS);
            assertThat(result.amount()).isEqualByComparingTo("99.90");
        }

        @Test
        @DisplayName("Should throw PaymentNotFound exception when order does not exist")
        void shouldThrownException_WhenOrderDontExist(){
            when(repository.findByOrderId("null")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPaymentByOrderId("null"))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("Payment no found for order: ");
        }
    }

    @Nested
    @DisplayName("getAllPayments")
    class getAllPayments{
        @Test
        @DisplayName("Should display all payments")
        void shouldDisplayAllPayments(){
            when(repository.findAll()).thenReturn(List.of(payment));
            when(mapper.paymentToResponse(payment)).thenReturn(paymentResponse);

            List<PaymentResponse> result = service.getAllPayments();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().orderId()).isEqualTo("order-123");
        }
        @Test
        @DisplayName("should return empty list when no payments exist")
        void shouldReturnEmptyList_whenNoPayments() {
            when(repository.findAll()).thenReturn(List.of());

            List<PaymentResponse> results = service.getAllPayments();

            assertThat(results).isEmpty();
        }
    }


}
