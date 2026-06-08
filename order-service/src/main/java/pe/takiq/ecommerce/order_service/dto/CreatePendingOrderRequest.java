package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePendingOrderRequest {
    private String guestId;
    private String sessionId;

    // ── Desglose financiero (viene de Cart Service via Pricing Service) ──────
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal total;
    // ────────────────────────────────────────────────────────────────────────

    private List<CartItemDTO> items;
}