package org.example.chatserver.config;

import lombok.RequiredArgsConstructor;
import org.example.chatserver.interceptor.AuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration class for setting up WebSocket and STOMP messaging.
 */
@Configuration
@EnableWebSocketMessageBroker // Enables WebSocket message handling, backed by a message broker.
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;

    /**
     * Configures the message broker.
     * @param registry The registry for message broker configuration.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // frontend code : this.stompClient.subscribe(/topic/${this.streamId}, message => { ...
        registry.enableSimpleBroker(WebSocketConstants.TOPIC_PREFIX);

        // Designates the "/publish" prefix for messages that are bound for @MessageMapping-annotated methods.
        // frontend code : this.stompClient.send("/publish/{@MessageMapping endpoint}", headers, message);
        registry.setApplicationDestinationPrefixes(WebSocketConstants.APP_PREFIX);
    }

    /**
     * Registers the STOMP endpoints.
     * @param registry The registry for STOMP endpoint configuration.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registers the "/ws" endpoint, enabling SockJS fallback options so that alternate transports can be used if WebSocket is not available.
        // frontend code :  const socket = new SockJS(${process.env.VUE_APP_BACKEND_URL}/ws)
        registry.addEndpoint(WebSocketConstants.WEBSOCKET_ENDPOINT)
                .setAllowedOriginPatterns("*") // localhost:3000
                .withSockJS();
    }

    /**
     * Configures the client inbound channel to intercept messages for authentication.
     * @param registration The registration for channel configuration.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Registers the custom channel interceptor to validate JWT tokens on CONNECT messages.
        registration.interceptors(authChannelInterceptor);
    }
}
