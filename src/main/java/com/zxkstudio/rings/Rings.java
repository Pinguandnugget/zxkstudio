package com.zxkstudio.rings;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.argument.EntityArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;

public final class Rings implements ModInitializer {
    public record Ring(int branch, int tier, Item item) {}
    private static final Map<Item,Ring> RINGS = new LinkedHashMap<>();
    private static final Ring[][] TIERS = new Ring[5][3];
    private static final Item[] MATERIALS = new Item[5];
    private static final Formatting[] COLORS = {Formatting.GREEN,Formatting.GOLD,Formatting.LIGHT_PURPLE,Formatting.RED,Formatting.AQUA};
    private static final int[] GOALS = {1000,25000,1,250,600};
    private static final Map<UUID,Vec3d> LAST = new HashMap<>();
    private static final Map<UUID,Long> DASH = new HashMap<>();
    private static final Set<String> SPACE = new HashSet<>();
    private static final TagKey<net.minecraft.entity.damage.DamageType> VACUUM = damageTag("vacuum");
    private static final TagKey<net.minecraft.entity.damage.DamageType> RADIATION = damageTag("radiation");
    private static final TagKey<net.minecraft.entity.damage.DamageType> PRESSURE = damageTag("pressure");
    private static final TagKey<net.minecraft.entity.EntityType<?>> MARINE = TagKey.of(RegistryKeys.ENTITY_TYPE,id("rings/marine"));
    private static final TagKey<net.minecraft.world.biome.Biome> OCEAN = TagKey.of(RegistryKeys.BIOME,Identifier.ofVanilla("is_ocean"));
    private static Item dormant;
    private static Block forge;
    private static long tick;
    private static Identifier id(String s){return Identifier.of("zxkstudio",s);}
    private static TagKey<net.minecraft.entity.damage.DamageType> damageTag(String s){return TagKey.of(RegistryKeys.DAMAGE_TYPE,id("rings/"+s));}
    private static Item item(String key,String name,boolean unique){
        return Registry.register(Registries.ITEM,id(key),new Item(new Item.Settings().maxCount(unique?1:64).component(DataComponentTypes.ITEM_NAME,Text.literal(name))));
    }
    private static void register(String key,String name,int branch,int tier){
        Item it=Registry.register(Registries.ITEM,id("anillo_"+key),new Item(new Item.Settings().maxCount(1)
          .rarity(tier==1?Rarity.RARE:Rarity.EPIC)
          .component(DataComponentTypes.ITEM_NAME,Text.literal(name).formatted(COLORS[branch],Formatting.BOLD))
          .component(DataComponentTypes.LORE,new LoreComponent(List.of(Text.literal("Equipar en la mano secundaria").formatted(Formatting.GRAY),Text.literal(tier==1?"Despertar":tier==2?"Ascensión":"Trascendencia").formatted(COLORS[branch]))))));
        Ring r=new Ring(branch,tier,it); RINGS.put(it,r); TIERS[branch][tier-1]=r;
    }
    @Override public void onInitialize(){
        loadSpace();
        dormant=item("anillo_sin_vida","Anillo sin vida",true);
        register("vida", "Anillo de la Vida", 0, 1);
        register("renacer", "Anillo del Renacer", 0, 2);
        register("eternidad", "Corazón de la Eternidad", 0, 3);
        register("juventud", "Anillo de la Juventud", 1, 1);
        register("tiempo", "Anillo del Tiempo", 1, 2);
        register("inmortalidad", "Legado de la Inmortalidad", 1, 3);
        register("espacial", "Anillo Espacial", 2, 1);
        register("vacio", "Anillo del Vacío", 2, 2);
        register("singularidad", "Singularidad Cósmica", 2, 3);
        register("venganza", "Anillo de la Venganza", 3, 1);
        register("rencor", "Anillo del Rencor", 3, 2);
        register("ira_carmesi", "Ira Carmesí", 3, 3);
        register("mareas", "Anillo de las Mareas", 4, 1);
        register("abisal", "Anillo Abisal", 4, 2);
        register("leviatan", "Corazón del Leviatán", 4, 3);
        MATERIALS[0] = item("esencia_vital", "Esencia vital", false);
        MATERIALS[1] = item("fragmento_temporal", "Fragmento temporal", false);
        MATERIALS[2] = item("nucleo_phora", "Núcleo de Phora", false);
        MATERIALS[3] = item("alma_rencor", "Alma del rencor", false);
        MATERIALS[4] = item("escama_abisal", "Escama abisal", false);
        forge=Registry.register(Registries.BLOCK,id("forja_reliquias"),new Block(AbstractBlock.Settings.create().strength(5f,1200f).requiresTool()));
        Item forgeItem=Registry.register(Registries.ITEM,id("forja_reliquias"),new BlockItem(forge,new Item.Settings().component(DataComponentTypes.ITEM_NAME,Text.literal("Forja de Reliquias"))));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e->{e.add(dormant);RINGS.keySet().forEach(e::add);for(Item m:MATERIALS)e.add(m);e.add(forgeItem);});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            tick++;
            Set<UUID> online=new HashSet<>();
            for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){online.add(p.getUuid());update(p);}
            if(tick%200==0){LAST.keySet().retainAll(online);DASH.keySet().retainAll(online);}
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(!(source.getAttacker() instanceof ServerPlayerEntity p))return;
            if(entity instanceof RaiderEntity raider && raider.hasActiveRaid() && p.getRandom().nextFloat()<0.08f)entity.dropItem(dormant);
            Ring r=equipped(p);
            if(r!=null && r.branch==3 && entity instanceof HostileEntity)progress(p,1);
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(!world.getBlockState(hit.getBlockPos()).isOf(forge)||hand!=Hand.MAIN_HAND||player.isSpectator())return ActionResult.PASS;
            if(player instanceof ServerPlayerEntity p)upgrade(p);
            return ActionResult.SUCCESS;
        });
        UseItemCallback.EVENT.register((player,world,hand)->{
            ItemStack stack=player.getStackInHand(hand);
            Ring r=equipped(player);
            if(hand!=Hand.OFF_HAND||!player.isSneaking()||r==null||r.tier!=3||!(r.branch==2||r.branch==4)||player.isSpectator())return TypedActionResult.pass(stack);
            if(player instanceof ServerPlayerEntity p){
                if((r.branch==2&&inSpace(p))||(r.branch==4&&ocean(p))){
                    if(tick>=DASH.getOrDefault(p.getUuid(),0L)){
                        DASH.put(p.getUuid(),tick+300);
                        p.setVelocity(p.getRotationVec(1).multiply(r.branch==2?1.25:1.7));p.velocityModified=true;
                    }
                }
            }
            return TypedActionResult.success(stack,world.isClient);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,access,env)->dispatcher.register(CommandManager.literal("reliquias")
          .then(CommandManager.literal("progreso").executes(c->{ServerPlayerEntity p=c.getSource().getPlayerOrThrow();Ring r=equipped(p);p.sendMessage(Text.literal(r==null?"Equipa un anillo.":r.tier==3?"Trascendencia alcanzada.":"Dominio: "+read(p.getOffHandStack())+" / "+goal(r)),false);return 1;}))
          .then(CommandManager.literal("otorgar").requires(s->s.hasPermissionLevel(2))
            .then(CommandManager.argument("jugador",EntityArgumentType.player())
              .then(CommandManager.argument("cantidad",IntegerArgumentType.integer(1,100000)).executes(c->{ServerPlayerEntity p=EntityArgumentType.getPlayer(c,"jugador");progress(p,IntegerArgumentType.getInteger(c,"cantidad"));return 1;}))))));
    }
    private static void loadSpace(){
        Path path=FabricLoader.getInstance().getConfigDir().resolve("zxkstudio-rings-space.txt");
        try{if(Files.notExists(path))Files.writeString(path,"# Un ID de dimensión espacial por línea\nzxkstudio:space\nzxkstudio:phora\nzxkstudio:espacio\n");
            for(String line:Files.readAllLines(path)){line=line.trim();if(!line.isEmpty()&&!line.startsWith("#"))SPACE.add(line);}
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger("zxkstudio-rings").warn("Configuración espacial no disponible; se desactiva la rama espacial en dimensiones",e);}
    }
    private static boolean inSpace(PlayerEntity p){return SPACE.contains(p.getWorld().getRegistryKey().getValue().toString());}
    public static boolean ocean(PlayerEntity p){return p.isSubmergedIn(FluidTags.WATER)&&p.getWorld().getBiome(p.getBlockPos()).isIn(OCEAN);}
    public static Ring equipped(PlayerEntity p){return RINGS.get(p.getOffHandStack().getItem());}
    private static int read(ItemStack s){return s.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getInt("relic_mastery");}
    private static int goal(Ring r){return GOALS[r.branch]*(r.branch==2?1:r.tier);}
    private static void progress(ServerPlayerEntity p,int value){
        Ring r=equipped(p);if(r==null||r.tier==3||value<=0)return;
        ItemStack s=p.getOffHandStack();int current=read(s),next=Math.min(goal(r),current+value);if(current==next)return;
        NbtCompound n=s.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();n.putInt("relic_mastery",next);s.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(n));
        if(next==goal(r))p.sendMessage(Text.literal("✦ Dominio completado. Visita la Forja de Reliquias.").formatted(Formatting.GOLD),false);
    }
    private static void upgrade(ServerPlayerEntity p){
        Ring r=equipped(p);if(r==null||r.tier>=3){p.sendMessage(Text.literal("Equipa un anillo evolutivo en la mano secundaria."),true);return;}
        ItemStack material=p.getMainHandStack();int count=r.tier==1?4:8,xp=r.tier==1?30:50;
        if(read(p.getOffHandStack())<goal(r)||!material.isOf(MATERIALS[r.branch])||material.getCount()<count||(!p.isCreative()&&p.experienceLevel<xp)){
            p.sendMessage(Text.literal("Dominio "+read(p.getOffHandStack())+"/"+goal(r)+" · "+count+" materiales de rama · "+xp+" niveles XP"),true);return;
        }
        if(!p.isCreative()){material.decrement(count);p.addExperienceLevels(-xp);}
        p.setStackInHand(Hand.OFF_HAND,new ItemStack(TIERS[r.branch][r.tier].item));
        p.sendMessage(Text.literal("✦ Tu reliquia ha evolucionado.").formatted(Formatting.GOLD),false);
    }
    private static void modifier(ServerPlayerEntity p,RegistryEntry<EntityAttribute> attr,String key,double amount,EntityAttributeModifier.Operation op){
        EntityAttributeInstance instance=p.getAttributeInstance(attr);if(instance==null)return;
        Identifier ident=id("rings/"+key);EntityAttributeModifier old=instance.getModifier(ident);
        if(old!=null&&Double.compare(old.value(),amount)==0)return;
        if(old!=null)instance.removeModifier(ident);
        if(amount!=0)instance.addTemporaryModifier(new EntityAttributeModifier(ident,amount,op));
    }
    private static void update(ServerPlayerEntity p){
        Ring r=equipped(p);int t=r==null?0:r.tier,b=r==null?-1:r.branch;
        boolean space=b==2&&inSpace(p),water=b==4&&ocean(p);
        var add=EntityAttributeModifier.Operation.ADD_VALUE;var scale=EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        modifier(p,EntityAttributes.GENERIC_MAX_HEALTH,"health",b==0?4*t:0,add);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
        modifier(p,EntityAttributes.GENERIC_MOVEMENT_SPEED,"speed",b==1?(t==1?.2:.4):space?new double[]{0,.15,.35,.6}[t]:0,scale);
        modifier(p,EntityAttributes.GENERIC_GRAVITY,"gravity",space?-new double[]{0,.4,.7,1}[t]:0,scale);
        modifier(p,EntityAttributes.GENERIC_ATTACK_DAMAGE,"attack",b==3?(t<3?t*.1:.2+.2*(1-p.getHealth()/p.getMaxHealth())):0,scale);
        modifier(p,EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY,"swim",water?new double[]{0,.25,.6,1}[t]:0,add);
        modifier(p,EntityAttributes.GENERIC_OXYGEN_BONUS,"oxygen",water?new double[]{0,1,3,0}[t]:0,add);
        if(space&&!p.isOnGround()&&!p.isCreative()&&!p.isSpectator()&&!p.isFallFlying()){
            Vec3d v=p.getVelocity();
            if(t==3&&tick>=DASH.getOrDefault(p.getUuid(),0L)-280){
                p.setVelocity(v.x*.98,p.isSneaking()?-.15:Math.max(-.1,Math.min(.1,v.y*.95)),v.z*.98);
            }
        }
        if(water&&t==3)p.setAir(p.getMaxAir());
        if(tick%20!=0)return;
        Vec3d old=LAST.put(p.getUuid(),p.getPos());
        if(r==null)return;
        if(b==0&&t==3&&tick%60==0)p.heal(1);
        if(water){int interval=t==1?160:t==2?80:60;if(tick%interval==0)p.heal(t==3?2:1);progress(p,1);}
        if(b==1&&old!=null){double distance=old.distanceTo(p.getPos());if(distance<30)progress(p,(int)distance);}
    }
    public static void healed(net.minecraft.entity.LivingEntity entity,float amount){
        if(entity instanceof ServerPlayerEntity p){Ring r=equipped(p);if(r!=null&&r.branch==0)progress(p,Math.max(0,(int)Math.min(amount,p.getMaxHealth()-p.getHealth())));}
    }
    public static float reduce(net.minecraft.entity.LivingEntity entity,DamageSource source,float amount){
        if(!(entity instanceof ServerPlayerEntity p))return amount;
        Ring r=equipped(p);if(r==null)return amount;int t=r.tier;
        if(r.branch==1&&t==3&&!source.isIn(DamageTypeTags.BYPASSES_RESISTANCE))return amount*.8f;
        if(r.branch==2&&inSpace(p)){
            if(source.isIn(DamageTypeTags.IS_FALL))return 0;
            if(source.isIn(VACUUM))return amount*new float[]{1,.7f,.4f,0}[t];
            if(source.isIn(RADIATION))return amount*new float[]{1,.8f,.5f,.2f}[t];
        }
        if(r.branch==4&&ocean(p)){
            if(source.isIn(PRESSURE))return amount*new float[]{1,.75f,.4f,0}[t];
            if(source.getAttacker()!=null&&source.getAttacker().getType().isIn(MARINE))return amount*new float[]{1,.9f,.8f,.65f}[t];
            if(t==3&&source.isIn(DamageTypeTags.IS_DROWNING))return 0;
        }
        return amount;
    }
}
