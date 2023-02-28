package app.music.resourceservice.config.kafka;

import app.music.resourceservice.event.ResourceEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;
import org.springframework.kafka.config.TopicBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class TopicConfiguration {
    @Value("${kafka.topic.resources}")
    private String topicName;
    @Value("${kafka.topic.partitions.count}")
    private int partitionsCount;
    @Value("${kafka.topic.replication.factor}")
    private int replicationFactor;

    @Bean
    public NewTopic resources() {
        return TopicBuilder
                .name(topicName)
                .partitions(partitionsCount)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics() {
        Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> map = new HashMap<>();
        map.put(ResourceEvent.class, Pair.of(topicName, message -> new ProducerRecord<>(topicName,
                String.valueOf(((ResourceEvent) message).resourceId()), message)));

        return map;
    }
}
