package com.juan.tfg.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puzzles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Puzzle {

    @Id
    @Column(length = 20)
    private String id; // El ID que viene de Lichess

    @Column(nullable = false, length = 100)
    private String fen;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String moves;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String themes;

    @Column(name = "game_url")
    private String gameUrl;

    public String getInitialMove(){
        String[] movesArray = getMoves().split(" ");
        return movesArray.length > 0 ? movesArray[0] : "";
    }
}
