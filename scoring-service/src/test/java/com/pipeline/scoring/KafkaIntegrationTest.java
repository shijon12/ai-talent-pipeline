package com.pipeline.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KafkaIntegrationTest {

    @Container
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1")
    );

    @Test
    void publishesAndConsumesCandidateEvent() throws Exception {
        String topic = "candidate-events-" + UUID.randomUUID();
        Map<String, Object> producerProperties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()
        );
        Map<String, Object> consumerProperties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "integration-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );

        try (
                KafkaProducer<String, String> producer = new KafkaProducer<>(
                        producerProperties,
                        new StringSerializer(),
                        new StringSerializer()
                );
                KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                        consumerProperties,
                        new StringDeserializer(),
                        new StringDeserializer()
                )
        ) {
            consumer.subscribe(List.of(topic));
            producer.send(new ProducerRecord<>(
                    topic,
                    "candidate-001",
                    "{\"id\":\"candidate-001\",\"targetJobId\":\"job-101\"}"
            )).get();

            var records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records)
                    .extracting(record -> record.key())
                    .contains("candidate-001");
        }
    }
}
