package org.example.userserver.global.dataloader;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class RedisDummyDataLoader implements ApplicationRunner {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String USER_SET_PREFIX = "stream:users:";
    private static final int MIN_STREAM_ID = 1;
    private static final int MAX_STREAM_ID = 6;
    private static final int MIN_USER_ID = 7;
    private static final int MAX_USER_ID = 40;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Loading dummy Redis data with even distribution...");

        // Clear existing dummy data to ensure clean state for redistribution
        for (int streamId = MIN_STREAM_ID; streamId <= MAX_STREAM_ID; streamId++) {
            String key = USER_SET_PREFIX + streamId;
            redisTemplate.delete(key);
        }

        Map<Integer, Set<Integer>> streamUserMap = new HashMap<>();
        for (int i = MIN_STREAM_ID; i <= MAX_STREAM_ID; i++) {
            streamUserMap.put(i, new HashSet<>());
        }

        for (int userId = MIN_USER_ID; userId <= MAX_USER_ID; userId++) {
            int streamId = (userId - MIN_USER_ID) % (MAX_STREAM_ID - MIN_STREAM_ID + 1) + MIN_STREAM_ID;
            String key = USER_SET_PREFIX + streamId;
            redisTemplate.opsForSet().add(key, String.valueOf(userId));
            streamUserMap.get(streamId).add(userId);
        }

        for (Map.Entry<Integer, Set<Integer>> entry : streamUserMap.entrySet()) {
            System.out.println("Stream " + entry.getKey() + " has " + entry.getValue().size() + " users: " + entry.getValue());
        }
        System.out.println("Dummy Redis data loaded with even distribution.");
    }
}