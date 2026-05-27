package com.demo.config;

import com.demo.model.Seat;
import com.demo.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private SeatRepository seatRepository;

    @Override
    public void run(String... args) throws Exception {
        if (seatRepository.count() == 0) {
            seatRepository.save(new Seat("A1", false));
            seatRepository.save(new Seat("A2", false));
            seatRepository.save(new Seat("B1", false));
            seatRepository.save(new Seat("B2", false));
        }
    }
}
