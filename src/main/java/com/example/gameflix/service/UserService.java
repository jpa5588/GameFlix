package com.example.gameflix.service;

import com.example.gameflix.model.User;

// Interface to handle the logic for a User login or registration
public interface UserService {
    // Receive the registration information and return the saved account.
    User register(User request);

    // Receive the login information and return the saved account after verification.
    User login(User request);

    // Use the account ID from the verified token to find the current user.
    User getCurrentUser(String subject);

}
