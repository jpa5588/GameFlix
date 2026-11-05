package com.example.gameflix.controller;

import com.example.gameflix.model.Genre;
import com.example.gameflix.service.GenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

// Handle API requests for the genres used in our game catalog.
@RestController
@RequestMapping("/api/genres")
public class GenreRestController {

    // Use the service to validate requests and work with genre records.
    @Autowired
    private GenreService genreService;

    // Return all genres in alphabetical order.
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Genre>> getAllGenres() {

        return ResponseEntity.ok(genreService.getAllGenres());
    }

    // Return one genre using the ID supplied in the URL.
    @RequestMapping(value = "/{genreId}", method = RequestMethod.GET)
    public ResponseEntity<Genre> getGenreById(
            @PathVariable("genreId") Integer genreId) {

        return ResponseEntity.ok(
                genreService.getGenreById(genreId)
        );
    }

    // Create a genre after the service validates the request.
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Genre> createGenre(@RequestBody Genre request) {

        Genre genre = genreService.createGenre(request);

        // Point the Location header to the newly created genre.
        URI location = URI.create("/api/genres/" + genre.getGenreId());

        // Return the saved genre with HTTP 201 Created.
        return ResponseEntity.created(location).body(genre);
    }

    // Return service validation errors with their status and message.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleRequestError(
            ResponseStatusException exception) {

        String message = exception.getReason();

        // Provide a fallback if the exception has no message.
        if (message == null) {
            message = "Unable to process the genre request.";
        }

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of("message", message));
    }
}