package payment.api.payment.service.exception;

public class PaymentNotFoundException extends RuntimeException{
    public PaymentNotFoundException(String orderId){
        super("Payment no found for order: "+ orderId);
    }
}
