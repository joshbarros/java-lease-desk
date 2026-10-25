package com.leasedesk.vacate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class VacateController {

    private final VacateRepository vacates;

    public VacateController(VacateRepository vacates) {
        this.vacates = vacates;
    }

    @GetMapping("/vacates/{applicationId}")
    public VacateEntity get(@PathVariable String applicationId) {
        return vacates.findById(applicationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vacate not found"));
    }
}
