package pe.takiq.ecommerce.inventory_service.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.inventory_service.dto.ReserveStockRequest;
import pe.takiq.ecommerce.inventory_service.event.OrderPaidEvent;
import pe.takiq.ecommerce.inventory_service.exception.InsufficientStockException;
import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class InventoryService {

    private final InventoryRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Qualifier("releaseStockScript")
    private final DefaultRedisScript<String> reserveStockScript;

    @Qualifier("releaseStockScript")
    private final DefaultRedisScript<String> releaseStockScript;

    public InventoryService(
            InventoryRepository repository,
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("reserveStockScript") DefaultRedisScript<String> reserveStockScript,
            @Qualifier("releaseStockScript") DefaultRedisScript<String> releaseStockScript
    ) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.reserveStockScript = reserveStockScript;
        this.releaseStockScript = releaseStockScript;
    }

    private static final String STOCK_PREFIX = "stock:";

    private void ensureStockInRedis(String productId) {
        String key = STOCK_PREFIX + productId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            Inventory inv = repository.findByProductId(productId)
                    .filter(Inventory::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no existe o está inactivo: " + productId));
            redisTemplate.opsForValue().set(key, inv.getAvailableQuantity().toString());
        }
    }

    public boolean checkAvailability(String productId, int quantity) {
        ensureStockInRedis(productId);
        Object current = redisTemplate.opsForValue().get(STOCK_PREFIX + productId);
        return current != null && Integer.parseInt(current.toString()) >= quantity;
    }

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
            throw new InsufficientStockException("Stock insuficiente para uno de los productos de la orden " + request.getOrderId());
        }
        log.info("Stock reservado temporalmente en Redis para orderId={}", request.getOrderId());
    }

    @Transactional
    public void deductStock(OrderPaidEvent event) {
        for (OrderPaidEvent.OrderItemEvent item : event.getItems()) {
            int updated = repository.deductStock(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                log.error("Discrepancia DB-Redis para producto {}", item.getProductId());
                throw new InsufficientStockException("No se pudo confirmar el stock físico para " + item.getProductId());
            }
        }
        redisTemplate.delete("reserve:" + event.getOrderId());
        log.info("Stock confirmado permanentemente en DB para orderId={}", event.getOrderId());
    }

    public void releaseReservation(String orderId) {
        String result = redisTemplate.execute(releaseStockScript, Collections.emptyList(), orderId);
        if ("OK".equals(result)) {
            log.info("Stock temporal liberado/restaurado en Redis para orderId={}", orderId);
        } else {
            log.warn("No se encontró reserva para liberar en Redis para orderId={}", orderId);
        }
    }

    @Transactional
    public void updateStock(String productId, int quantity) {
        Inventory inv = repository.findByProductId(productId).orElseThrow();
        inv.setAvailableQuantity(Math.max(0, quantity));
        repository.save(inv);
        redisTemplate.opsForValue().set(STOCK_PREFIX + productId, String.valueOf(Math.max(0, quantity)));
    }
}