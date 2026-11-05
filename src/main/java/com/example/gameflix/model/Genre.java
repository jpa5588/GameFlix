package com.example.gameflix.model;

import jakarta.persistence.*;

// Store the genres available in the game catalog.
@Entity
@Table(name = "Genres", uniqueConstraints = @UniqueConstraint(name = "uk_genres_genre_name", columnNames = "genre_name"))
public class Genre {

    // Let MySQL generate the primary key for each genre.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Integer genreId;

    // Each genre must have a unique name, such as RPG or Adventure.
    @Column(name = "genre_name", nullable = false, length = 100)
    private String genreName;

    // Default constructor required by JPA.
    public Genre() {
    }

    // Getters and setters for the genre information.

    public Integer getGenreId() {
        return genreId;
    }

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }
}