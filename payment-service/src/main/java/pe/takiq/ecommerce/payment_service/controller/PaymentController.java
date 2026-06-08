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