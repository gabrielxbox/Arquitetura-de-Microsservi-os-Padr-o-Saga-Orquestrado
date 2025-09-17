package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import br.com.microservices.orchestrated.orchestratorservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import static org.springframework.util.ObjectUtils.isEmpty;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Log4j2
@Component
@AllArgsConstructor
public class SagaExecutionCtroller {

    private static final String SAGA_LOG_ID = "ORDER ID %s | TRANSACTION ID %s | EVENT ID %s";

    public ETopics getNextTopic(Event event) {
        if (isEmpty(event.getSource())
                || isEmpty(event.getStatus())) {
            throw new ValidationException("Source or Status must be informed!");
        }
      return findTopicBySourceAndStatus(event);
    }

    private  ETopics findTopicBySourceAndStatus(Event event) {
        return(ETopics) Arrays.stream(SagaHandler.SAGA_HANDLER)
                .filter(row -> isEventSourceAndStatusValid(event, row))
                .map( i -> i[SagaHandler.TOPIC_INDEX])
                .findFirst()
                .orElseThrow(() -> new ValidationException("Topic not found!"));
    }

    private boolean isEventSourceAndStatusValid(Event event, Object[] row) {
        var source = row[SagaHandler.EVENT_SOURCE_INDEX];
        var status = row[SagaHandler.SAGA_STATUS_INDEX];


        return event.getSource().equals(source)
                && event.getStatus().equals(status);
    }

    private void logCurrentSaga(Event event, ETopics topic) {
        var sagaId = createSagaId(event);
        var source = event.getSource();
        switch (event.getStatus()) {
            case SUCCESS -> log.info("### CURRENT SAGA: {} | SUCCESS | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
            case ROLLBACK_PENDING -> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK CURRENT SERVICE | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
            case FAIL -> log.info("### CURRENT SAGA: {} | SENDING TO ROLLBACK PREVIOUS SERVICE | NEXT TOPIC {} | {}",
                    source, topic, sagaId);
        }
    }



    private String createSagaId(Event event) {
        return String.format(SAGA_LOG_ID,
                event.getPayload().getId(),
                event.getTransactionId(),
                event.getId());
    }
}
