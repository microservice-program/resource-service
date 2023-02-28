package app.music.resourceservice.event;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class KafkaManager {
    private static final Logger log = LoggerFactory.getLogger(KafkaManager.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics;


    public void publish(Object message) {
        if (publicationTopics.containsKey(message.getClass())) {
            log.info("publishing {} message to kafka: {}", message.getClass().getName(), message);
            kafkaTemplate.send(publicationTopics.get(message.getClass()).getSecond().apply(message));
        }
    }

    public void publishCallback(Object message) {
        if (publicationTopics.containsKey(message.getClass())) {
            log.info("publishing {} message to kafka: {}", message.getClass().getName(), message);
            var send = kafkaTemplate.send(publicationTopics.get(message.getClass()).getSecond().apply(message));
            send.whenComplete((result, ex) -> {
                log.info("Message sending is completed, data = {}, ex = {}", result, ex);
                if (result != null) {
                    log.info("Successfully published: {} message delivered with offset -{}", result,
                            result.getRecordMetadata().offset());
                }
                if (ex != null) {
                    log.warn("Publishing failed: {} unable to be delivered, {}", message, ex.getMessage());
                }
            });

        }
    }
}
