package com.juan.tfg.service;

import org.springframework.stereotype.Service;

@Service
public class EloService {

    // Factor K estándar del ajedrez para la volatilidad de los puntos
    private static final int K_FACTOR = 32;

    // Penalizaciones
    private static final double PENALTY_PER_HINT = 0.25;
    private static final double PENALTY_PER_ERROR = 0.10;

    /**
     * Calcula el nuevo ELO del jugador tras terminar un puzle.
     */
    public int calculateNewPlayerElo(int playerElo, int puzzleElo, int hintsUsed, int failedAttempts) {

        // 1. Calcular probabilidad esperada (E)
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (puzzleElo - playerElo) / 400.0));

        // 2. Calcular rendimiento real (S)
        double hintPenalty = hintsUsed * PENALTY_PER_HINT;
        double errorPenalty = failedAttempts * PENALTY_PER_ERROR;
        double actualScore = Math.max(0.0, 1.0 - hintPenalty - errorPenalty);

        // 3. Aplicar fórmula ELO
        double eloChange = K_FACTOR * (actualScore - expectedScore);

        // Devolver el nuevo ELO redondeado
        return (int) Math.round(playerElo + eloChange);
    }
}
