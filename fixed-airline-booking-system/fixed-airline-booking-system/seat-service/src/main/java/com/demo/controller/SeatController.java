package com.demo.controller;

import com.demo.model.Seat;
import com.demo.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SeatController {

    @Autowired
    private SeatRepository seatRepository;

    @GetMapping("/seats")
    public List<String> getSeats() {
        return seatRepository.findAll().stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList());
    }

    @PostMapping("/seats")
    public Seat createSeat(@RequestBody Seat seat) {
        return seatRepository.save(seat);
    }
}
