package com.example.gameflix.repository;

import com.example.gameflix.model.Game;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Handle database access for games.
// Specification support lets us combine the selected catalog filters.
@Repository
public interface GameRepository extends
        JpaRepository<Game, Integer>,
        JpaSpecificationExecutor<Game> {

    // Return all games with their genres loaded.
    @Override
    @EntityGraph(attributePaths = "genres")
    List<Game> findAll();

    // Find one game by its ID and include its genres.
    @Override
    @EntityGraph(attributePaths = "genres")
    Optional<Game> findById(Integer gameId);

    // Return games matching the supplied filters and sorting rules.
    @Override
    @EntityGraph(attributePaths = "genres")
    List<Game> findAll(
            Specification<Game> specification,
            Sort sort
    );
}