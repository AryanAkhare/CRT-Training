package com.demo.config;

import com.demo.model.Flight;
import com.demo.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private FlightRepository flightRepository;

    @Override
    public void run(String... args) throws Exception {
        if (flightRepository.count() == 0) {
            flightRepository.save(new Flight("AI101", "Pune", "Delhi"));
            flightRepository.save(new Flight("AI202", "Mumbai", "Bangalore"));
        }
    }
}
