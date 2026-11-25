package com.zxkstudio.sirenia.util;

/**
 * Reloj de un fruto cosechado: instante real (ms) en que "nació" su injerto.
 *
 * Truco para apilar por fase: dos relojes se consideran IGUALES si, en este momento, ambos frutos
 * están en la misma fase (maduro / marchito / podrido). Como Minecraft apila dos objetos solo si
 * sus componentes son iguales, frutos de la misma fase se apilan aunque tengan minutos distintos
 * de vida, y frutos de fases distintas nunca se mezclan.
 *
 * El stack conserva el reloj del stack al que se une el resto (ver README: "Apilado de frutos").
 * El hashCode es constante a propósito: así es coherente con equals aunque la fase cambie con el tiempo.
 */
public record FruitClock(long birthMs) {
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FruitClock that)) {
			return false;
		}
		long now = FruitAging.nowMs();
		return FruitAging.stageForAgeMs(now - birthMs) == FruitAging.stageForAgeMs(now - that.birthMs);
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
