package eu.forgeops;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ForgeOpsApplicationTests {

    @MockBean
    StringRedisTemplate stringRedisTemplate;

    @MockBean
    RedisMessageListenerContainer redisMessageListenerContainer;

    @Test
    void contextLoads() {
    }
}
