package com.example.mailsender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class MailSender {
    public static final String REQUEST_TOPIC_NAME = "request-topic";
    public static final String RESPONSE_TOPIC_NAME = "response-topic";

    public void listener() {
        KafkaConsumer<String, String> kafkaConsumer = getKafkaConsumer();
        try {
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMinutes(10));
                for (ConsumerRecord<String, String> message: records){
                    sender(message.value());
                }
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally {
            kafkaConsumer.close();
        }
    }

    private int confirmationCodeGenerator(){
        return 100_000 + new Random().nextInt(900_000);
    }

    private void sender(String email) {
        KafkaProducer<String, String> kafkaProducer = getKafkaProducer();

        String confirmationCode = String.valueOf(confirmationCodeGenerator());
        System.out.printf("Выслан код подтверждения %s на почту %s.%n", confirmationCode, email);

        Map<String, String> map = new HashMap<>();
        map.put(email, confirmationCode);

        String st;
        ObjectMapper mapper = new ObjectMapper();
        try {
            st = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(RESPONSE_TOPIC_NAME, st);

        kafkaProducer.send(record);

        kafkaProducer.flush();
        kafkaProducer.close();
    }

    private static KafkaConsumer<String, String> getKafkaConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("group.id", "consuming");

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<> (properties);
        List<String> topics = new ArrayList<>();
        topics.add(REQUEST_TOPIC_NAME);
        kafkaConsumer.subscribe(topics);

        return kafkaConsumer;
    }

    private static KafkaProducer<String, String> getKafkaProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return new KafkaProducer<>(properties);
    }
}
