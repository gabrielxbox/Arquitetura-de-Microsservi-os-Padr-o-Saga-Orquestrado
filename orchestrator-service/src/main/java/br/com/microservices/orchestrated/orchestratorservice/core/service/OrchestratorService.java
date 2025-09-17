package br.com.microservices.orchestrated.orchestratorservice.core.service;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.History;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSorce;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import br.com.microservices.orchestrated.orchestratorservice.core.producer.SagaOrchestratorPrtoducer;
import br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaExecutionCtroller;
import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
@Log4j2
public class OrchestratorService {

    private final JsonUtil jsonUtil;
    private final SagaOrchestratorPrtoducer prtoducer;
    private final SagaExecutionCtroller sagaExecutionCtroller;

    public void startSaga(Event event) {
      event.setSource(EEventSorce.ORCHESTRATOR);
      event.setStatus(ESagaStatus.SUCCESS);
      var topic = getTopic(event);
      log.info("SAGA STARTED");
      addHistory(event,"saga started");
        sandToProducerWithTopic(event, topic);
    }

    public void continueSaga(Event event) {
        var topic = getTopic(event);
        log.info("SAGA CONTINUE FOR EVENT ID {} TO TOPIC {}", event.getId(), topic);
        sandToProducerWithTopic(event, topic);
    }

    public void  finishSagaSuccess(Event event) {
        event.setSource(EEventSorce.ORCHESTRATOR);
        event.setStatus(ESagaStatus.SUCCESS);
        log.info("SAGA FINISHED SUCCESS FOR EVENT ID {}", event.getId());
        addHistory(event,"saga finished success");
       notifyFinishedSaga(event);
    }

    private void notifyFinishedSaga(Event event) {
        sandToProducerWithTopic(event, ETopics.NOTIFY_ENDING);
    }

    public void  finishSagaFail(Event event) {
        event.setSource(EEventSorce.ORCHESTRATOR);
        event.setStatus(ESagaStatus.FAIL);
        log.info("SAGA FINISHED WITH ERROS FOR EVENT ID {}", event.getId());
        addHistory(event,"saga finished whith errors");
        notifyFinishedSaga(event);
    }

    private ETopics getTopic(Event event) {
        return  sagaExecutionCtroller.getNextTopic(event);
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    private void sandToProducerWithTopic(Event event, ETopics topic) {
        prtoducer.sendEvent(jsonUtil.toJson(event), topic.getTopic());
    }

}
