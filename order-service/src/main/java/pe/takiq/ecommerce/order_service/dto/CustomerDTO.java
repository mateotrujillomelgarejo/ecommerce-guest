package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class CustomerDTO {

    private String id;

    private String email;
    private String name;
    private String phone;

    private String addressStreet;
    private String addressCity;
    private String addressCountry;
    private String addressZip;
}