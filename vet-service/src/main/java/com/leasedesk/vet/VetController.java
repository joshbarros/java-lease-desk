package com.leasedesk.vet;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class VetController {

    private final VetRepository vets;

    public VetController(VetRepository vets) {
        this.vets = vets;
    }

    @GetMapping("/vets/{applicationId}")
    public VetEntity get(@PathVariable String applicationId) {
        return vets.findById(applicationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vet not found"));
    }
}
