package br.com.microservices.orchestrated.paymentservice.core.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaPrtoducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.orchestrator}")
    private String orchestrator;


    public void sandEvent(String payload) {
        try {
        log.info("Sending event to topic {}: {}", orchestrator, payload);
        kafkaTemplate.send(orchestrator, payload);
        } catch (Exception e) {
            log.error("Error trying to send data to topic {}, with data {}, exeception {}", orchestrator, payload, e);
        }
    }

}
