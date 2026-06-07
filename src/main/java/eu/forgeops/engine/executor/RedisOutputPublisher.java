package eu.forgeops.engine.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOutputPublisher implements OutputHandler {

    private static final String CHANNEL_PATTERN = "forgeops:runs:%s:output";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void emit(UUID runId, UUID taskId, UUID nodeId, String nodeName,
                     int lineNumber, String level, String message) {
        try {
            Map<String, Object> payload = Map.of(
                "runId", runId.toString(),
                "taskId", taskId != null ? taskId.toString() : "",
                "nodeId", nodeId.toString(),
                "nodeName", nodeName,
                "line", lineNumber,
                "level", level,
                "message", message,
                "ts", Instant.now().toEpochMilli()
            );
            String json = objectMapper.writeValueAsString(payload);
            String channel = CHANNEL_PATTERN.formatted(runId);
            redisTemplate.convertAndSend(channel, json);
        } catch (Exception e) {
            log.warn("Failed to publish run output to Redis: {}", e.getMessage());
        }
    }
}
