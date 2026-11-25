package com.zxkstudio.sirenia.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zxkstudio.ZxkStudio;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ajustes de PotitoSirenia. Se guardan en config/zxkstudio_sirenia.json.
 * Los nombres de los campos son los mismos que aparecen en el JSON.
 * Gson ya viene incluido en Minecraft, no hace falta ninguna dependencia extra.
 */
@SuppressWarnings("java:S116")
public final class SireniaConfig {
	/** Versión del archivo (0 = archivo anterior a este campo). Si es menor que 2 se migran los valores que cambiaron de diseño. */
	public int config_version = 0;

	// ---- Crecimiento del fruto (minutos de tiempo cargado desde el injerto) ----
	public int fruit_small_minutes = 10;
	public int fruit_growth_minutes = 25;
	public int fruit_wilt_minutes = 55;
	public int fruit_rot_minutes = 70;

	// ---- Cultivo ----
	public int bone_meal_required = 5;
	public int required_water_blocks = 4;
	public int water_detection_radius = 3;
	/** Probabilidad (0 a 1) de que un fruto maduro cosechado dé un Germen extra. */
	public double seed_bonus_chance = 0.25;
	/** Semillas del árbol que da un fruto al partirlo con hacha (mínimo y máximo, ambos incluidos). */
	public int tree_seed_min = 2;
	public int tree_seed_max = 3;
	/** El brote crece de fase con probabilidad 1/N en cada tick aleatorio (si el sitio es válido). */
	public int sapling_growth_chance = 6;

	// ---- Efectos de los frutos ----
	public int sirenomorphosis_duration_seconds = 180;
	public int wilted_effect_duration_seconds = 60;
	/** Podredumbre de Sirenia (fruto podrido). 0 = dura hasta curarse con un Corazón de Sirenia en buen estado. */
	public int rot_effect_duration_seconds = 0;
	public float rot_damage = 1.0f;
	public int rot_damage_interval_ticks = 25;

	// ---- Sirenomorphosis ----
	/** Fuera del agua el jugador se ahoga: pierde aire y, al agotarse, recibe daño de ahogamiento. */
	public boolean land_penalty_enabled = true;
	/** Aire que se pierde por tick fuera del agua. El juego devuelve 4 por tick por su cuenta, así que 9 = 5 netos. */
	public int land_air_loss_per_tick = 9;
	public int land_grace_ticks = 20;
	public float land_damage = 2.0f;
	public int land_damage_interval_ticks = 20;
	/** Si es true, nadar con la cabeza fuera del agua (superficie) también cuenta como estar en el agua. */
	public boolean land_breathe_when_touching_water = true;
	public boolean underwater_mining_enabled = true;
	public boolean underwater_speed_enabled = true;
	/** 0 a 1. Eficiencia de movimiento en el agua (atributo de vanilla, como Agilidad Acuática). 1 = máximo. */
	public double underwater_swim_efficiency = 1.0;
	/** Solo lo lee el cliente (la niebla se dibuja en el cliente). */
	public boolean underwater_vision_enabled = true;
	public int underwater_vision_distance = 300;
	/** Niebla corta y rosada fuera del agua, como en el efecto original de Dedsafio 3. Solo la lee el cliente. */
	public boolean land_fog_enabled = true;
	public int land_fog_distance = 20;
	public boolean underwater_particles_enabled = true;
	public int underwater_particle_interval_ticks = 5;

	// ---- Talasofobia (miedo al agua profunda). Apagada por defecto; se activa con /talasofobia on ----
	public boolean thalassophobia_enabled = false;
	/** Bloques de agua seguidos por debajo del jugador (con la cabeza bajo el agua) para contar como "abismo". */
	public int thalassophobia_depth_blocks = 12;
	/** Segundos seguidos en el abismo antes de que aparezca el miedo. */
	public int thalassophobia_expose_seconds = 10;
	/** Segundos que tiene para reaccionar (salir del agua o comer un Corazón de Sirenia). */
	public int thalassophobia_countdown_seconds = 45;
	/** En los últimos segundos de la cuenta atrás empiezan las alucinaciones. */
	public int thalassophobia_hallucination_seconds = 10;
	/** Al llegar a 0 empieza el pánico: cada tantos segundos recibe un golpe letal que gasta un tótem (o lo mata). */
	public int thalassophobia_panic_interval_seconds = 4;
	public boolean thalassophobia_immune_with_sirenomorphosis = true;
	/** Volumen de los sonidos de las alucinaciones (1 = normal). */
	public float thalassophobia_sound_volume = 2.0f;

	// ---- Generación natural (probabilidad por intento; hay 1 intento por chunk en superficie) ----
	public boolean generation_enabled = true;
	public double mangrove_chance = 0.12;
	public double coast_chance = 0.03;
	public double lush_cave_chance = 0.05;
	public boolean require_water_for_generation = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static SireniaConfig instance = new SireniaConfig();

	public static SireniaConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("zxkstudio_sirenia.json");
	}

	public static void load() {
		Path p = path();
		if (Files.exists(p)) {
			try (Reader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				SireniaConfig read = GSON.fromJson(reader, SireniaConfig.class);
				if (read != null) {
					instance = read;
				}
			} catch (Exception e) {
				ZxkStudio.LOGGER.error("No se pudo leer {}; se usan los valores por defecto", p, e);
				instance = new SireniaConfig();
			}
		}
		instance.migrate();
		instance.sanitize();
		save();
	}

	/** Archivos creados con la versión anterior (sin config_version) pasan a los nuevos valores por defecto. */
	private void migrate() {
		if (config_version < 2) {
			sirenomorphosis_duration_seconds = 180;
			land_penalty_enabled = true;
			land_air_loss_per_tick = 9;
			land_grace_ticks = 20;
			land_damage = 2.0f;
			land_damage_interval_ticks = 20;
			underwater_particle_interval_ticks = 5;
			config_version = 2;
		}
	}

	public static void save() {
		Path p = path();
		try {
			Files.createDirectories(p.getParent());
			try (Writer writer = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ZxkStudio.LOGGER.error("No se pudo guardar {}", p, e);
		}
	}

	private void sanitize() {
		thalassophobia_depth_blocks = clamp(thalassophobia_depth_blocks, 3, 64);
		thalassophobia_expose_seconds = clamp(thalassophobia_expose_seconds, 1, 600);
		thalassophobia_countdown_seconds = clamp(thalassophobia_countdown_seconds, 5, 3600);
		thalassophobia_hallucination_seconds = clamp(thalassophobia_hallucination_seconds, 1, thalassophobia_countdown_seconds);
		thalassophobia_panic_interval_seconds = clamp(thalassophobia_panic_interval_seconds, 1, 600);
		thalassophobia_sound_volume = Math.max(0.1f, Math.min(4.0f, thalassophobia_sound_volume));
		fruit_small_minutes = clamp(fruit_small_minutes, 0, 100000);
		fruit_growth_minutes = clamp(fruit_growth_minutes, fruit_small_minutes + 1, 100000);
		fruit_wilt_minutes = clamp(fruit_wilt_minutes, fruit_growth_minutes + 1, 100000);
		fruit_rot_minutes = clamp(fruit_rot_minutes, fruit_wilt_minutes + 1, 100000);
		bone_meal_required = clamp(bone_meal_required, 1, 64);
		required_water_blocks = clamp(required_water_blocks, 0, 200);
		tree_seed_min = clamp(tree_seed_min, 1, 16);
		tree_seed_max = clamp(tree_seed_max, tree_seed_min, 16);
		sapling_growth_chance = clamp(sapling_growth_chance, 1, 1000);
		water_detection_radius = clamp(water_detection_radius, 1, 6);
		seed_bonus_chance = Math.max(0.0, Math.min(1.0, seed_bonus_chance));
		sirenomorphosis_duration_seconds = clamp(sirenomorphosis_duration_seconds, 1, 86400);
		wilted_effect_duration_seconds = clamp(wilted_effect_duration_seconds, 1, 86400);
		rot_effect_duration_seconds = clamp(rot_effect_duration_seconds, 0, 86400);
		rot_damage = Math.max(0.0f, Math.min(1000.0f, rot_damage));
		rot_damage_interval_ticks = clamp(rot_damage_interval_ticks, 1, 6000);
		land_fog_distance = clamp(land_fog_distance, 4, 1000);
		land_air_loss_per_tick = clamp(land_air_loss_per_tick, 1, 300);
		land_grace_ticks = clamp(land_grace_ticks, 0, 6000);
		land_damage = Math.max(0.0f, Math.min(1000.0f, land_damage));
		land_damage_interval_ticks = clamp(land_damage_interval_ticks, 1, 6000);
		underwater_swim_efficiency = Math.max(0.0, Math.min(1.0, underwater_swim_efficiency));
		underwater_vision_distance = clamp(underwater_vision_distance, 8, 1000);
		underwater_particle_interval_ticks = clamp(underwater_particle_interval_ticks, 2, 200);
		mangrove_chance = Math.max(0.0, Math.min(1.0, mangrove_chance));
		coast_chance = Math.max(0.0, Math.min(1.0, coast_chance));
		lush_cave_chance = Math.max(0.0, Math.min(1.0, lush_cave_chance));
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	// ---- Valores derivados ----

	public long smallTicks() {
		return fruit_small_minutes * 1200L;
	}

	public long matureTicks() {
		return fruit_growth_minutes * 1200L;
	}

	public long wiltTicks() {
		return fruit_wilt_minutes * 1200L;
	}

	public long rotTicks() {
		return fruit_rot_minutes * 1200L;
	}

	/** Polvo de hueso necesario para pasar de injerto a fruto pequeño (2 de 5 por defecto). */
	public int boneMealForSmall() {
		return Math.max(1, Math.min(bone_meal_required, Math.round(bone_meal_required * 0.4f)));
	}
}
