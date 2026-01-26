package pe.takiq.ecommerce.customer_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.customer_service.model.Customer;
import pe.takiq.ecommerce.customer_service.repository.CustomerRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;

    public Customer createGuest(String email, String name, String phone) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setName(name);
        customer.setPhone(phone);
        return repository.save(customer);
    }

    public Optional<Customer> findById(String id) {
        return repository.findById(id);
    }

    public Optional<Customer> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Customer update(String id, Customer updated) {
        Customer existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        // Actualiza solo campos no nulos
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
        // Agrega address si existe
        return repository.save(existing);
    }
}