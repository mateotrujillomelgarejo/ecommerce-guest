// ────────────────────────────────────────────────
// pe.takiq.ecommerce.payment_service.service.PaymentService.java
// ────────────────────────────────────────────────
package pe.takiq.ecommerce.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.takiq.ecommerce.payment_service.dto.DirectPaymentRequest;
import pe.takiq.ecommerce.payment_service.dto.PaymentRequest;
import pe.takiq.ecommerce.payment_service.dto.PaymentResponse;
import pe.takiq.ecommerce.payment_service.events.PaymentSucceededEvent;
import pe.takiq.ecommerce.payment_service.model.PaymentTransaction;
import pe.takiq.ecommerce.payment_service.repository.PaymentTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String ORDER_EVENTS_EXCHANGE = "ecommerce.events";
    private static final String ROUTING_KEY_PAYMENT_SUCCEEDED = "payment.succeeded";

@Transactional
    public PaymentResponse simulateDirectSuccess(DirectPaymentRequest request) {
        // Validaciones mínimas
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount es obligatorio y > 0");
        }
        if (request.getGuestId() == null || request.getGuestId().isBlank()) {
            throw new IllegalArgumentException("guestId es obligatorio");
        }

        String paymentId = UUID.randomUUID().toString();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentId(paymentId);
        tx.setAmount(request.getAmount());
        tx.setStatus("SUCCESS");
        tx.setGateway("SIMULATED_DIRECT");
        tx.setGuestEmail(request.getGuestEmail() != null ? request.getGuestEmail() : "unknown");

        transactionRepository.save(tx);

        // Publicar evento inmediatamente
        PaymentSucceededEvent event = PaymentSucceededEvent.builder()
                .orderId(null)
                .paymentId(paymentId)
                .amount(request.getAmount().doubleValue())
                .gateway(tx.getGateway())
                .confirmedAt(LocalDateTime.now())
                .guestEmail(request.getGuestEmail())
                .build();

        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_KEY_PAYMENT_SUCCEEDED, event);

        log.info("Pago simulado directo → paymentId={}, amount={}, guestId={}", 
                 paymentId, request.getAmount(), request.getGuestId());

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(paymentId);
        response.setStatus("SUCCEEDED");
        response.setMessage("Pago simulado exitoso de forma directa");
        response.setRedirectUrl(null);

        return response;
    }

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        // Validación básica
        if (request.getOrderId() == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("orderId y amount son obligatorios y amount > 0");
        }

        String paymentId = UUID.randomUUID().toString();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentId(paymentId);
        tx.setOrderId(request.getOrderId());
        tx.setAmount(request.getAmount());
        tx.setStatus("PENDING");
        tx.setGateway("SIMULATED"); // → cambiar a "STRIPE", etc. cuando integres real

        transactionRepository.save(tx);

        log.info("Pago iniciado → paymentId={}, orderId={}, amount={}", 
                 paymentId, request.getOrderId(), request.getAmount());

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(paymentId);
        response.setStatus("PENDING");
        response.setRedirectUrl(request.getReturnUrl() + "?paymentId=" + paymentId + "&status=pending");
        response.setMessage("Pago iniciado. Completa el proceso en la pasarela.");

        return response;
    }

    @Transactional
    public void confirmPayment(String paymentId) {
        PaymentTransaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + paymentId));

        // Idempotencia fuerte
        if ("SUCCESS".equals(tx.getStatus())) {
            log.info("Pago {} ya confirmado previamente (idempotencia)", paymentId);
            return;
        }

        if (!"PENDING".equals(tx.getStatus())) {
            throw new IllegalStateException("No se puede confirmar transacción en estado: " + tx.getStatus());
        }

        // Simulación de aprobación (en real: callback del gateway)
        tx.setStatus("SUCCESS");
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // Publicar evento → Order Service lo escuchará y pasará a PAID + publicará order.created
        PaymentSucceededEvent event = PaymentSucceededEvent.builder()
                .orderId(tx.getOrderId())
                .paymentId(tx.getPaymentId())
                .amount(tx.getAmount().doubleValue())
                .gateway(tx.getGateway())
                .confirmedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_KEY_PAYMENT_SUCCEEDED, event);

        log.info("Pago confirmado → evento payment.succeeded publicado para orderId={}, paymentId={}", 
                 tx.getOrderId(), paymentId);
    }

    // Para testing: simular fallo
    public void failPayment(String paymentId) {
        PaymentTransaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));

        if ("SUCCESS".equals(tx.getStatus())) {
            throw new IllegalStateException("No se puede fallar un pago ya exitoso");
        }

        tx.setStatus("FAILED");
        transactionRepository.save(tx);

        log.warn("Pago fallido manualmente → paymentId={}", paymentId);
        // Opcional: publicar payment.failed si lo necesitas en el futuro
    }
}