package pe.takiq.ecommerce.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Flujo correcto: Inicia un pago asociado a una orden ya creada.
     * Devuelve paymentId para que el frontend haga confirmación o polling.
     */
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        // Validación estricta
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("orderId es obligatorio (la orden debe existir antes de iniciar pago)");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount es obligatorio y debe ser > 0");
        }

        String paymentId = UUID.randomUUID().toString();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentId(paymentId);
        tx.setOrderId(request.getOrderId());
        tx.setAmount(request.getAmount());
        tx.setStatus("PENDING");
        tx.setGateway("SIMULATED"); // Cambiar a "STRIPE", "PAYPAL", etc. en producción
        // Opcional: guarda guestEmail si viene en el request
        if (request.getGuestEmail() != null && !request.getGuestEmail().isBlank()) {
            tx.setGuestEmail(request.getGuestEmail());
        }

        transactionRepository.save(tx);

        log.info("Pago iniciado → paymentId={}, orderId={}, amount={}", 
                 paymentId, request.getOrderId(), request.getAmount());

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(paymentId);
        response.setStatus("PENDING");
        response.setRedirectUrl(request.getReturnUrl() != null 
            ? request.getReturnUrl() + "?paymentId=" + paymentId + "&status=pending"
            : null);
        response.setMessage("Pago iniciado. Completa el proceso en la pasarela.");

        return response;
    }

    /**
     * Confirma el pago (simulado o real callback del gateway).
     * Actualiza estado y publica evento con orderId garantizado.
     */
    @Transactional
    public void confirmPayment(String paymentId) {
        PaymentTransaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + paymentId));

        // Idempotencia
        if ("SUCCESS".equals(tx.getStatus())) {
            log.info("Pago {} ya confirmado previamente (idempotencia)", paymentId);
            return;
        }

        if (!"PENDING".equals(tx.getStatus())) {
            throw new IllegalStateException("No se puede confirmar transacción en estado: " + tx.getStatus());
        }

        // Simulación de aprobación
        tx.setStatus("SUCCESS");
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // Publicar evento con orderId (siempre presente)
        if (tx.getOrderId() == null) {
            log.error("¡Transacción sin orderId! paymentId={}", paymentId);
            throw new IllegalStateException("No se puede publicar evento sin orderId asociado");
        }

        PaymentSucceededEvent event = PaymentSucceededEvent.builder()
                .orderId(tx.getOrderId())                     // ← Clave: siempre desde BD
                .paymentId(tx.getPaymentId())
                .amount(tx.getAmount().doubleValue())
                .gateway(tx.getGateway())
                .confirmedAt(LocalDateTime.now())
                .guestEmail(tx.getGuestEmail())               // Opcional: si lo guardaste
                .build();

        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_KEY_PAYMENT_SUCCEEDED, event);

        log.info("Pago confirmado → evento publicado → orderId={}, paymentId={}, amount={}", 
                 tx.getOrderId(), paymentId, tx.getAmount());
    }

    /**
     * Método para testing: simular fallo manual
     */
    public void failPayment(String paymentId) {
        PaymentTransaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));

        if ("SUCCESS".equals(tx.getStatus())) {
            throw new IllegalStateException("No se puede fallar un pago ya exitoso");
        }

        tx.setStatus("FAILED");
        transactionRepository.save(tx);

        log.warn("Pago fallido manualmente → paymentId={}, orderId={}", 
                 paymentId, tx.getOrderId());
    }

    /**
     * Procesa un reembolso automáticamente cuando otro microservicio (ej. Inventario)
     * falla y no puede cumplir con la orden que ya fue pagada.
     */
    @Transactional
    public void processRefundForFailedOrder(String orderId) {
        transactionRepository.findByOrderIdAndStatus(orderId, "SUCCESS")
                .ifPresent(tx -> {
                    tx.setStatus("REFUNDED");
                    tx.setUpdatedAt(LocalDateTime.now());
                    transactionRepository.save(tx);
                    
                    log.warn("Reembolso procesado exitosamente para la orden: {} por falta de inventario", orderId);
                    // NOTA: Aquí en producción llamarías a la API real de Stripe/Niubiz/MercadoPago
                    // para ejecutar el "Refund" en la tarjeta del cliente.
                });
    }
}