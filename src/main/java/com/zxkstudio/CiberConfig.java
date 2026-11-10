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
 * Ajustes de Cyberpsychosis. Se guardan en config/zxkstudio_ciberpsicosis.json
 * y se cambian en el juego con /ciberpsicosis (alias /cp).
 */
public final class CiberConfig {
	/**
	 * false = el Inmunosupresor y el Bloqueador curan siempre (modo normal).
	 * true = quien está en cyberpsychosis NO puede curarse con ellos: solo matando
	 * a las entidades que pide (las "bajas"). Al volver a false (fin del evento)
	 * los inmunosupresores curan con normalidad otra vez.
	 */
	public boolean modoSoloBajas = false;
	/** Segundos entre un inmunosupresor y la cyberpsychosis (900 = 15 minutos). */
	public int tiempoSegundos = 900;
	/** Bajas (entidades muertas) necesarias para estabilizarse. */
	public int bajasNecesarias = 6;
	/** Daño de cada "pop" en cyberpsychosis (2.0 = 1 corazón). */
	public float danoPop = 4.0f;
	/** El pop llega cada entre popMinSegundos y popMaxSegundos. */
	public int popMinSegundos = 9;
	public int popMaxSegundos = 12;
	/** Duración de la inmunidad del Bloqueador Neural. */
	public int bloqueadorSegundos = 1800;
	public boolean overlays = true;
	public boolean figuras = true;
	public boolean chatFalso = true;
	public boolean sacudidas = true;
	/** Alucinaciones de playa y nieve durante la cyberpsychosis. */
	public boolean alucinaciones = true;
	/** Duración de cada alucinación antes del susto final. */
	public int alucinacionSegundos = 30;
	/** Espera entre alucinaciones (al azar entre min y max), contada dentro de la cyberpsychosis. */
	public int alucinacionEsperaMin = 45;
	public int alucinacionEsperaMax = 120;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static CiberConfig instancia = new CiberConfig();

	public static CiberConfig get() {
		return instancia;
	}

	public int tiempoTicks() {
		return tiempoSegundos * 20;
	}

	private static Path ruta() {
		return FabricLoader.getInstance().getConfigDir().resolve("zxkstudio_ciberpsicosis.json");
	}

	public static void cargar() {
		Path p = ruta();
		if (Files.exists(p)) {
			try (Reader lector = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				CiberConfig leida = GSON.fromJson(lector, CiberConfig.class);
				if (leida != null) {
					instancia = leida;
				}
			} catch (Exception e) {
				ZxkStudio.LOGGER.error("No se pudo leer {}; se usan los valores por defecto", p, e);
				instancia = new CiberConfig();
			}
		}
		guardar();
	}

	public static void guardar() {
		instancia.corregir();
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
		instancia = new CiberConfig();
		guardar();
	}

	private void corregir() {
		tiempoSegundos = limitar(tiempoSegundos, 30, 86400);
		bajasNecesarias = limitar(bajasNecesarias, 1, 100);
		danoPop = Math.max(0.0f, Math.min(1000.0f, danoPop));
		popMinSegundos = limitar(popMinSegundos, 1, 600);
		popMaxSegundos = limitar(popMaxSegundos, popMinSegundos, 600);
		bloqueadorSegundos = limitar(bloqueadorSegundos, 10, 86400);
		alucinacionSegundos = limitar(alucinacionSegundos, 10, 300);
		alucinacionEsperaMin = limitar(alucinacionEsperaMin, 5, 3600);
		alucinacionEsperaMax = limitar(alucinacionEsperaMax, alucinacionEsperaMin, 3600);
	}

	private static int limitar(int valor, int min, int max) {
		return Math.max(min, Math.min(max, valor));
	}
}
