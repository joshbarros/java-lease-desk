package com.leasedesk.deed;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DeedController {

    private final DeedRepository deeds;

    public DeedController(DeedRepository deeds) {
        this.deeds = deeds;
    }

    @GetMapping("/deeds/{applicationId}")
    public DeedEntity get(@PathVariable String applicationId) {
        return deeds.findById(applicationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "deed not found"));
    }
}
