package com.juan.tfg.model.dto;

import java.time.LocalDateTime;

public record EloHistoryPointDTO(
        Long attemptId,
        LocalDateTime attemptDate,
        Integer puzzleRating,
        Integer eloChange,
        Integer resultingElo
) {
}
