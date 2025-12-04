package com.compuinside.auth.kafka;

import com.compuinside.auth.dto.UserEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Profile("!disable-kafka")
@Service
@RequiredArgsConstructor
    public class KafkaProducerService {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private static final String TOPIC = "user-events";

    public void sendUserEvent(UserEvent userEvent) {
        System.out.println("Enviando evento de usuario: " + userEvent);
        kafkaTemplate.send(TOPIC, userEvent);
    }
}
