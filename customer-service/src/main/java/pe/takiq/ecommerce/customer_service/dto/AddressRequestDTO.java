package pe.takiq.ecommerce.customer_service.dto;

import lombok.Data;

@Data
public class AddressRequestDTO {
    private String street;
    private String city;
    private String postalCode;
    private String country;
}