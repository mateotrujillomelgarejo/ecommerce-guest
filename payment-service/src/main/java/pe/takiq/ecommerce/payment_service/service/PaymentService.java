package pe.takiq.ecommerce.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.payment_service.dto.PaymentRequest;
import pe.takiq.ecommerce.payment_service.dto.PaymentResponse;
import pe.takiq.ecommerce.payment_service.model.PaymentTransaction;
import pe.takiq.ecommerce.payment_service.repository.PaymentTransactionRepository;
import pe.takiq.ecommerce.events.PaymentSucceededEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;

    // Exchange / routing key (usar mismo exchange que order-service)
    private static final String ORDER_EVENTS_EXCHANGE = "order.events.exchange";
    private static final String ROUTING_KEY_PAYMENT_SUCCEEDED = "payment.succeeded";

    public PaymentResponse initiatePayment(PaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentId(paymentId);
        tx.setOrderId(request.getOrderId());
        tx.setAmount(request.getAmount());
        tx.setStatus("PENDING");
        tx.setGateway("SIMULATED");

        transactionRepository.save(tx);

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(paymentId);
        response.setStatus("PENDING");
        response.setRedirectUrl("http://localhost:8080/checkout/success?paymentId=" + paymentId); // Simulado
        response.setMessage("Pago iniciado - simulado");

        return response;
    }

    // Método para simular confirmación (llamado manual o por webhook simulado)
    public void confirmPayment(String paymentId) {
        PaymentTransaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));

        // Idempotencia: si ya SUCCESS, devolver sin re-publicar
        if ("SUCCESS".equals(tx.getStatus())) {
            log.info("Pago {} ya estaba confirmado (idempotencia)", paymentId);
            return;
        }

        if ("PENDING".equals(tx.getStatus())) {
            tx.setStatus("SUCCESS");
            transactionRepository.save(tx);

            // Publicar evento de confirmación para que order-service lo escuche y confirme la orden (desacoplado)
            PaymentSucceededEvent event = PaymentSucceededEvent.builder()
                    .paymentId(tx.getPaymentId())
                    .orderId(tx.getOrderId())
                    .amount(tx.getAmount().doubleValue())
                    .gateway(tx.getGateway())
                    .confirmedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_KEY_PAYMENT_SUCCEEDED, event);
            log.info("PaymentSucceededEvent publicado para paymentId={}, orderId={}", tx.getPaymentId(), tx.getOrderId());
        } else {
            throw new IllegalStateException("Transacción en estado inválido para confirmación: " + tx.getStatus());
        }
    }
}