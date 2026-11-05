package com.example.gameflix.repository;

import com.example.gameflix.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Handle database access for genre records.
@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    // Return all genres in alphabetical order.
    List<Genre> findAllByOrderByGenreNameAsc();

    // Find a genre by name without requiring matching capitalization.
    Optional<Genre> findByGenreNameIgnoreCase(String genreName);

    // Check whether a genre name is already being used.
    boolean existsByGenreNameIgnoreCase(String genreName);
}