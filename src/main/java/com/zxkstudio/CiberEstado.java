package com.zxkstudio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Estado de Cyberpsychosis de un jugador (equivale a los scoreboards cp_* del datapack).
 * Se guarda en el jugador, así que sobrevive a reinicios del servidor.
 */
public final class CiberEstado {
	/** Ticks que le quedan antes de la cyberpsychosis (reloj de inmunosupresores). */
	public int t;
	/** 0 = normal, 1 = en cyberpsychosis. */
	public int est;
	/** Bajas conseguidas durante la cyberpsychosis actual. */
	public int kills;
	/** Ticks transcurridos dentro de la cyberpsychosis. */
	public int pt;
	/** Ticks hasta el próximo pop (daño + efectos). */
	public int pop;
	/** Ticks de inmunidad del Bloqueador Neural que le quedan. */
	public int inm;

	public CiberEstado(int t, int est, int kills, int pt, int pop, int inm) {
		this.t = t;
		this.est = est;
		this.kills = kills;
		this.pt = pt;
		this.pop = pop;
		this.inm = inm;
	}

	public static final Codec<CiberEstado> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
			Codec.INT.optionalFieldOf("t", 0).forGetter(e -> e.t),
			Codec.INT.optionalFieldOf("est", 0).forGetter(e -> e.est),
			Codec.INT.optionalFieldOf("kills", 0).forGetter(e -> e.kills),
			Codec.INT.optionalFieldOf("pt", 0).forGetter(e -> e.pt),
			Codec.INT.optionalFieldOf("pop", 0).forGetter(e -> e.pop),
			Codec.INT.optionalFieldOf("inm", 0).forGetter(e -> e.inm)
	).apply(instancia, CiberEstado::new));
}
