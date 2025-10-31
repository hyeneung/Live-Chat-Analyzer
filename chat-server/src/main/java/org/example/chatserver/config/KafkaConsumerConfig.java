package org.example.chatserver.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.chatserver.dto.AnalysisResultDto;
import org.example.chatserver.dto.ChatMessageDto;
import org.example.chatserver.dto.SummaryResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    //<editor-fold desc="Default Consumer Config for ChatMessage">
    @Bean
    @Primary
    public ConsumerFactory<String, ChatMessageDto> chatMessageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Default JsonDeserializer for ChatMessage (expects type headers)
        JsonDeserializer<ChatMessageDto> deserializer = new JsonDeserializer<>(ChatMessageDto.class);
        deserializer.addTrustedPackages("org.example.chatserver.dto");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageDto> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChatMessageDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatMessageConsumerFactory());
        return factory;
    }
    //</editor-fold>

    //<editor-fold desc="Custom Consumer Config for AnalysisResultDto">
    @Bean
    public ConsumerFactory<String, AnalysisResultDto> analysisResultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Custom JsonDeserializer for AnalysisResultDto (ignores type headers)
        JsonDeserializer<AnalysisResultDto> deserializer = new JsonDeserializer<>(AnalysisResultDto.class);
        deserializer.setUseTypeHeaders(false); // Do not look for type info in headers
        deserializer.addTrustedPackages("org.example.chatserver.dto");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnalysisResultDto> analysisResultListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AnalysisResultDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(analysisResultConsumerFactory());
        return factory;
    }
    //</editor-fold>

    //<editor-fold desc="Custom Consumer Config for SummaryResultDto">
    @Bean
    public ConsumerFactory<String, SummaryResultDto> summaryResultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Custom JsonDeserializer for SummaryResultDto (ignores type headers)
        JsonDeserializer<SummaryResultDto> deserializer = new JsonDeserializer<>(SummaryResultDto.class);
        deserializer.setUseTypeHeaders(false); // Do not look for type info in headers
        deserializer.addTrustedPackages("org.example.chatserver.dto");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SummaryResultDto> summaryResultListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SummaryResultDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(summaryResultConsumerFactory());
        return factory;
    }
    //</editor-fold>
}
