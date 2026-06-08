package pe.takiq.ecommerce.user_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.user_service.event.UserRegisteredEvent;
import pe.takiq.ecommerce.user_service.service.UserService;

/**
 * Escucha el evento user.registered publicado por auth-service.
 * Crea automáticamente el perfil del usuario en user-service.
 * Es idempotente: si el perfil ya existe lo ignora.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final UserService userService;

    @RabbitListener(queues = "${rabbitmq.queue.user-registered}")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Evento user.registered recibido: userId={}, email={}",
            event.getUserId(), event.getEmail());
        try {
            userService.createUserFromEvent(
                event.getUserId(),
                event.getEmail(),
                event.getFirstName(),
                event.getLastName(),
                event.getPhone()
            );
        } catch (Exception e) {
            log.error("Error procesando user.registered para userId={}: {}",
                event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
}
