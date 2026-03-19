package payment.api.payment.service.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import payment.api.payment.service.model.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,String> {

    Optional<Payment> findByOrderId(String orderId);
}
