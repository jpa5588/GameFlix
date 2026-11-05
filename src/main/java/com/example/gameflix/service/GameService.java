package com.example.gameflix.service;

import com.example.gameflix.model.Game;

import java.util.List;
import java.util.Map;

// Define the operations available for managing the game catalog.
public interface GameService {

    // Search active games using any combination of these filters.
    List<Game> searchGames(
            String search,
            Integer genreId,
            String platform,
            String rating
    );

    // Return an active game for the customer catalog.
    Game getGameById(Integer gameId);

    // Return active and inactive games for administrator management.
    List<Game> getAllGamesForAdmin();

    // Allow administrators to retrieve an inactive game for editing.
    Game getGameByIdForAdmin(Integer gameId);

    // Validate and create a new game.
    Game createGame(Game request);

    // Update only the fields included in a PATCH request.
    Game updateGame(Integer gameId, Map<String, Object> changes);

    // Remove a game from the active catalog while retaining its record.
    Game deactivateGame(Integer gameId);
}