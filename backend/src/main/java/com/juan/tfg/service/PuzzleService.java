package com.juan.tfg.service;

import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.repository.PuzzleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PuzzleService {

    @Autowired
    private PuzzleRepository puzzleRepository;

    public Optional<PuzzleDTO> getRandomPuzzle(String theme, int minRating, int maxRating) {
        return puzzleRepository.findRandomPuzzleByThemeAndRating(theme, minRating, maxRating)
                .map(PuzzleDTO::from);
    }
}
