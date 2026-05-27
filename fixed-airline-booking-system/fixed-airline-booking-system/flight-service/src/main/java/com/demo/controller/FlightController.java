package com.demo.controller;

import com.demo.model.Flight;
import com.demo.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class FlightController {

    @Autowired
    private FlightRepository flightRepository;

    @GetMapping("/flights")
    public List<String> getFlights() {
        return flightRepository.findAll().stream()
                .map(f -> f.getFlightNumber() + " - " + f.getOrigin() + " to " + f.getDestination())
                .collect(Collectors.toList());
    }

    @PostMapping("/flights")
    public Flight createFlight(@RequestBody Flight flight) {
        return flightRepository.save(flight);
    }
}
