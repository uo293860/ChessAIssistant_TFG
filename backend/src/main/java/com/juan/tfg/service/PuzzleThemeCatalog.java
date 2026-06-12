package com.juan.tfg.service;

import com.juan.tfg.model.dto.PuzzleThemeDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PuzzleThemeCatalog {

    private static final List<PuzzleThemeDTO> THEMES = List.of(
            new PuzzleThemeDTO("advancedPawn", "Advanced pawn"),
            new PuzzleThemeDTO("advantage", "Advantage"),
            new PuzzleThemeDTO("anastasiaMate", "Anastasia's mate"),
            new PuzzleThemeDTO("arabianMate", "Arabian mate"),
            new PuzzleThemeDTO("attackingF2F7", "Attacking f2 or f7"),
            new PuzzleThemeDTO("attraction", "Attraction"),
            new PuzzleThemeDTO("backRankMate", "Back rank mate"),
            new PuzzleThemeDTO("balestraMate", "Balestra mate"),
            new PuzzleThemeDTO("bishopEndgame", "Bishop endgame"),
            new PuzzleThemeDTO("blindSwineMate", "Blind Swine mate"),
            new PuzzleThemeDTO("bodenMate", "Boden's mate"),
            new PuzzleThemeDTO("capturingDefender", "Capture the defender"),
            new PuzzleThemeDTO("castling", "Castling"),
            new PuzzleThemeDTO("clearance", "Clearance"),
            new PuzzleThemeDTO("collinearMove", "Collinear move"),
            new PuzzleThemeDTO("cornerMate", "Corner mate"),
            new PuzzleThemeDTO("crushing", "Crushing"),
            new PuzzleThemeDTO("defensiveMove", "Defensive move"),
            new PuzzleThemeDTO("deflection", "Deflection"),
            new PuzzleThemeDTO("discoveredAttack", "Discovered attack"),
            new PuzzleThemeDTO("discoveredCheck", "Discovered check"),
            new PuzzleThemeDTO("doubleBishopMate", "Double bishop mate"),
            new PuzzleThemeDTO("doubleCheck", "Double check"),
            new PuzzleThemeDTO("dovetailMate", "Dovetail mate"),
            new PuzzleThemeDTO("endgame", "Endgame"),
            new PuzzleThemeDTO("enPassant", "En passant"),
            new PuzzleThemeDTO("epauletteMate", "Epaulette mate"),
            new PuzzleThemeDTO("equality", "Equality"),
            new PuzzleThemeDTO("exposedKing", "Exposed king"),
            new PuzzleThemeDTO("fork", "Fork"),
            new PuzzleThemeDTO("hangingPiece", "Hanging piece"),
            new PuzzleThemeDTO("hookMate", "Hook mate"),
            new PuzzleThemeDTO("interference", "Interference"),
            new PuzzleThemeDTO("intermezzo", "Intermezzo"),
            new PuzzleThemeDTO("killBoxMate", "Kill box mate"),
            new PuzzleThemeDTO("kingsideAttack", "Kingside attack"),
            new PuzzleThemeDTO("knightEndgame", "Knight endgame"),
            new PuzzleThemeDTO("long", "Long puzzle"),
            new PuzzleThemeDTO("master", "Master games"),
            new PuzzleThemeDTO("masterVsMaster", "Master vs Master games"),
            new PuzzleThemeDTO("mate", "Checkmate"),
            new PuzzleThemeDTO("mateIn1", "Mate in 1"),
            new PuzzleThemeDTO("mateIn2", "Mate in 2"),
            new PuzzleThemeDTO("mateIn3", "Mate in 3"),
            new PuzzleThemeDTO("mateIn4", "Mate in 4"),
            new PuzzleThemeDTO("mateIn5", "Mate in 5 or more"),
            new PuzzleThemeDTO("middlegame", "Middlegame"),
            new PuzzleThemeDTO("morphysMate", "Morphy's mate"),
            new PuzzleThemeDTO("oneMove", "One-move puzzle"),
            new PuzzleThemeDTO("opening", "Opening"),
            new PuzzleThemeDTO("operaMate", "Opera mate"),
            new PuzzleThemeDTO("pawnEndgame", "Pawn endgame"),
            new PuzzleThemeDTO("pillsburysMate", "Pillsbury's mate"),
            new PuzzleThemeDTO("pin", "Pin"),
            new PuzzleThemeDTO("promotion", "Promotion"),
            new PuzzleThemeDTO("queenEndgame", "Queen endgame"),
            new PuzzleThemeDTO("queenRookEndgame", "Queen and Rook"),
            new PuzzleThemeDTO("queensideAttack", "Queenside attack"),
            new PuzzleThemeDTO("quietMove", "Quiet move"),
            new PuzzleThemeDTO("rookEndgame", "Rook endgame"),
            new PuzzleThemeDTO("sacrifice", "Sacrifice"),
            new PuzzleThemeDTO("short", "Short puzzle"),
            new PuzzleThemeDTO("skewer", "Skewer"),
            new PuzzleThemeDTO("smotheredMate", "Smothered mate"),
            new PuzzleThemeDTO("superGM", "Super GM games"),
            new PuzzleThemeDTO("swallowstailMate", "Swallow's tail mate"),
            new PuzzleThemeDTO("trappedPiece", "Trapped piece"),
            new PuzzleThemeDTO("triangleMate", "Triangle mate"),
            new PuzzleThemeDTO("underPromotion", "Underpromotion"),
            new PuzzleThemeDTO("veryLong", "Very long puzzle"),
            new PuzzleThemeDTO("vukovicMate", "Vukovic mate"),
            new PuzzleThemeDTO("xRayAttack", "X-Ray attack"),
            new PuzzleThemeDTO("zugzwang", "Zugzwang")
    );

    private static final Set<String> THEME_IDS = THEMES.stream()
            .map(PuzzleThemeDTO::id)
            .collect(Collectors.toUnmodifiableSet());

    public List<PuzzleThemeDTO> getThemes() {
        return THEMES;
    }

    public Optional<String> resolveSelectedThemeId(String themeId) {
        if (themeId == null || themeId.isBlank()) {
            return Optional.empty();
        }

        String normalizedThemeId = themeId.trim();
        if (!THEME_IDS.contains(normalizedThemeId)) {
            throw new IllegalArgumentException("Unknown puzzle theme: " + normalizedThemeId);
        }

        return Optional.of(normalizedThemeId);
    }
}
