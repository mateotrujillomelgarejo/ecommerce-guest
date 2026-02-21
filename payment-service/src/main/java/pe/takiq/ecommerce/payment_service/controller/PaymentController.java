package pe.takiq.ecommerce.payment_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.takiq.ecommerce.payment_service.dto.PaymentRequest;
import pe.takiq.ecommerce.payment_service.dto.PaymentResponse;
import pe.takiq.ecommerce.payment_service.service.PaymentService;

/**
 * Controlador para operaciones de pago.
 * 
 * Flujo recomendado (producción y pruebas reales):
 * 1. Crear orden en order-service (estado PAYMENT_PENDING) → obtener orderId
 * 2. POST /payments/initiate con orderId → obtener paymentId
 * 3. POST /payments/confirm/{paymentId} (simulado o webhook real) → publica evento payment.succeeded
 * 
 * El endpoint POST /payments (simulateDirectSuccess) está DEPRECADO porque no garantiza orderId.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Inicia el proceso de pago asociado a una orden ya creada (PAYMENT_PENDING).
     * 
     * Requerido: orderId válido (debe existir en order-service).
     * Devuelve paymentId para seguimiento o confirmación posterior.
     * 
     * Uso típico:
     * - Frontend crea orden pendiente → obtiene orderId
     * - Llama aquí para iniciar pago
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/confirm/{paymentId}")
    public ResponseEntity<String> confirmPayment(@PathVariable("paymentId") String paymentId) {
        paymentService.confirmPayment(paymentId);
        return ResponseEntity.ok("Pago confirmado exitosamente. Evento payment.succeeded publicado.");
    }


    @PostMapping("/fail/{paymentId}")
    public ResponseEntity<String> failPayment(@PathVariable("paymentId") String paymentId) {
        paymentService.failPayment(paymentId);
        return ResponseEntity.ok("Pago marcado como fallido (sin evento de éxito publicado).");
    }
}