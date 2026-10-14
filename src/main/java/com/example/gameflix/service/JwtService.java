package com.example.gameflix.service;

import com.example.gameflix.model.User;

//Interface to define the method for creating a JWT.
public interface JwtService {

    //Pass in the user after their login information has been verified and return the signed JWT as a string
    String generateToken(User user);
}
