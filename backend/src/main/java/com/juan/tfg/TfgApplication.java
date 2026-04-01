package com.juan.tfg;

import com.juan.tfg.model.dto.LichessPuzzleResponse;
import com.juan.tfg.service.gemini.GeminiService;
import com.juan.tfg.service.lichess.LichessService;
import com.juan.tfg.util.ConsoleBoardPrinter;
import com.juan.tfg.util.PgnHelper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;


@SpringBootApplication
public class TfgApplication {

	public static void main(String[] args) {
		SpringApplication.run(TfgApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(LichessService lichessService, GeminiService geminiService) {
		return (args) -> {
			// Llamamos a tu servicio
			LichessPuzzleResponse response = lichessService.probarConexion();
			String pgnRaw = response.getGame().getPgn();
			int ply = response.getPuzzle().getInitialPly(); // En tu ejemplo es 60
			List<String> solution = response.getPuzzle().getSolution();
			List<String> themes = response.getPuzzle().getThemes();

			System.out.println("⏳ Reconstruyendo partida hasta el movimiento: " + ply);

			// 1. Obtenemos el objeto Board en el estado exacto del puzzle
			String board = PgnHelper.getFenFromPgn(pgnRaw);

			// 2. Usamos tu método anterior para pintarlo
			// Nota: board.getFen() nos devuelve el String FEN que necesita tu printer antiguo
			ConsoleBoardPrinter.printBoard(board);
			System.out.println("This is a piece of advice for the previous problem");
			geminiService.probarConexion(board, solution, themes);
		};
	}

}
