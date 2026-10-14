package com.example.gameflix.controller;

import com.example.gameflix.model.User;
import com.example.gameflix.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Handle authentication API requests for /api/users
@RestController
@RequestMapping("/api/users")
public class UsersRestController {

    // Use the service to carry out the user lookup work.
    @Autowired
    private UserService userService;

    // Handle GET requests to /api/users/me.
    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public ResponseEntity<User> me(@AuthenticationPrincipal Jwt jwt) {

        // Spring supplies the JWT after checking it through the security filter.
        // Require an authenticated JWT before looking up an account.
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        // Get the account ID from the token's subject line.
        // Service uses that ID to retrieve the caller's own account.
        User user = userService.getCurrentUser(jwt.getSubject());



        // I return the profile with HTTP 200.
        // User's JSON annotations hide the password and password hash.
        return new ResponseEntity<User>(user, HttpStatus.OK);
    }

    // Spring calls this if a request handled by this controller throws a ResponseStatusException.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleRequestError(
            ResponseStatusException exception) {

        // Get the message explaining what went wrong with the request.
        String message = exception.getReason();

        // Use this message if the exception did not provide one.
        if (message == null) {
            message = "Authentication is required.";
        }

        // I return the error message and the status from the exception.
        return new ResponseEntity<Map<String, String>>(
                Map.of("message", message),
                exception.getStatusCode());
    }
}