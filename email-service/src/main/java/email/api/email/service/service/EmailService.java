package email.api.email.service.service;

import email.api.email.service.model.OrderEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendOrderConfirmation(OrderEvent orderEvent){
        try {


            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(orderEvent.getCustomerEmail());
            helper.setSubject("Order confirmation - #" + orderEvent.getOrderId());
            helper.setText(buildEmailBody(orderEvent),true);
            mailSender.send(message);

            log.info("Confirmation Email send to {} for order [{}]",
                    orderEvent.getCustomerEmail(),
                    orderEvent.getOrderId());
        } catch (MessagingException e) {
            log.error("Failed to send confirmation email for order [{}] : {}",orderEvent.getOrderId(),
                    e.getMessage());
            throw new RuntimeException("Email sending failed for order: "+ orderEvent.getOrderId());
        }
    }




    private String buildEmailBody(OrderEvent orderEvent){
        StringBuilder items = new StringBuilder();
        orderEvent.getItems().forEach(item ->
                items.append(String.format("""
                        <tr>
                          <td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td>
                          <td style="padding: 8px; border-bottom: 1px solid #eee; text-align: center;">%d</td>
                          <td style="padding: 8px; border-bottom: 1px solid #eee; text-align: right;">$%.2f</td>
                         </tr>
                        """,
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
        );
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #4CAF50; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Order Confirmed!</h1>
                    </div>
                    
                    <div style="padding: 20px;">
                        <p>Hi there,</p>
                        <p>Thank you for your order. Here's your summary:</p>
                        
                        <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px;">
                            <strong>Order ID:</strong> %s<br/>
                            <strong>Date:</strong> %s
                        </div>
                        
                        <h3>Items Ordered</h3>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <thead>
                                <tr style="background-color: #f2f2f2;">
                                    <th style="padding: 8px; text-align: left;">Product</th>
                                    <th style="padding: 8px; text-align: center;">Qty</th>
                                    <th style="padding: 8px; text-align: right;">Price</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                        
                        <div style="text-align: right; margin-top: 15px; font-size: 18px;">
                            <strong>Total: $%.2f</strong>
                        </div>
                        
                        <p style="color: #888; font-size: 12px; margin-top: 30px;">
                            This is an automated message. Please do not reply to this email.
                        </p>
                    </div>
                </body>
                </html>
                """,
                orderEvent.getOrderId(),
                orderEvent.getCreatedAt(),
                items.toString(),
                orderEvent.getTotalAmount()
        );
    }
}
