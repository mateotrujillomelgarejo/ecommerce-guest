package pe.takiq.ecommerce.inventory_service.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.inventory_service.dto.ReserveStockRequest;
import pe.takiq.ecommerce.inventory_service.dto.RestockRequest;
import pe.takiq.ecommerce.inventory_service.event.OrderPaidEvent;
import pe.takiq.ecommerce.inventory_service.event.StockRestockedEvent;
import pe.takiq.ecommerce.inventory_service.exception.InsufficientStockException;
import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.repository.InventoryRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class InventoryService {

    private final InventoryRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<String> reserveStockScript;
    private final DefaultRedisScript<String> releaseStockScript;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.stock-restocked}")
    private String stockRestockedRoutingKey;

    private static final String STOCK_PREFIX = "stock:";

    public InventoryService(
            InventoryRepository repository,
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("reserveStockScript") DefaultRedisScript<String> reserveStockScript,
            @Qualifier("releaseStockScript") DefaultRedisScript<String> releaseStockScript,
            RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.reserveStockScript = reserveStockScript;
        this.releaseStockScript = releaseStockScript;
        this.rabbitTemplate = rabbitTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFICACIÓN (lectura pura)
    // ─────────────────────────────────────────────────────────────────────────

    public boolean checkAvailability(String productId, int quantity) {
        ensureStockInRedis(productId);
        Object current = redisTemplate.opsForValue().get(STOCK_PREFIX + productId);
        return current != null && Integer.parseInt(current.toString()) >= quantity;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESERVA ATÓMICA
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void reserveStock(ReserveStockRequest request) {
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();

        for (ReserveStockRequest.Item item : request.getItems()) {
            ensureStockInRedis(item.getProductId());
            keys.add(STOCK_PREFIX + item.getProductId());
            args.add(item.getQuantity().toString());
        }
        args.add(request.getOrderId());

        String result = redisTemplate.execute(reserveStockScript, keys, args.toArray());

        if (result != null && result.startsWith("INSUFFICIENT_STOCK")) {
            throw new InsufficientStockException(
                    "Stock insuficiente para uno de los productos de la orden "
                    + request.getOrderId());
        }

        // ✅ Actualizar reservedQuantity en PostgreSQL para trazabilidad
        for (ReserveStockRequest.Item item : request.getItems()) {
            repository.findByProductId(item.getProductId()).ifPresent(inv -> {
                inv.setReservedQuantity(inv.getReservedQuantity() + item.getQuantity());
                repository.save(inv);
            });
        }

        log.info("Stock reservado temporalmente en Redis para orderId={}", request.getOrderId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRMACIÓN DE STOCK (al recibir order.paid)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deductStock(OrderPaidEvent event) {
        for (OrderPaidEvent.OrderItemEvent item : event.getItems()) {
            int updated = repository.deductStock(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                log.error("Discrepancia DB-Redis para producto {}", item.getProductId());
                throw new InsufficientStockException(
                        "No se pudo confirmar el stock físico para " + item.getProductId());
            }
            // ✅ Reducir reservedQuantity — ya no está reservado, está descontado
            repository.findByProductId(item.getProductId()).ifPresent(inv -> {
                int newReserved = Math.max(0, inv.getReservedQuantity() - item.getQuantity());
                inv.setReservedQuantity(newReserved);
                repository.save(inv);
            });
        }
        // Borra la reserva temporal de Redis — stock ya descontado en DB
        redisTemplate.delete("reserve:" + event.getOrderId());
        log.info("Stock confirmado permanentemente en DB para orderId={}", event.getOrderId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIBERACIÓN DE RESERVA (al recibir order.cancelled)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void releaseReservation(String orderId) {
        String result = redisTemplate.execute(
                releaseStockScript, Collections.emptyList(), orderId);

        if ("OK".equals(result)) {
            log.info("Stock temporal liberado en Redis para orderId={}", orderId);
        } else {
            log.warn("No se encontró reserva temporal para liberar en Redis para orderId={} " +
                     "(puede haber expirado por TTL)", orderId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESTOCK ADMIN — ✅ nuevo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * El administrador recarga stock de un producto.
     * Actualiza PostgreSQL y Redis en la misma operación.
     * Publica StockRestockedEvent para que Search Service marque el producto
     * como disponible si antes estaba sin stock.
     */
    @Transactional
    public Inventory restock(RestockRequest request) {
        Inventory inv = repository.findByProductId(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + request.getProductId()));

        int previousQty = inv.getAvailableQuantity();
        int newQty = previousQty + request.getQuantity();

        inv.setAvailableQuantity(newQty);
        inv = repository.save(inv);

        // ✅ Actualizar Redis atomicamente
        redisTemplate.opsForValue().set(
                STOCK_PREFIX + request.getProductId(),
                String.valueOf(newQty));

        // ✅ Publicar evento para Search Service
        StockRestockedEvent event = StockRestockedEvent.builder()
                .productId(request.getProductId())
                .quantityAdded(request.getQuantity())
                .newAvailableQuantity(newQty)
                .reason(request.getReason())
                .restockedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, stockRestockedRoutingKey, event);

        log.info("Restock exitoso: productId={}, anterior={}, nuevo={}, agregado={}",
                request.getProductId(), previousQty, newQty, request.getQuantity());

        return inv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN DIRECTA (mantener por compatibilidad interna)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void updateStock(String productId, int quantity) {
        Inventory inv = repository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + productId));
        int safeQty = Math.max(0, quantity);
        inv.setAvailableQuantity(safeQty);
        repository.save(inv);
        // ✅ Sincronizar Redis en la misma operación
        redisTemplate.opsForValue().set(STOCK_PREFIX + productId, String.valueOf(safeQty));
        log.info("Stock actualizado: productId={}, cantidad={}", productId, safeQty);
    }

    public List<Inventory> getAllInventory() {
        return repository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private void ensureStockInRedis(String productId) {
        String key = STOCK_PREFIX + productId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            Inventory inv = repository.findByProductId(productId)
                    .filter(Inventory::isActive)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no existe o está inactivo: " + productId));
            redisTemplate.opsForValue().set(key, inv.getAvailableQuantity().toString());
        }
    }
}