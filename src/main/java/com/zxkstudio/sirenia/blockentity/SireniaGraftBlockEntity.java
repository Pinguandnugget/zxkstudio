package com.zxkstudio.sirenia.blockentity;

import com.zxkstudio.sirenia.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Datos de un injerto: solo dos números. No tickea por sí mismo; el bloque programa un tick
 * programado cada pocos segundos que sí persiste con el chunk y no corre si el chunk está descargado.
 */
public class SireniaGraftBlockEntity extends BlockEntity {
	/** Ticks de tiempo cargado con condiciones válidas acumulados desde el injerto. */
	public long growthTicks;
	/** Polvo de hueso aplicado mientras el fruto crecía. */
	public int boneMeal;

	public SireniaGraftBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GRAFT, pos, state);
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putLong("growth_ticks", growthTicks);
		nbt.putInt("bone_meal", boneMeal);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		growthTicks = nbt.getLong("growth_ticks");
		boneMeal = nbt.getInt("bone_meal");
	}
}
