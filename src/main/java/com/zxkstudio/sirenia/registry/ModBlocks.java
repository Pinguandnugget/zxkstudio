package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.block.SireniaGraftBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
	private ModBlocks() {
	}

	public static Block SIRENIA_LOG;
	public static Block STRIPPED_SIRENIA_LOG;
	public static Block SIRENIA_WOOD;
	public static Block STRIPPED_SIRENIA_WOOD;
	public static Block SIRENIA_PLANKS;
	public static Block SIRENIA_LEAVES;
	/** Injerto/fruto que crece pegado al costado de un tronco pelado (no tiene ítem). */
	public static Block SIRENIA_GRAFT;

	public static void register() {
		SIRENIA_LOG = add("sirenia_log", new PillarBlock(logSettings(MapColor.BLUE)));
		STRIPPED_SIRENIA_LOG = add("stripped_sirenia_log", new PillarBlock(logSettings(MapColor.TEAL)));
		SIRENIA_WOOD = add("sirenia_wood", new PillarBlock(logSettings(MapColor.BLUE)));
		STRIPPED_SIRENIA_WOOD = add("stripped_sirenia_wood", new PillarBlock(logSettings(MapColor.TEAL)));
		SIRENIA_PLANKS = add("sirenia_planks", new Block(AbstractBlock.Settings.create()
				.mapColor(MapColor.CYAN)
				.instrument(NoteBlockInstrument.BASS)
				.strength(2.0f, 3.0f)
				.sounds(BlockSoundGroup.WOOD)));
		SIRENIA_LEAVES = add("sirenia_leaves", new LeavesBlock(AbstractBlock.Settings.create()
				.mapColor(MapColor.EMERALD_GREEN)
				.strength(0.2f)
				.ticksRandomly()
				.sounds(BlockSoundGroup.GRASS)
				.nonOpaque()
				.allowsSpawning((state, world, pos, type) -> type == EntityType.OCELOT || type == EntityType.PARROT)
				.suffocates((state, world, pos) -> false)
				.blockVision((state, world, pos) -> false)));
		SIRENIA_GRAFT = add("sirenia_graft", new SireniaGraftBlock(AbstractBlock.Settings.create()
				.mapColor(MapColor.TEAL)
				.strength(0.3f)
				.nonOpaque()
				.pistonBehavior(PistonBehavior.DESTROY)
				.sounds(BlockSoundGroup.MANGROVE_ROOTS)
				.luminance(state -> SireniaGraftBlock.lightFor(state.get(SireniaGraftBlock.STAGE)))));
	}

	private static AbstractBlock.Settings logSettings(MapColor color) {
		return AbstractBlock.Settings.create()
				.mapColor(color)
				.instrument(NoteBlockInstrument.BASS)
				.strength(2.0f)
				.sounds(BlockSoundGroup.WOOD);
	}

	private static Block add(String id, Block block) {
		return Registry.register(Registries.BLOCK, Identifier.of(ZxkStudio.MOD_ID, id), block);
	}
}
