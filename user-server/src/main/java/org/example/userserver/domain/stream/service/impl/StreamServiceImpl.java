package org.example.userserver.domain.stream.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userserver.domain.stream.dto.response.ReadStreamListResponseDto;
import org.example.userserver.domain.stream.dto.response.StreamUserCountUpdateDto;
import org.example.userserver.domain.stream.entity.Stream;
import org.example.userserver.domain.stream.repository.StreamRepository;
import org.example.userserver.domain.stream.service.StreamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service implementation for stream-related operations.
 * This includes managing stream data, handling user interactions like entering a stream,
 * and broadcasting real-time updates via Server-Sent Events (SSE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamServiceImpl implements StreamService {

    private final StreamRepository streamRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis-channel}")
    private String streamUpdateChannel;

    // A thread-safe list to store active SSE emitters for broadcasting events.
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private static final String USER_SET_PREFIX = "stream:users:";

    /**
     * Retrieves a paginated list of all available streams.
     *
     * @param pageable Pagination information.
     * @return A DTO containing the list of streams and pagination details.
     */
    @Override
    public ReadStreamListResponseDto getAllStreams(Pageable pageable) {
        Slice<Stream> streams = streamRepository.findAllWithHost(pageable);
        List<ReadStreamListResponseDto.StreamDto> streamDtos = streams.getContent()
            .stream()
            .map(stream -> {
                long viewerCount = getStreamViewerCount(String.valueOf(stream.getId()));
                return ReadStreamListResponseDto.StreamDto.from(stream, viewerCount);
            }).toList();

        return ReadStreamListResponseDto.builder()
            .streams(streamDtos)
            .hasNext(streams.hasNext())
            .numberOfElements(streams.getNumberOfElements())
            .pageNumber(streams.getNumber())
            .pageSize(streams.getSize())
            .build();
    }

    /**
     * Handles a user entering a stream room.
     * It adds the user's ID to a Redis Set for the given stream
     * and publishes a message to a Redis channel to notify of the update.
     *
     * @param userId The ID of the user entering the stream.
     * @param streamId The ID of the stream being entered.
     */
    @Override
    @Transactional
    public void enterStream(Long userId, String streamId) {
        String key = USER_SET_PREFIX + streamId;
        redisTemplate.opsForSet().add(key, String.valueOf(userId));
        Long userCount = redisTemplate.opsForSet().size(key);
        if (userCount == null) {
            userCount = 0L;
        }
        StreamUserCountUpdateDto dto = StreamUserCountUpdateDto.builder()
                .streamId(streamId)
                .userCount(userCount)
                .build();
        try {
            String message = objectMapper.writeValueAsString(dto);
            redisTemplate.convertAndSend(streamUpdateChannel, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing stream update DTO", e);
        }
    }

    /**
     * Creates and registers a new SSE emitter for a client to subscribe to stream updates.
     * The emitter is stored in a central list to receive broadcasted events.
     *
     * @return The SseEmitter instance for the client.
     */
    @Override
    public SseEmitter subscribe() {
        // Create an emitter with a very long timeout.
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.info("SSE connection completed. Removing emitter.");
            this.emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out. Removing emitter.");
            emitter.complete();
            this.emitters.remove(emitter);
        });

        emitters.add(emitter);


        try {
            // Send an initial event to confirm the connection.
            emitter.send(SseEmitter.event().name("connect").data("SSE connection established."));
        } catch (IOException e) {
            // If sending fails, remove the emitter immediately.
            log.warn("Client disconnected right after subscribing. Completing emitter.", e);
            emitter.complete();
        }

        return emitter;
    }

    /**
     * Notifies all subscribed clients of a user count update for a specific stream.
     * This method is called by the Redis subscriber when an update message is received.
     *
     * @param dto The DTO containing the stream ID and the new user count.
     */
    @Override
    // this doesn't need db transaction. so exclude it
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notifyUserCountUpdate(StreamUserCountUpdateDto dto) {
        // Iterate over a copy of the emitters list to avoid ConcurrentModificationException.
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                // Send the update event to each client.
                emitter.send(SseEmitter.event().name("userCountUpdate").data(dto));
            } catch (IOException e) {
                log.info("Client disconnected. Emitter will be removed.");
                // If an error occurs (e.g., client disconnected), mark the emitter for removal.
                deadEmitters.add(emitter);
            }
        });

        // Remove all dead emitters from the main list.
        emitters.removeAll(deadEmitters);
    }

    @Override
    public long getStreamViewerCount(String streamId) {
        String key = USER_SET_PREFIX + streamId;
        Long userCount = redisTemplate.opsForSet().size(key);
        return userCount != null ? userCount : 0L;
    }
}