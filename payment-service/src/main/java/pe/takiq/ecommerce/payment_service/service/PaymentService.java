package pe.takiq.ecommerce.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.payment_service.dto.PaymentRequest;
import pe.takiq.ecommerce.payment_service.dto.PaymentResponse;
import pe.takiq.ecommerce.payment_service.events.PaymentSucceededEvent;
import pe.takiq.ecommerce.payment_service.model.PaymentOutboxEvent;
import pe.takiq.ecommerce.payment_service.model.PaymentTransaction;
import pe.takiq.ecommerce.payment_service.repository.PaymentOutboxRepository;
import pe.takiq.ecommerce.payment_service.repository.PaymentTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("orderId es obligatorio");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount debe ser > 0");
        }

        // Simulación: Aquí harías la llamada a Stripe (Stripe.createSession)
        String paymentId = UUID.randomUUID().toString(); 

        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentId(paymentId);
        tx.setOrderId(request.getOrderId());
        tx.setAmount(request.getAmount());
        tx.setStatus("PENDING");
        tx.setGateway("SIMULATED");
        if (request.getGuestEmail() != null && !request.getGuestEmail().isBlank()) {
            tx.setGuestEmail(request.getGuestEmail());
        }

        transactionRepository.save(tx);
        log.info("Pago iniciado → paymentId={}, orderId={}", paymentId, request.getOrderId());

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(paymentId);
        response.setStatus("PENDING");
        response.setRedirectUrl(request.getReturnUrl() != null 
            ? request.getReturnUrl() + "?paymentId=" + paymentId + "&status=pending" : null);
        response.setMessage("Pago iniciado. Completa el proceso.");
        
        return response;
    }

    @Transactional
    public void confirmPayment(String paymentId) {
        PaymentTransaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + paymentId));

        if ("SUCCESS".equals(tx.getStatus())) return;
        if (!"PENDING".equals(tx.getStatus())) throw new IllegalStateException("Estado inválido: " + tx.getStatus());

        tx.setStatus("SUCCESS");
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        PaymentSucceededEvent event = PaymentSucceededEvent.builder()
                .orderId(tx.getOrderId())
                .paymentId(tx.getPaymentId())
                .amount(tx.getAmount().doubleValue())
                .gateway(tx.getGateway())
                .confirmedAt(LocalDateTime.now())
                .guestEmail(tx.getGuestEmail())
                .build();

        try {
            PaymentOutboxEvent outbox = new PaymentOutboxEvent();
            outbox.setEventType("payment.succeeded");
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outboxRepository.save(outbox);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando evento de pago", e);
        }

        log.info("Pago confirmado y guardado en Outbox → paymentId={}", paymentId);
    }

    @Transactional
    public void failPayment(String paymentId) {
        PaymentTransaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));

        if ("SUCCESS".equals(tx.getStatus())) {
            throw new IllegalStateException("No se puede fallar un pago ya exitoso");
        }

        tx.setStatus("FAILED");
        transactionRepository.save(tx);
        log.warn("Pago fallido manualmente → paymentId={}, orderId={}", paymentId, tx.getOrderId());
    }

    @Transactional
    public void processRefundForFailedOrder(String orderId) {
        transactionRepository.findByOrderIdAndStatus(orderId, "SUCCESS")
                .ifPresent(tx -> {
                    log.info("Conectando con pasarela para reembolsar orden: {}", orderId);
                    
                    // TODO: Llamada HTTP real a la pasarela (ej. stripeClient.refund(tx.getPaymentId()))
                    // Si esta llamada HTTP falla, el método lanzará excepción, deshaciendo la transacción 
                    // y permitiendo que RabbitMQ reintente la compensación de la Saga más tarde.
                    
                    tx.setStatus("REFUNDED");
                    tx.setUpdatedAt(LocalDateTime.now());
                    transactionRepository.save(tx);
                    log.warn("SAGA REVERSAL: Reembolso procesado exitosamente para la orden: {}", tx.getOrderId());
                });
    }
}