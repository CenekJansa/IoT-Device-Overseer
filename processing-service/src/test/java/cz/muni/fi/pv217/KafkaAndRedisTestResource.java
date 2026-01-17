package cz.muni.fi.pv217;

import com.redis.testcontainers.RedisContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

/**
 * Test resource that starts both Kafka and Redis containers for integration tests.
 * This combines KafkaCompanionResource functionality with Redis container.
 */
public class KafkaAndRedisTestResource implements QuarkusTestResourceLifecycleManager {

    private final KafkaCompanionResource kafkaResource = new KafkaCompanionResource();
    private RedisContainer redisContainer;

    @Override
    public Map<String, String> start() {
        // Start Kafka using KafkaCompanionResource
        Map<String, String> kafkaConfig = kafkaResource.start();

        // Start Redis container
        redisContainer = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
        redisContainer.start();

        // Combine configurations
        Map<String, String> config = new HashMap<>(kafkaConfig);

        // Redis configuration
        config.put("quarkus.redis.hosts", "redis://" + redisContainer.getHost() + ":" + redisContainer.getFirstMappedPort());

        return config;
    }

    @Override
    public void stop() {
        kafkaResource.stop();
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    @Override
    public void inject(Object testInstance) {
        kafkaResource.inject(testInstance);
    }
}

