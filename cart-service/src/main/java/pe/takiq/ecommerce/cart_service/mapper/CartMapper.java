package pe.takiq.ecommerce.cart_service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.cart_service.dto.response.CartItemResponseDTO;
import pe.takiq.ecommerce.cart_service.dto.response.CartResponseDTO;
import pe.takiq.ecommerce.cart_service.model.Cart;

@Component
public class CartMapper {

    public CartResponseDTO toResponse(Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());

        List<CartItemResponseDTO> items = cart.getItems().stream().map(item -> {
            CartItemResponseDTO i = new CartItemResponseDTO();
            i.setProductId(item.getProductId());
            i.setProductName(item.getProductName());
            i.setPrice(item.getPrice());
            i.setQuantity(item.getQuantity());
            i.setSubtotal(item.getPrice() * item.getQuantity());
            return i;
        }).toList();

        dto.setItems(items);
        dto.setTotal(
                items.stream().mapToDouble(CartItemResponseDTO::getSubtotal).sum()
        );

        return dto;
    }
}
