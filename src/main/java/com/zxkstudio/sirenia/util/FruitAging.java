package com.zxkstudio.sirenia.util;

import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModComponents;
import com.zxkstudio.sirenia.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Caducidad de los frutos ya cosechados.
 *
 * Cada fruto guarda en el componente zxkstudio:fruit_clock el instante (milisegundos reales) en que
 * "nació" el injerto. El estado se calcula siempre a partir de ese dato, así que no depende de dónde esté
 * el objeto (inventario, cofre, barril, shulker, mochila...) ni se reinicia al dividir stacks o moverlo.
 * Los frutos de la misma fase se apilan entre sí (ver FruitClock); los de fases distintas no.
 */
public final class FruitAging {
	public static final int MATURE = 2;
	public static final int WILTED = 3;
	public static final int ROTTEN = 4;
	private static final long MINUTE_MS = 60_000L;

	private FruitAging() {
	}

	public static long nowMs() {
		return System.currentTimeMillis();
	}

	/** Redondea al minuto para que frutos cosechados a la vez sigan apilándose. */
	public static long quantize(long ms) {
		return (ms / MINUTE_MS) * MINUTE_MS;
	}

	/** Estado (2 maduro, 3 marchito, 4 podrido) según los minutos de vida del fruto. */
	public static int stageForAgeMs(long ageMs) {
		SireniaConfig c = SireniaConfig.get();
		long ageTicks = ageMs / 50L;
		if (ageTicks >= c.rotTicks()) {
			return ROTTEN;
		}
		if (ageTicks >= c.wiltTicks()) {
			return WILTED;
		}
		return MATURE;
	}

	/** Estado mínimo que corresponde al ID del objeto. */
	public static int stageOfItem(Item item) {
		if (item == ModItems.ROTTEN_SIRENIA_FRUIT) {
			return ROTTEN;
		}
		if (item == ModItems.WILTED_SIRENIA_FRUIT) {
			return WILTED;
		}
		return MATURE;
	}

	public static Item itemForStage(int stage) {
		return switch (stage) {
			case ROTTEN -> ModItems.ROTTEN_SIRENIA_FRUIT;
			case WILTED -> ModItems.WILTED_SIRENIA_FRUIT;
			default -> ModItems.SIRENIA_FRUIT;
		};
	}

	/** Instante de nacimiento; si el objeto no lo tiene (creativo, /give) se deduce del estado del ID. */
	public static long birthMs(ItemStack stack, long now) {
		FruitClock clock = stack.get(ModComponents.FRUIT_CLOCK);
		if (clock != null) {
			return clock.birthMs();
		}
		Long legacy = stack.get(ModComponents.BIRTH_TIME); // frutos de la primera versión
		if (legacy != null) {
			return legacy;
		}
		SireniaConfig c = SireniaConfig.get();
		long ticks = switch (stageOfItem(stack.getItem())) {
			case ROTTEN -> c.rotTicks();
			case WILTED -> c.wiltTicks();
			default -> c.matureTicks();
		};
		return quantize(now - ticks * 50L);
	}

	/** Estado real del fruto en este momento. Nunca "rejuvenece" respecto a su ID. */
	public static int effectiveStage(ItemStack stack, long now) {
		long age = Math.max(0L, now - birthMs(stack, now));
		return Math.max(stageOfItem(stack.getItem()), stageForAgeMs(age));
	}

	/** Minutos que faltan para el siguiente estado, o -1 si ya está podrido. */
	public static long minutesToNextStage(ItemStack stack, long now) {
		SireniaConfig c = SireniaConfig.get();
		long ageTicks = Math.max(0L, now - birthMs(stack, now)) / 50L;
		long target;
		if (ageTicks < c.wiltTicks()) {
			target = c.wiltTicks();
		} else if (ageTicks < c.rotTicks()) {
			target = c.rotTicks();
		} else {
			return -1L;
		}
		return Math.max(1L, (target - ageTicks) / 1200L);
	}

	/** Da al objeto su reloj si todavía no lo tiene (o migra el componente antiguo). */
	public static void ensureBirth(ItemStack stack, long now) {
		if (!stack.contains(ModComponents.FRUIT_CLOCK)) {
			stack.set(ModComponents.FRUIT_CLOCK, new FruitClock(birthMs(stack, now)));
			stack.remove(ModComponents.BIRTH_TIME);
		}
	}

	/** Copia el reloj de un fruto a otro (al cambiar de ID por la fase). */
	public static void copyClock(ItemStack from, ItemStack to, long now) {
		to.set(ModComponents.FRUIT_CLOCK, new FruitClock(birthMs(from, now)));
	}

	/** Crea el fruto cosechado de un injerto con ese tiempo de crecimiento (en ticks cargados). */
	public static ItemStack harvested(int stage, long growthTicks) {
		ItemStack stack = new ItemStack(itemForStage(stage));
		stack.set(ModComponents.FRUIT_CLOCK, new FruitClock(nowMs() - growthTicks * 50L));
		return stack;
	}
}
