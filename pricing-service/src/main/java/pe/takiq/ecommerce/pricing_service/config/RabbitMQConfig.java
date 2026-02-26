package pe.takiq.ecommerce.pricing_service.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {
@Bean
public Jackson2JsonMessageConverter jsonMessageConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule()); // Soporte para LocalDateTime
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Formato ISO-8601 (String)

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); 

    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
    converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    return converter;
}
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
}