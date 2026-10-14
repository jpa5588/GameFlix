package com.example.gameflix.service;

import com.example.gameflix.model.User;
import com.example.gameflix.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

// Spring uses this class to carry out the work listed in UserService.
@Service
public class UserServiceImpl implements UserService {

    // Use the repository to look up and save accounts.
    @Autowired
    private UserRepository userRepository;

    // Use the password encoder bean to hash passwords before saving them.
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User register(User request) {

        // Make sure registration information was provided.
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Registration information is required.");
        }

        // Check that the display name was provided.
        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Display name is required.");
        }

        // Remove spaces from the beginning and end of the display name.
        String displayName = request.getDisplayName().trim();

        if (displayName.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Display name must be 100 characters or fewer.");
        }

        // Check that an email was provided.
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email is required.");
        }

        // Remove spaces at either end and store emails in lowercase.
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (email.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email must be 255 characters or fewer.");
        }

        // Check that the email has a basic email format.
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        }

        // Keep the password exactly as it was entered.
        String password = request.getPassword();

        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password is required.");
        }

        // Keep the password length checks from our registration setup.
        if (password.length() < 15 || password.length() > 64) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password must be between 15 and 64 characters.");
        }

        // BCrypt has a 72-byte limit. Some characters use more than one byte.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password must be 72 UTF-8 bytes or fewer.");
        }

        // Stop registration if this email already belongs to an account.
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        // Create a fresh account using only the information we need.
        // The server sets the ID, status, and creation time.
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus("ACTIVE");

        try {
            // Save the account and return it. User hides both password fields in JSON.
            // Flush now so database constraint errors are handled here.
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {

            // Two requests could try to register the same email at the same time.
            // Return 409 if another request saved the email before this one.
            if (userRepository.existsByEmail(email)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An account with this email already exists.");
            }

            // Let other database errors continue through the normal error handling.
            throw exception;
        }
    }

    @Override
    public User login(User request){

        //Error message to tell us something is wrong with Email or Password
        String errorMessage = "Invalid Email or Password";

        //Checks if the request variable is blank, or the email and password fields are null or blank
        //If true throw our error message
        if(request == null || request.getEmail() == null || request.getEmail().isBlank() || request.getPassword() == null || request.getPassword().isBlank()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errorMessage);
        }

        //Format the email the same way I did during registration.
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        //Keep the password exactly as it was entered.
        String password = request.getPassword();

        //Checks if the email is within length, password is within length, or the byte size is within length
        //If any are fail throw our error message
        if (email.length() > 255 || password.length() > 64 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errorMessage);
        }

        //Look up the account by email. If none is found, user will be null.
        User user = userRepository.findByEmail(email).orElse(null);

        //Check the login if the account was not found, throw our errormessage
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errorMessage);
        }

        //Check the submitted password with the hash stored in MySQL.
        boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());

        //Only allow login when the password matches and the account is active.
        if (!passwordMatches || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errorMessage);
        }

        //Return the verified database account for the login controller to use.
        return user;

    }

    @Override
    public User getCurrentUser(String subject) {

        // Declare the account ID that I will use for the database lookup, store as Integer to easy method to do lookup
        Integer accountId;

        try {
            // The JWT stores the account ID as text, so convert it to an Integer.
            accountId = Integer.valueOf(subject);

        } catch (NumberFormatException exception) {

            // I reject a subject that cannot be used as an account ID.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        // Find the account using the ID from the verified token, if not value then return null
        User user = userRepository.findById(accountId).orElse(null);

        // Check if the required account exists and is active by grabbing the getStatus column.
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        // Return the saved account to the controller.
        return user;
    }
}
