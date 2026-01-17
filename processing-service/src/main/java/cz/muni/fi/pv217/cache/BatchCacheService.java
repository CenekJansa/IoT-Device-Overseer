package cz.muni.fi.pv217.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.muni.fi.pv217.entity.vo.InputEventVo;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Service for caching batch data in Redis.
 * Stores the list of InputEventVo objects associated with a batch ID.
 */
@ApplicationScoped
public class BatchCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(BatchCacheService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "batch:";

    private final ValueCommands<String, String> commands;
    private final ObjectMapper objectMapper;

    @Inject
    public BatchCacheService(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.commands = redisDataSource.value(String.class);
        this.objectMapper = objectMapper;
        // Register JavaTimeModule to support Java 8 date/time types like Instant
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Stores a batch of events in Redis cache with the given batch ID.
     *
     * @param batchId The unique batch identifier
     * @param events The list of events to cache
     */
    public void storeBatch(String batchId, List<InputEventVo> events) {
        if (batchId == null || batchId.isEmpty()) {
            LOG.error("Cannot store batch with null or empty batch ID");
            return;
        }
        if (events == null || events.isEmpty()) {
            LOG.warn("Storing empty batch for batch ID: {}", batchId);
        }

        try {
            String key = KEY_PREFIX + batchId;
            String jsonValue = objectMapper.writeValueAsString(events);
            commands.setex(key, CACHE_TTL.getSeconds(), jsonValue);
            LOG.debug("Stored batch {} with {} events in Redis cache", batchId, events != null ? events.size() : 0);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize batch {} to JSON. Error: {}", batchId, e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Failed to store batch {} in Redis. Error: {}", batchId, e.getMessage(), e);
        }
    }

    /**
     * Retrieves a batch of events from Redis cache by batch ID.
     *
     * @param batchId The unique batch identifier
     * @return The list of cached events, or null if not found or error occurs
     */
    public List<InputEventVo> retrieveBatch(String batchId) {
        if (batchId == null || batchId.isEmpty()) {
            LOG.error("Cannot retrieve batch with null or empty batch ID");
            return null;
        }

        try {
            String key = KEY_PREFIX + batchId;
            String jsonValue = commands.get(key);
            
            if (jsonValue == null) {
                LOG.warn("No batch found in cache for batch ID: {}", batchId);
                return null;
            }

            List<InputEventVo> events = objectMapper.readValue(
                jsonValue, 
                new TypeReference<List<InputEventVo>>() {}
            );
            LOG.debug("Retrieved batch {} with {} events from Redis cache", batchId, events.size());
            return events;
        } catch (JsonProcessingException e) {
            LOG.error("Failed to deserialize batch {} from JSON. Error: {}", batchId, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            LOG.error("Failed to retrieve batch {} from Redis. Error: {}", batchId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Removes a batch from Redis cache.
     *
     * @param batchId The unique batch identifier
     */
    public void removeBatch(String batchId) {
        if (batchId == null || batchId.isEmpty()) {
            LOG.error("Cannot remove batch with null or empty batch ID");
            return;
        }

        try {
            String key = KEY_PREFIX + batchId;
            commands.getdel(key);
            LOG.debug("Removed batch {} from Redis cache", batchId);
        } catch (Exception e) {
            LOG.error("Failed to remove batch {} from Redis. Error: {}", batchId, e.getMessage(), e);
        }
    }
}

