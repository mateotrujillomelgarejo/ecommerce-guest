// ────────────────────────────────────────────────
// pe.takiq.ecommerce.payment_service.controller.PaymentController.java
// ────────────────────────────────────────────────
package pe.takiq.ecommerce.payment_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.payment_service.dto.PaymentRequest;
import pe.takiq.ecommerce.payment_service.dto.PaymentResponse;
import pe.takiq.ecommerce.payment_service.service.PaymentService;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Inicia el proceso de pago (llamado por frontend después de crear orden pendiente)
     * Devuelve paymentId y redirectUrl (o instrucción para 3DS/simulado)
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para simular confirmación del pago (en producción sería webhook del gateway)
     * Aquí se publica el evento payment.succeeded
     */
    @PostMapping("/confirm/{paymentId}")
    public ResponseEntity<String> confirmPayment(@PathVariable String paymentId) {
        paymentService.confirmPayment(paymentId);
        return ResponseEntity.ok("Pago confirmado y evento publicado");
    }

    /**
     * Opcional: endpoint para simular fallo (testing)
     */
    @PostMapping("/fail/{paymentId}")
    public ResponseEntity<String> failPayment(@PathVariable String paymentId) {
        paymentService.failPayment(paymentId);
        return ResponseEntity.ok("Pago fallido (evento no publicado)");
    }
}