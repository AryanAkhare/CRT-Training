package com.demo.controller;

import com.demo.client.PaymentClient;
import com.demo.model.Ticket;
import com.demo.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TicketController {

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private TicketRepository ticketRepository;

    @PostMapping("/tickets/book")
    public Map<String, Object> bookTicket() {
        String paymentStatus = paymentClient.pay();
        Ticket ticket = new Ticket("BOOKED", paymentStatus);
        ticket = ticketRepository.save(ticket);

        Map<String, Object> response = new HashMap<>();
        response.put("ticketId", ticket.getId());
        response.put("status", ticket.getStatus());
        response.put("paymentStatus", ticket.getPaymentStatus());
        return response;
    }

    @GetMapping("/tickets")
    public List<Ticket> getTickets() {
        return ticketRepository.findAll();
    }
}
