package com.example.gameflix.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

// Store each game as a record in the Games table.
@Entity
@Table(name = "Games")
public class Game {

    // Let MySQL generate the primary key for each game.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Integer gameId;

    // Store the game's title, with a maximum of 200 characters.
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    // Use a TEXT column so descriptions can contain more information.
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    // Store the platform used when filtering the catalog.
    @Column(name = "platform", nullable = false, length = 100)
    private String platform;

    // Store the game's content rating for display and filtering.
    @Column(name = "rating", nullable = false, length = 30)
    private String rating;

    // New games start as active.
    // We can later deactivate a game while keeping its database record.
    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    // A game can have multiple genres, and each genre can belong to many games.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "Game_Genres",

            // Connect each join-table row to a game.
            joinColumns = @JoinColumn(
                    name = "game_id",
                    nullable = false
            ),

            // Connect that same row to a genre.
            inverseJoinColumns = @JoinColumn(
                    name = "genre_id",
                    nullable = false
            )
    )
    // Start new games with an empty collection of genres.
    private Set<Genre> genres = new HashSet<>();

    // Return the genres connected to this game.
    public Set<Genre> getGenres() {
        return genres;
    }

    // Replace the game's genres with the supplied collection.
    public void setGenres(Set<Genre> genres) {

        // Keep an empty collection if no genres were provided.
        this.genres = genres == null ? new HashSet<>() : genres;
    }
    // Default constructor required by JPA.
    public Game() {
    }

    // Getters and setters for the game's information.

    public Integer getGameId() {
        return gameId;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}