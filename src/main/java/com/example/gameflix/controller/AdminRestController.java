package com.example.gameflix.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Handle requests for administrator functions.
@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    // Spring Security checks the ADMIN role before this method runs.
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> check() {

        // Return confirmation that the request passed the permission check.
        return new ResponseEntity<>(
                Map.of("message", "Administrator access confirmed."),
                HttpStatus.OK
        );
    }
}
