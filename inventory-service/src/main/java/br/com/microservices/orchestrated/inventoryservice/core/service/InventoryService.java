package br.com.microservices.orchestrated.inventoryservice.core.service;

import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dto.History;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaPrtoducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Log4j2
@Service
@AllArgsConstructor
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaPrtoducer kafkaProducer;
    private final InventoryRepository inventoryRepository;
    private final OderInventoryRepository oderInventoryRepository;

    public void updateInventory(Event event) {
        try {
            checkCurrentValidation(event);
            criateOrderInvesntory(event);
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to update inventory {}. Exception: {}", event, ex);
            handleFailCurrentNotExexecuted(event, ex.getMessage());

        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void updateInventory(Order order) {
        order.getProducts().forEach(product -> {
            var inventory = findInventoryByProductCode(product.getProduct().getCode());
            ckeckInventory(inventory.getAvailable(), product.getQuantity());
            inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
            inventoryRepository.save(inventory);
        });
    }

    public void ckeckInventory(int available, int orderQuantity){
        if (orderQuantity > available)
            throw new ValidationException("Product is out of stock!");
    }

    private void handleSuccess(Event event) {
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "inventory realized successfully!");
    }
    private void criateOrderInvesntory(Event event) {
        event
                .getPayload()
                .getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    var oderInventory = criateOrderInvesntory(event, product, inventory);
                });
    }

    private OderInventory criateOrderInvesntory(Event event, OrderProducts product, Inventory inventory) {
        return OderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(product.getQuantity())
                .newQuantity(inventory.getAvailable() - product.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();
    }


    private void checkCurrentValidation(Event event) {
        if (oderInventoryRepository.existsByOrderIdAndTransactionId(
                event.getPayload().getId(), event.getTransactionId())) {
            throw new ValidationException("There's another transactionId for this validation!");
        }
    }

    private Inventory findInventoryByProductCode(String productCode) {
        return inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Inventory not found for product code: ".concat(productCode)));
    }

    private void handleFailCurrentNotExexecuted(Event event, String message) {
        event.setStatus(ESagaStatus.ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to update inventory: ".concat(message));
    }

    public void  rollbackInventory(Event event) {
        event.setStatus(ESagaStatus.FAIL);
        event.setSource(CURRENT_SOURCE);
        try{
            returnInventorytoPreviousValues(event);
            addHistory(event, "Rollback executed for inventory!");
        } catch (Exception ex) {
            addHistory(event, "Rollback not executed for inventory: ".concat(ex.getMessage()));
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void returnInventorytoPreviousValues(Event event) {
        oderInventoryRepository.findByOrderIdAndTransactionId(
                        event.getPayload().getId(), event.getTransactionId())
                .forEach(oderInventory -> {
                    var inventory = oderInventory.getInventory();
                    inventory.setAvailable(oderInventory.getOldQuantity());
                    inventoryRepository.save(inventory);
                    log.info("Retorned inventory for oder {}, from {} to {} ",
                            event.getPayload().getId(),
                            oderInventory.getOldQuantity(),
                            inventory.getAvailable());
                });
    }

    private void addHistory(Event event, String message) {
        var history = History.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addEventHistory(history);
    }

}
