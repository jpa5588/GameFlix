package com.example.gameflix.repository;

import com.example.gameflix.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    //During login find the user by email
    Optional<User> findByEmail(String email);

    //Boolean check to determine if the account email does exist, if it does value should be true
    boolean existsByEmail(String email);
}
