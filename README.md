# ♟️ AI Chess Trainer - TFG

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Gemini API](https://img.shields.io/badge/Google_Gemini-API-8E75B2?style=for-the-badge&logo=google-bard&logoColor=white)
![Lichess](https://img.shields.io/badge/Lichess-Data-white?style=for-the-badge&logo=lichess&logoColor=black)

> **Entrenador personal de táctica de ajedrez potenciado por Inteligencia Artificial Generativa.**

## 📖 Descripción del Proyecto

Este proyecto es un Trabajo de Fin de Grado (TFG) que implementa una plataforma web para el entrenamiento de ajedrez. El sistema combina la inmensa base de datos de problemas tácticos de **Lichess** con la capacidad de razonamiento de un LLM (**Google Gemini**).

El objetivo principal es simular la experiencia de tener un entrenador humano: en lugar de dar la solución cuando el usuario falla, la IA analiza la posición y ofrece pistas conceptuales adaptadas al nivel del alumno.

### ✨ Características Principales

- **🧩 Puzzles Reales:** Obtención dinámica de ejercicios tácticos desde la API de Lichess.
- **🤖 Tutor IA Interactivo:** Integración con Gemini para explicar _por qué_ una jugada es buena o mala sin hacer spoilers directos.
- **📈 Adaptabilidad:** Selección de problemas basada en el ELO (nivel) del usuario.
- **💡 Sistema de Pistas:** El usuario puede solicitar ayuda textual ("¿Qué pieza debo mover?", "¿Hay algún mate cerca?").

---

## 🛠️ Stack Tecnológico

| Componente   | Tecnología         | Uso                                                |
| :----------- | :----------------- |:---------------------------------------------------|
| **Backend**  | Java Spring Boot   | API REST, Lógica de negocio.                       |
| **IA**       | Google Gemini API  | Generación de explicaciones y análisis de FEN/PGN. |
| **Datos**    | Lichess API        | Fuente de los problemas tácticos (Puzzles).        |
| **Frontend** | React - TypeScript | Interfaz de usuario y tablero de ajedrez.          |

---

## 🎓 Información Académica

### Trabajo de Fin de Grado (TFG) Grado en Ingeniería Informática del Software

- **Título**: Desarrollo de un Entrenador de Ajedrez Inteligente basado en LLMs.
- **Alumno**: Juan López Álvarez
- **Tutor**: Pablo González González
- **Universidad**: Universidad de Oviedo
- **Fecha**: 2026
