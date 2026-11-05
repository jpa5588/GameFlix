package com.example.gameflix.service;

import com.example.gameflix.model.Game;
import com.example.gameflix.model.Genre;
import com.example.gameflix.repository.GameRepository;
import com.example.gameflix.repository.GenreRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Carry out the operations defined in GameService.
@Service
@Transactional(readOnly = true)
public class GameServiceImpl implements GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Override
    public List<Game> searchGames(
            String search,
            Integer genreId,
            String platform,
            String rating) {

        // Clean up the optional filters before building the query.
        String titleFilter = optionalText(search, "Search", 200);
        String platformFilter = optionalText(platform, "Platform", 100);
        String ratingFilter = optionalText(rating, "Rating", 30);

        if (genreId != null && genreId <= 0) {
            throw badRequest("A valid genre ID is required.");
        }

        // Look up the selected genre so we can check game membership.
        Genre genreFilter = genreId == null
                ? null
                : genreRepository.findById(genreId).orElse(null);

        // A genre that does not exist cannot match any games.
        if (genreId != null && genreFilter == null) {
            return List.of();
        }

        // Escape LIKE wildcard characters so searches match literal text.
        String titlePattern = titleFilter == null
                ? null
                : "%" + titleFilter.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";

        // Combine the selected conditions into one database query.
        Specification<Game> specification = (root, query, builder) -> {

            List<Predicate> conditions = new ArrayList<>();

            // Customer catalog searches always include only active games.
            conditions.add(
                    builder.equal(root.get("status"), "ACTIVE")
            );

            if (titlePattern != null) {
                conditions.add(
                        builder.like(
                                builder.lower(root.get("title")),
                                titlePattern,
                                '!'
                        )
                );
            }

            if (genreFilter != null) {
                conditions.add(
                        builder.isMember(
                                genreFilter,
                                root.<Set<Genre>>get("genres")
                        )
                );
            }

            if (platformFilter != null) {
                conditions.add(
                        builder.equal(
                                builder.lower(root.get("platform")),
                                platformFilter.toLowerCase(Locale.ROOT)
                        )
                );
            }

            if (ratingFilter != null) {
                conditions.add(
                        builder.equal(
                                builder.lower(root.get("rating")),
                                ratingFilter.toLowerCase(Locale.ROOT)
                        )
                );
            }

            // Every selected condition must match.
            return builder.and(conditions.toArray(new Predicate[0]));
        };

        return gameRepository.findAll(
                specification,
                Sort.by("title", "gameId")
        );
    }

    @Override
    public Game getGameById(Integer gameId) {

        Game game = findGame(gameId);

        // Inactive games are unavailable through the customer catalog.
        if (!"ACTIVE".equals(game.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Game was not found."
            );
        }

        return game;
    }

    @Override
    public List<Game> getAllGamesForAdmin() {

        // Apply no status restriction to the administrator's game list.
        Specification<Game> allGames =
                (root, query, builder) -> builder.conjunction();

        return gameRepository.findAll(
                allGames,
                Sort.by("title", "gameId")
        );
    }

    @Override
    public Game getGameByIdForAdmin(Integer gameId) {
        return findGame(gameId);
    }

    @Override
    @Transactional
    public Game createGame(Game request) {

        if (request == null) {
            throw badRequest("Game information is required.");
        }

        // Create a fresh record so the request cannot choose an existing ID.
        Game game = new Game();

        game.setTitle(requiredText(request.getTitle(), "Title", 200));
        game.setDescription(validatedDescription(request.getDescription()));
        game.setPlatform(
                requiredText(request.getPlatform(), "Platform", 100)
        );
        game.setRating(requiredText(request.getRating(), "Rating", 30));

        // Connect the game to existing genre records.
        game.setGenres(genresFromRequest(request.getGenres()));

        // New games start as active.
        game.setStatus("ACTIVE");

        return gameRepository.saveAndFlush(game);
    }

    @Override
    @Transactional
    public Game updateGame(
            Integer gameId,
            Map<String, Object> changes) {

        if (changes == null || changes.isEmpty()) {
            throw badRequest("At least one game field must be provided.");
        }

        Game game = findGame(gameId);

        // Apply only the fields supplied in the PATCH request.
        for (Map.Entry<String, Object> change : changes.entrySet()) {

            Object value = change.getValue();

            switch (change.getKey()) {

                case "title" ->
                        game.setTitle(requiredText(value, "Title", 200));

                case "description" ->
                        game.setDescription(validatedDescription(value));

                case "platform" ->
                        game.setPlatform(
                                requiredText(value, "Platform", 100)
                        );

                case "rating" ->
                        game.setRating(requiredText(value, "Rating", 30));

                case "status" ->
                        game.setStatus(validatedStatus(value));

                case "genres" -> {
                    Set<Genre> genres = genresFromJson(value);

                    // Update the existing collection tracked by Hibernate.
                    game.getGenres().clear();
                    game.getGenres().addAll(genres);
                }

                default ->
                        throw badRequest(
                                "Unsupported game field: " + change.getKey()
                        );
            }
        }

        return gameRepository.saveAndFlush(game);
    }

    @Override
    @Transactional
    public Game deactivateGame(Integer gameId) {

        // Reuse the partial-update operation to change the status.
        return updateGame(gameId, Map.of("status", "INACTIVE"));
    }

    // Find a game for internal operations, including inactive records.
    private Game findGame(Integer gameId) {

        if (gameId == null || gameId <= 0) {
            throw badRequest("A valid game ID is required.");
        }

        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Game was not found."
                ));
    }

    // Validate required text and remove spaces at either end.
    private String requiredText(
            Object value,
            String fieldName,
            int maximumLength) {

        if (!(value instanceof String text) || text.isBlank()) {
            throw badRequest(fieldName + " is required.");
        }

        String cleaned = text.trim();

        if (cleaned.length() > maximumLength) {
            throw badRequest(
                    fieldName + " must be "
                            + maximumLength + " characters or fewer."
            );
        }

        return cleaned;
    }

    // Validate the description against the MySQL TEXT column's byte limit.
    private String validatedDescription(Object value) {

        if (!(value instanceof String text) || text.isBlank()) {
            throw badRequest("Description is required.");
        }

        String description = text.trim();

        if (description.getBytes(StandardCharsets.UTF_8).length > 65_535) {
            throw badRequest("Description is too long.");
        }

        return description;
    }

    // Treat a missing or blank search filter as an unselected filter.
    private String optionalText(
            String value,
            String fieldName,
            int maximumLength) {

        return value == null || value.isBlank()
                ? null
                : requiredText(value, fieldName, maximumLength);
    }

    // Collect genre IDs from the Game received during creation.
    private Set<Genre> genresFromRequest(Set<Genre> requestedGenres) {

        if (requestedGenres == null) {
            throw badRequest("At least one genre is required.");
        }

        Set<Integer> genreIds = new HashSet<>();

        for (Genre genre : requestedGenres) {

            if (genre == null
                    || genre.getGenreId() == null
                    || genre.getGenreId() <= 0) {

                throw badRequest(
                        "Each selected genre must have a valid genre ID."
                );
            }

            genreIds.add(genre.getGenreId());
        }

        return loadGenres(genreIds);
    }

    // Read the genre ID objects supplied in a PATCH request.
    private Set<Genre> genresFromJson(Object value) {

        if (!(value instanceof List<?> entries)) {
            throw badRequest("Genres must be supplied as an array.");
        }

        Set<Integer> genreIds = new HashSet<>();

        for (Object entry : entries) {

            if (!(entry instanceof Map<?, ?> genre)
                    || !(genre.get("genreId") instanceof Integer id)
                    || id <= 0) {

                throw badRequest(
                        "Each selected genre must have a valid integer genre ID."
                );
            }

            genreIds.add(id);
        }

        return loadGenres(genreIds);
    }

    // Retrieve the actual saved genres instead of trusting submitted names.
    private Set<Genre> loadGenres(Set<Integer> genreIds) {

        if (genreIds.isEmpty()) {
            throw badRequest("At least one genre is required.");
        }

        List<Genre> genres = genreRepository.findAllById(genreIds);

        if (genres.size() != genreIds.size()) {
            throw badRequest(
                    "One or more selected genres were not found."
            );
        }

        return new HashSet<>(genres);
    }

    // Restrict game status to the values used by our catalog.
    private String validatedStatus(Object value) {

        String status = requiredText(value, "Status", 30)
                .toUpperCase(Locale.ROOT);

        if (!Set.of("ACTIVE", "INACTIVE").contains(status)) {
            throw badRequest("Status must be ACTIVE or INACTIVE.");
        }

        return status;
    }

    // Keep validation errors consistent throughout this service.
    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}