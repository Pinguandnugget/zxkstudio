package com.zxkstudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ajustes de Sangre Jurásica. Se guardan en config/zxkstudio_sangre_jurasica.json
 * y se cambian en el juego con /sangrejurasica (alias /sj).
 */
public final class SangreConfig {
	/** Interruptor general: si es false, no hay infecciones ni síntomas. */
	public boolean activo = true;
	/** Segundos de la fase 1 (cuenta atrás hasta la fase terminal). */
	public int tiempoSegundos = 300;
	/** Daño (en medios corazones) de cada golpe de la fase terminal. 2.0 = 1 corazón. */
	public float danoTerminal = 2.0f;
	/** Cada cuántos segundos se aplica el daño de la fase terminal. */
	public int intervaloDanoSegundos = 1;
	/** Nivel del efecto Veneno en fase terminal. 0 = sin veneno. */
	public int nivelVeneno = 0;
	/** Nivel de Debilidad (0 = apagada). En fase terminal sube un nivel. */
	public int nivelDebilidad = 1;
	/** Nivel de Lentitud (0 = apagada). En fase terminal sube un nivel. */
	public int nivelLentitud = 1;
	public boolean nausea = true;
	public boolean oscuridad = true;
	public boolean latido = true;
	public boolean particulas = true;
	public boolean mensajes = true;
	/** Los dinosaurios hostiles cercanos se lanzan contra el jugador. */
	public boolean provocarDinos = true;
	public int radioProvocar = 20;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static SangreConfig instancia = new SangreConfig();

	public static SangreConfig get() {
		return instancia;
	}

	private static Path ruta() {
		return FabricLoader.getInstance().getConfigDir().resolve("zxkstudio_sangre_jurasica.json");
	}

	public static void cargar() {
		Path p = ruta();
		if (Files.exists(p)) {
			try (Reader lector = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				SangreConfig leida = GSON.fromJson(lector, SangreConfig.class);
				if (leida != null) {
					instancia = leida;
				}
			} catch (Exception e) {
				ZxkStudio.LOGGER.error("No se pudo leer {}; se usan los valores por defecto", p, e);
				instancia = new SangreConfig();
			}
		}
		instancia.corregir();
		guardar();
	}

	public static void guardar() {
		Path p = ruta();
		try {
			Files.createDirectories(p.getParent());
			try (Writer escritor = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
				GSON.toJson(instancia, escritor);
			}
		} catch (IOException e) {
			ZxkStudio.LOGGER.error("No se pudo guardar {}", p, e);
		}
	}

	public static void restablecer() {
		instancia = new SangreConfig();
		guardar();
	}

	private void corregir() {
		tiempoSegundos = limitar(tiempoSegundos, 10, 86400);
		danoTerminal = Math.max(0.0f, Math.min(1000.0f, danoTerminal));
		intervaloDanoSegundos = limitar(intervaloDanoSegundos, 1, 3600);
		nivelVeneno = limitar(nivelVeneno, 0, 10);
		nivelDebilidad = limitar(nivelDebilidad, 0, 10);
		nivelLentitud = limitar(nivelLentitud, 0, 10);
		radioProvocar = limitar(radioProvocar, 1, 128);
	}

	private static int limitar(int valor, int min, int max) {
		return Math.max(min, Math.min(max, valor));
	}
}
