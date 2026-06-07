package eu.forgeops.api.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunOutputRelay {

    private final SimpMessagingTemplate ws;
    private final RedisMessageListenerContainer listenerContainer;

    public void subscribe(UUID runId) {
        String pattern = "forgeops:runs:" + runId + ":output";
        listenerContainer.addMessageListener(
            (message, patternBytes) -> {
                String payload = new String(message.getBody());
                ws.convertAndSend("/topic/runs/" + runId + "/output", payload);
            },
            new PatternTopic(pattern)
        );
        log.debug("Subscribed WebSocket relay for run {}", runId);
    }

    public void unsubscribe(UUID runId) {
        String pattern = "forgeops:runs:" + runId + ":output";
        listenerContainer.removeMessageListener(null, new PatternTopic(pattern));
        log.debug("Unsubscribed WebSocket relay for run {}", runId);
    }
}
