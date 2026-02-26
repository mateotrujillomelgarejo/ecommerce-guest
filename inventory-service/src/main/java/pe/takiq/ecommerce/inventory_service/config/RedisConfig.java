package pe.takiq.ecommerce.inventory_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer()); 
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer()); 
        return template;
    }

    @Bean
    public DefaultRedisScript<String> reserveStockScript() {
        String script =
            "local num_items = #KEYS\n" +
            "local order_id = ARGV[#ARGV]\n" +
            "for i=1, num_items do\n" +
            "   local current = redis.call('GET', KEYS[i])\n" +
            "   local requested = ARGV[i]\n" +
            "   if not current then return 'INSUFFICIENT_STOCK:' .. KEYS[i] end\n" +
            "   if tonumber(current) < tonumber(requested) then return 'INSUFFICIENT_STOCK:' .. KEYS[i] end\n" +
            "end\n" +
            "for i=1, num_items do\n" +
            "   redis.call('DECRBY', KEYS[i], tonumber(ARGV[i]))\n" +
            "   redis.call('HSET', 'reserve:' .. order_id, KEYS[i], tonumber(ARGV[i]))\n" +
            "end\n" +
            "redis.call('EXPIRE', 'reserve:' .. order_id, 1800)\n" +
            "return 'OK'";
        return new DefaultRedisScript<>(script, String.class);
    }

    @Bean
    public DefaultRedisScript<String> releaseStockScript() {
        String script =
            "local order_id = ARGV[1]\n" +
            "local reserve_key = 'reserve:' .. order_id\n" +
            "local exists = redis.call('EXISTS', reserve_key)\n" +
            "if exists == 0 then return 'NOT_FOUND' end\n" +
            "local items = redis.call('HGETALL', reserve_key)\n" +
            "for i=1, #items, 2 do\n" +
            "   redis.call('INCRBY', items[i], tonumber(items[i+1]))\n" +
            "end\n" +
            "redis.call('DEL', reserve_key)\n" +
            "return 'OK'";
        return new DefaultRedisScript<>(script, String.class);
    }
}