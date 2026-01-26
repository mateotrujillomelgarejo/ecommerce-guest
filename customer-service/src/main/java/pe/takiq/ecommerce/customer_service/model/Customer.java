package pe.takiq.ecommerce.customer_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Customer {

    @Id
    private String id = UUID.randomUUID().toString();

    private String email;
    private String name;
    private String phone;

    private String addressStreet;
    private String addressCity;
    private String addressCountry;
    private String addressZip;
}