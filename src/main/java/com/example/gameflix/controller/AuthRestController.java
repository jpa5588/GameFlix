package com.example.gameflix.controller;

import com.example.gameflix.model.User;
import com.example.gameflix.service.JwtService;
import com.example.gameflix.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Handle authentication API requests and return the results as JSON.
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    // Use the service to carry out the registration work.
    @Autowired
    private UserService userService;

    // Use the JWT service to create a token after verifying the login.
    @Autowired
    private JwtService jwtService;

    // Handle POST requests to /api/auth/register.
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<User> register(@RequestBody User request) {

        // RequestBody puts the incoming JSON into a User object.
        // The service validates the information and saves the account.
        User user = userService.register(request);

        // Return the saved account with HTTP 201 Created.
        // User's JSON annotations keep both password fields out of the response.
        return new ResponseEntity<User>(user, HttpStatus.CREATED);
    }

    // Handle POST requests to /api/auth/login.
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> login(@RequestBody User request) {

        // Send the login information to the service for verification.
        User user = userService.login(request);

        // Create the JWT after the service verifies the user.
        String token = jwtService.generateToken(user);

        // Put the token  into the response.
        Map<String, String> response = Map.of("accessToken", token);

        // Return the response with HTTP 200
        return new ResponseEntity<Map<String, String>>(
                response, HttpStatus.OK);
    }

    // Spring calls this if a request handled by this controller throws a ResponseStatusException.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleRequestError(
            ResponseStatusException exception) {

        // Get the message explaining what went wrong with the request.
        String message = exception.getReason();

        // If there isn't a message, use this one instead.
        if (message == null) {
            message = "Unable to process the request.";
        }

        return ResponseEntity
                // Return the status from the exception, like 400 or 409.
                .status(exception.getStatusCode())
                // Send the message back as JSON, using "message" as the key.
                .body(Map.of("message", message));
    }
}
