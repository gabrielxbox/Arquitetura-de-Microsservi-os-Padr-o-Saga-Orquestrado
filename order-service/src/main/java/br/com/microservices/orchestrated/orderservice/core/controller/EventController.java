package br.com.microservices.orchestrated.orderservice.core.controller;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.service.EventService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/event")
@AllArgsConstructor
public class EventController {

    private final EventService eventService;

    public ResponseEntity<Event> findByFilters(@RequestBody @Valid EventFilters filters) {
       var event = eventService.findByFilters(filters);
        return new ResponseEntity<>(event, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Event>> findAll() {
        var events = eventService.findAll();
        return new ResponseEntity<>(events, HttpStatus.OK);
    }
}
