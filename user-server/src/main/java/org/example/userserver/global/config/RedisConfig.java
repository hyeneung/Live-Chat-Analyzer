package org.example.userserver.global.config;

import lombok.RequiredArgsConstructor;
import org.example.userserver.global.redis.RedisSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration class for Redis Pub/Sub.
 * This class sets up the necessary components to listen for messages from Redis channels.
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisSubscriber redisSubscriber;

    @Value("${app.redis-channel}")
    private String streamUpdateChannel;


    /**
     * Creates and configures the container for Redis message listeners.
     * This container manages the lifecycle of listeners and message dispatching.
     * As a user unfamiliar with Redis, you can think of this as setting up
     * a "listener" that waits for messages on a specific "channel" (like a radio frequency).
     *
     * @param connectionFactory The Redis connection factory provided by Spring Boot.
     * @return A configured RedisMessageListenerContainer bean.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        // Set the connection factory for the container.
        container.setConnectionFactory(connectionFactory);
        // Register our custom RedisSubscriber to listen for messages on the specified channel.
        // When a message appears on the 'streamUpdateChannel', the 'redisSubscriber' will be notified.
        container.addMessageListener(redisSubscriber, new ChannelTopic(streamUpdateChannel));
        return container;
    }
}
