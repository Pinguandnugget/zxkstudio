package zxk.leviathan.arena;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class GrappleItems {
    public static final Block ANCHOR = Registry.register(Registries.BLOCK, id("emergency_anchor"),
        new Block(AbstractBlock.Settings.create().strength(4.0f, 12.0f).requiresTool().luminance(s -> 7)));
    public static final Item ROD = Registry.register(Registries.ITEM, id("abyssal_grapple"),
        new GrappleRod(new Item.Settings().maxCount(1)));
    private static Identifier id(String name){return Identifier.of("leviathan_abisal",name);}
    public static void init(){
        Registry.register(Registries.ITEM,id("emergency_anchor"),new BlockItem(ANCHOR,new Item.Settings()));
        ServerTickEvents.END_SERVER_TICK.register(GrappleRod::tick);
    }
}
