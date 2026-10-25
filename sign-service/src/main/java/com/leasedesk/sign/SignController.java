package com.leasedesk.sign;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SignController {

    private final SignRepository signs;

    public SignController(SignRepository signs) {
        this.signs = signs;
    }

    @GetMapping("/signatures/{applicationId}")
    public SignEntity get(@PathVariable String applicationId) {
        return signs.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "signature not found"));
    }
}
