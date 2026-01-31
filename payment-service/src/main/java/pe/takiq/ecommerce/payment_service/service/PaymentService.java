package pe.takiq.ecommerce.payment_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.payment_service.dto.PaymentRequest;
import pe.takiq.ecommerce.payment_service.dto.PaymentResponse;
import pe.takiq.ecommerce.payment_service.model.PaymentTransaction;
import pe.takiq.ecommerce.payment_service.repository.PaymentTransactionRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;

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

        if ("PENDING".equals(tx.getStatus())) {
            tx.setStatus("SUCCESS");
            transactionRepository.save(tx);

            // Publicar evento de confirmación (para que order-service lo escuche)
            // rabbitTemplate.convertAndSend(... OrderPaymentConfirmedEvent ...)
        }
    }
}