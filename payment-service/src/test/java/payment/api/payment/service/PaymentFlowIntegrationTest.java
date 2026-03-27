package payment.api.payment.service;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import payment.api.payment.service.model.OrderEvent;
import payment.api.payment.service.model.Payment;
import payment.api.payment.service.persistance.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
public class PaymentFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("paymentsdb")
            .withUsername("admin")
            .withPassword("secret");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private PaymentRepository repository;


    @Test
    @DisplayName("Should persist payment when OrderEvent is published to Kafka")
    public void shouldPersistPayment(){
        String orderId = UUID.randomUUID().toString();
        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(orderId)
                .customerEmail("integration@example.com")
                .totalAmount(new BigDecimal("149.90"))
                .createdAt(LocalDateTime.now())
                .items(List.of(OrderEvent.OrderItem.builder()
                        .productId("p1")
                        .productName("Clean Code")
                        .quantity(1)
                        .unitPrice(new BigDecimal("149.90"))
                        .build()))
                .build();

        KafkaTemplate<String,OrderEvent> kafkaTemplate = buildKafkaTemplate();
        kafkaTemplate.send("orders",orderId,orderEvent);

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500,TimeUnit.MILLISECONDS)
                .untilAsserted(() ->{
                    Optional<Payment> payment = repository.findByOrderId(orderId);
                    assertThat(payment).isPresent();
                    assertThat(payment.get().getCustomerEmail()).isEqualTo("integration@example.com");
                    assertThat(payment.get().getAmount()).isEqualByComparingTo("149.90");
                    assertThat(payment.get().getStatus()).isEqualTo(Payment.PaymentStatus.SUCCESS);
                });


    }

    @Test
    @DisplayName("Should not create duplicate payment when same order is published twice")
    void shouldNotCreateDuplicate_whenSameOrderPublishedTwice() {
        String orderId = UUID.randomUUID().toString();

        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(orderId)
                .customerEmail("idempotency@example.com")
                .totalAmount(new BigDecimal("50.00"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();

        KafkaTemplate<String, OrderEvent> kafkaTemplate = buildKafkaTemplate();
        kafkaTemplate.send("orders", orderId, orderEvent);
        kafkaTemplate.send("orders", orderId, orderEvent);

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Payment> payments = repository.findAll()
                            .stream()
                            .filter(p -> p.getOrderId().equals(orderId))
                            .toList();

                    assertThat(payments).hasSize(1);
                });
    }

    @Test
    @DisplayName("Should not process payment when customer email contains fail")
    void shouldNotProcessPayment_whenEmailContainsFail() {
        String orderId = UUID.randomUUID().toString();

        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(orderId)
                .customerEmail("fail@example.com")
                .totalAmount(new BigDecimal("99.90"))
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();

        KafkaTemplate<String, OrderEvent> kafkaTemplate = buildKafkaTemplate();
        kafkaTemplate.send("orders", orderId, orderEvent);


        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Optional<Payment> payment = repository.findByOrderId(orderId);
                    assertThat(payment).isEmpty();
                });
    }




    private KafkaTemplate<String, OrderEvent> buildKafkaTemplate(){
        Map<String,Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                JsonSerializer.ADD_TYPE_INFO_HEADERS, false
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
