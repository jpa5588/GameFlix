package com.example.gameflix.service;

import com.example.gameflix.model.Genre;

import java.util.List;

// Define the operations available for managing genres.
public interface GenreService {

    // Return all genres in alphabetical order.
    List<Genre> getAllGenres();

    // Find one genre using its database ID.
    Genre getGenreById(Integer genreId);

    // Validate and save a new genre.
    Genre createGenre(Genre request);
}