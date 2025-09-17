package br.com.microservices.orchestrated.inventoryservice.core.dto;

import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private String id;

    private String transactionId;

    private Order payload;

    private String source;

    private ESagaStatus status;

    private List<History> eventHistory;

    private LocalDateTime createdAt;

    public void addEventHistory(History history) {
        if (CollectionUtils.isEmpty(eventHistory)) {
            this.eventHistory = Collections.emptyList();
        }
        this.eventHistory.add(history);
    }
}
