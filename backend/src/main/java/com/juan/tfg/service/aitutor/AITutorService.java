package com.juan.tfg.service.aitutor;

import java.util.List;

public interface AITutorService {
    String[] getHints(String fen, List<String> solution, List<String> themes);
}
