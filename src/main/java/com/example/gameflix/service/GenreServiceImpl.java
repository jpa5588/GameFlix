package com.example.gameflix.service;

import com.example.gameflix.model.Genre;
import com.example.gameflix.repository.GenreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Carry out the operations defined in GenreService.
@Service
public class GenreServiceImpl implements GenreService {

    // Use the repository to look up and save genre records.
    @Autowired
    private GenreRepository genreRepository;

    @Override
    public List<Genre> getAllGenres() {

        // Return the genres sorted by their names.
        return genreRepository.findAllByOrderByGenreNameAsc();
    }

    @Override
    public Genre getGenreById(Integer genreId) {

        // A database ID must be a positive number.
        if (genreId == null || genreId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid genre ID is required."
            );
        }

        // Return the genre or report that the ID was not found.
        return genreRepository.findById(genreId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Genre was not found."
                ));
    }

    @Override
    public Genre createGenre(Genre request) {

        // Make sure genre information was provided.
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Genre information is required."
            );
        }

        // Require a name containing something other than whitespace.
        if (request.getGenreName() == null
                || request.getGenreName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Genre name is required."
            );
        }

        // Remove spaces from the beginning and end of the name.
        String genreName = request.getGenreName().trim();

        // Match the maximum length defined in our Genre entity.
        if (genreName.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Genre name must be 100 characters or fewer."
            );
        }

        // Check for an existing name without requiring matching capitalization.
        if (genreRepository.existsByGenreNameIgnoreCase(genreName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A genre with this name already exists."
            );
        }

        // Create a fresh record so the request cannot supply an existing ID.
        Genre genre = new Genre();
        genre.setGenreName(genreName);

        try {
            // Save and flush so database constraint errors are raised here.
            return genreRepository.saveAndFlush(genre);

        } catch (DataIntegrityViolationException exception) {

            // Another request might have saved this name after our first check.
            if (genreRepository.existsByGenreNameIgnoreCase(genreName)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A genre with this name already exists."
                );
            }

            // Let unrelated database errors follow the normal error handling.
            throw exception;
        }
    }
}