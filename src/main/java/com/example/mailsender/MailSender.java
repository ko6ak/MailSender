package com.example.mailsender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.example.mailsender.config.KafkaTopicConfig.RESPONSE_TOPIC_NAME;

@Component
@AllArgsConstructor
public class MailSender {
    public static final String REQUEST_TOPIC_NAME = "request-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = REQUEST_TOPIC_NAME)
    public void listener(String email) {
        sender(email);
    }

    private int confirmationCodeGenerator(){
        return 100_000 + new Random().nextInt(900_000);
    }

    private void sender(String email) {
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

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(RESPONSE_TOPIC_NAME, st);

        future.whenComplete((result, error) -> {
            if (error != null) System.err.println();
            System.out.println(result);
        });
    }
}
