package ladysnake.pickyourpoison.common;

import ladysnake.pickyourpoison.common.entity.PoisonDartEntity;
import ladysnake.pickyourpoison.common.entity.PoisonDartFrogEntity;
import ladysnake.pickyourpoison.common.item.PoisonDartFrogBowlItem;
import ladysnake.pickyourpoison.common.item.ThrowingDartItem;
import ladysnake.pickyourpoison.common.statuseffect.EmptyStatusEffect;
import ladysnake.pickyourpoison.common.statuseffect.NumbnessStatusEffect;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import software.bernie.geckolib3.GeckoLib;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PickYourPoison implements ModInitializer {
    public static final String MODID = "pickyourpoison";
    private static final String FROGGY_PLAYERS_URL = "http://doctor4t.uuid.gg/pyp-data";
    public static final ArrayList<UUID> FROGGY_PLAYERS = new ArrayList<>();

    // STATUS EFFECTS
    public static final StatusEffect VULNERABILITY = registerStatusEffect("vulnerability", new EmptyStatusEffect(StatusEffectCategory.HARMFUL, 0xFF891C));
    public static final StatusEffect COMATOSE = registerStatusEffect("comatose", new EmptyStatusEffect(StatusEffectCategory.HARMFUL, 0x35A2F3));
    public static final StatusEffect NUMBNESS = registerStatusEffect("numbness", new NumbnessStatusEffect(StatusEffectCategory.HARMFUL, 0x62B229));
    public static final StatusEffect TORPOR = registerStatusEffect("torpor", new EmptyStatusEffect(StatusEffectCategory.HARMFUL, 0xD8C0B8));
    public static final StatusEffect BATRACHOTOXIN = registerStatusEffect("batrachotoxin", new EmptyStatusEffect(StatusEffectCategory.HARMFUL, 0xEAD040));
    public static final StatusEffect STIMULATION = registerStatusEffect("stimulation", new EmptyStatusEffect(StatusEffectCategory.HARMFUL, 0xD85252).addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "91AEAA56-376B-4498-935B-2F7F68070635", 0.2f, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    // ENTITIES
    public static EntityType<PoisonDartFrogEntity> POISON_DART_FROG;
    public static EntityType<PoisonDartEntity> POISON_DART;
    // ITEMS
    public static Item POISON_DART_FROG_SPAWN_EGG;
    public static Item BLUE_POISON_DART_FROG_BOWL;
    public static Item GOLDEN_POISON_DART_FROG_BOWL;
    public static Item GREEN_POISON_DART_FROG_BOWL;
    public static Item ORANGE_POISON_DART_FROG_BOWL;
    public static Item CRIMSON_POISON_DART_FROG_BOWL;
    public static Item RED_POISON_DART_FROG_BOWL;
    public static Item LUXALAMANDER_BOWL;
    public static Item THROWING_DART;
    public static Item COMATOSE_POISON_DART;
    public static Item BATRACHOTOXIN_POISON_DART;
    public static Item NUMBNESS_POISON_DART;
    public static Item VULNERABILITY_POISON_DART;
    public static Item TORPOR_POISON_DART;
    public static Item STIMULATION_POISON_DART;
    public static Item BLINDNESS_POISON_DART;
    // SOUNDS
    public static SoundEvent ENTITY_POISON_DART_FROG_AMBIENT = new SoundEvent(new Identifier(MODID, "entity.poison_dart_frog.ambient"));
    public static SoundEvent ENTITY_POISON_DART_FROG_HURT = new SoundEvent(new Identifier(MODID, "entity.poison_dart_frog.hurt"));
    public static SoundEvent ENTITY_POISON_DART_FROG_DEATH = new SoundEvent(new Identifier(MODID, "entity.poison_dart_frog.death"));
    public static SoundEvent ENTITY_POISON_DART_HIT = new SoundEvent(new Identifier(MODID, "entity.poison_dart.hit"));
    public static SoundEvent ITEM_POISON_DART_FROG_BOWL_FILL = new SoundEvent(new Identifier(MODID, "item.poison_dart_frog_bowl.fill"));
    public static SoundEvent ITEM_POISON_DART_FROG_BOWL_EMPTY = new SoundEvent(new Identifier(MODID, "item.poison_dart_frog_bowl.empty"));
    public static SoundEvent ITEM_POISON_DART_FROG_BOWL_LICK = new SoundEvent(new Identifier(MODID, "item.poison_dart_frog_bowl.lick"));
    public static SoundEvent ITEM_POISON_DART_COAT = new SoundEvent(new Identifier(MODID, "item.poison_dart.coat"));
    public static SoundEvent ITEM_POISON_DART_THROW = new SoundEvent(new Identifier(MODID, "item.poison_dart.throw"));

    private static <T extends Entity> EntityType<T> registerEntity(String name, EntityType<T> entityType) {
        return Registry.register(Registry.ENTITY_TYPE, new Identifier(MODID, name), entityType);
    }

    public static Item registerItem(String name, Item item) {
        Registry.register(Registry.ITEM, new Identifier(MODID, name), item);
        return item;
    }

    public static Item registerDartItem(String name, Item item) {
        Registry.register(Registry.ITEM, new Identifier(MODID, name), item);

        DispenserBlock.registerBehavior(item, new ProjectileDispenserBehavior() {
            @Override
            protected ProjectileEntity createProjectile(World world, Position position, ItemStack itemStack) {
                PoisonDartEntity throwingDart = new PoisonDartEntity(world, position.getX(), position.getY(), position.getZ());
                throwingDart.setDamage(throwingDart.getDamage());
                throwingDart.setItem(itemStack);
                StatusEffectInstance statusEffectInstance = ThrowingDartItem.class.cast(itemStack.getItem()).getStatusEffectInstance();
                if (statusEffectInstance != null) {
                    StatusEffectInstance potion = new StatusEffectInstance(statusEffectInstance);
                    throwingDart.addEffect(potion);
                    throwingDart.setColor(potion.getEffectType().getColor());
                }

                itemStack.decrement(1);
                return throwingDart;
            }
        });

        return item;
    }

    private static <T extends StatusEffect> T registerStatusEffect(String name, T effect) {
        Registry.register(Registry.STATUS_EFFECT, new Identifier(MODID, name), effect);
        return effect;
    }

    // INIT
    @Override
    public void onInitialize() {
        GeckoLib.initialize();

        loadPlayerCosmetics();

        // ENTITIES
        POISON_DART_FROG = registerEntity("poison_dart_frog", FabricEntityTypeBuilder.createMob().entityFactory(PoisonDartFrogEntity::new).spawnGroup(SpawnGroup.CREATURE).dimensions(EntityDimensions.changing(0.5F, 0.4F)).trackRangeBlocks(8).spawnRestriction(SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, PoisonDartFrogEntity::canMobSpawn).build());
        FabricDefaultAttributeRegistry.register(POISON_DART_FROG, PoisonDartFrogEntity.createPoisonDartFrogAttributes());
        BiomeModifications.addSpawn(
                biome -> biome.getBiome().getCategory().equals(Biome.Category.JUNGLE),
                SpawnGroup.CREATURE, POISON_DART_FROG, 50, 2, 5
        );
        POISON_DART = registerEntity("poison_dart", FabricEntityTypeBuilder.<PoisonDartEntity>create(SpawnGroup.MISC, PoisonDartEntity::new).dimensions(EntityDimensions.changing(0.5f, 0.5f)).trackRangeBlocks(4).trackedUpdateRate(20).build());

        // ITEMS
        POISON_DART_FROG_SPAWN_EGG = registerItem("poison_dart_frog_spawn_egg", new SpawnEggItem(POISON_DART_FROG, 0x5BBCF4, 0x22286B, (new Item.Settings()).group(ItemGroup.MISC)));
        BLUE_POISON_DART_FROG_BOWL = registerItem("blue_poison_dart_frog_bowl", new PoisonDartFrogBowlItem((new Item.Settings()).group(ItemGroup.FOOD).maxCount(1), new Identifier(MODID, "textures/entity/blue.png")));
        GOLDEN_POISON_DART_FROG_BOWL = registerItem("golden_poison_dart_frog_bowl", new PoisonDartFrogBowlItem((new Item.Settings()).group(ItemGroup.FOOD).maxCount(1), new Identifier(MODID, "textures/entity/golden.png")));
        GREEN_POISON_DART_FROG_BOWL = registerItem("green_poison_dart_frog_bowl", new PoisonDartFrogBowlItem((new Item.Settings()).group(ItemGroup.FOOD).maxCount(1), new Identifier(MODID, "textures/entity/green.png")));
        ORANGE_POISON_DART_FROG_BOWL = registerItem("orange_poison_dart_frog_bowl", new PoisonDartFrogBowlItem((new Item.Settings()).group(ItemGroup.FOOD).maxCount(1), new Identifier(MODID, "textures/entity/orange.png")));
        CRIMSON_POISON_DART_FROG_BOWL = registerItem("crimson_poison_dart_frog_bowl", new PoisonDartFrogBowlItem((new Item.Settings()).group(ItemGroup.FOOD).maxCount(1), new Identifier(MODID, "textures/entity/crimson.png")));
        RED_POISON_DART_FROG_BOWL = registerItem("red_poison_dart_frog_bowl", new PoisonDartFrogBowlItem((new Item.Settings()).group(ItemGroup.FOOD).maxCount(1), new Identifier(MODID, "textures/entity/red.png")));
        LUXALAMANDER_BOWL = registerItem("luxalamander_bowl", new PoisonDartFrogBowlItem((new Item.Settings()).group(ItemGroup.FOOD).maxCount(1).rarity(Rarity.RARE), new Identifier(MODID, "textures/entity/luxintrus.png")));
        THROWING_DART = registerDartItem("throwing_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(64), null));
        COMATOSE_POISON_DART = registerDartItem("comatose_poison_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(1), new StatusEffectInstance(PickYourPoison.COMATOSE, 100))); // 5s
        BATRACHOTOXIN_POISON_DART = registerDartItem("batrachotoxin_poison_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(1), new StatusEffectInstance(PickYourPoison.BATRACHOTOXIN, 80))); // 4s
        NUMBNESS_POISON_DART = registerDartItem("numbness_poison_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(1), new StatusEffectInstance(PickYourPoison.NUMBNESS, 200))); // 10s
        VULNERABILITY_POISON_DART = registerDartItem("vulnerability_poison_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(1), new StatusEffectInstance(PickYourPoison.VULNERABILITY, 200))); // 10s
        TORPOR_POISON_DART = registerDartItem("torpor_poison_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(1), new StatusEffectInstance(PickYourPoison.TORPOR, 200))); // 10s
        STIMULATION_POISON_DART = registerDartItem("stimulation_poison_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(1), new StatusEffectInstance(PickYourPoison.STIMULATION, 600))); // 30s
        BLINDNESS_POISON_DART = registerDartItem("blindness_poison_dart", new ThrowingDartItem((new Item.Settings()).group(ItemGroup.COMBAT).maxCount(1), new StatusEffectInstance(StatusEffects.BLINDNESS, 200))); // 10s

        // SOUNDS
        ENTITY_POISON_DART_FROG_AMBIENT = Registry.register(Registry.SOUND_EVENT, ENTITY_POISON_DART_FROG_AMBIENT.getId(), ENTITY_POISON_DART_FROG_AMBIENT);
        ENTITY_POISON_DART_FROG_HURT = Registry.register(Registry.SOUND_EVENT, ENTITY_POISON_DART_FROG_HURT.getId(), ENTITY_POISON_DART_FROG_HURT);
        ENTITY_POISON_DART_FROG_DEATH = Registry.register(Registry.SOUND_EVENT, ENTITY_POISON_DART_FROG_DEATH.getId(), ENTITY_POISON_DART_FROG_DEATH);
        ENTITY_POISON_DART_HIT = Registry.register(Registry.SOUND_EVENT, ENTITY_POISON_DART_HIT.getId(), ENTITY_POISON_DART_HIT);
        ITEM_POISON_DART_FROG_BOWL_FILL = Registry.register(Registry.SOUND_EVENT, ITEM_POISON_DART_FROG_BOWL_FILL.getId(), ITEM_POISON_DART_FROG_BOWL_FILL);
        ITEM_POISON_DART_FROG_BOWL_EMPTY = Registry.register(Registry.SOUND_EVENT, ITEM_POISON_DART_FROG_BOWL_EMPTY.getId(), ITEM_POISON_DART_FROG_BOWL_EMPTY);
        ITEM_POISON_DART_FROG_BOWL_LICK = Registry.register(Registry.SOUND_EVENT, ITEM_POISON_DART_FROG_BOWL_LICK.getId(), ITEM_POISON_DART_FROG_BOWL_LICK);
        ITEM_POISON_DART_COAT = Registry.register(Registry.SOUND_EVENT, ITEM_POISON_DART_COAT.getId(), ITEM_POISON_DART_COAT);
        ITEM_POISON_DART_THROW = Registry.register(Registry.SOUND_EVENT, ITEM_POISON_DART_THROW.getId(), ITEM_POISON_DART_THROW);

        // TICK
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.hasStatusEffect(TORPOR) && (player.age % (100 / (MathHelper.clamp(player.getStatusEffect(TORPOR).getAmplifier() + 1, 1, 20))) == 0)) {
                    player.getHungerManager().add(1, 0);
                }
            }
        });
    }


    public static class JsonReader {
        private static String readAll(Reader rd) throws IOException {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }

        public static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
            try (InputStream is = new URL(url).openStream()) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String jsonText = readAll(rd);
                return new JSONArray(jsonText);
            }
        }
    }

    public static void loadPlayerCosmetics() {
        // get illuminations player cosmetics
        CompletableFuture.supplyAsync(() -> {
            try {
                return JsonReader.readJsonFromUrl(FROGGY_PLAYERS_URL);
            } catch (IOException ignored) {
                System.out.println(ignored);
            }

            return null;
        }).exceptionally(throwable -> {
            System.out.println(throwable);
            return null;
        }).thenAcceptAsync(playerData -> {
            for (Object o : playerData.toList()) {
                FROGGY_PLAYERS.add(UUID.fromString((String) o));
            }
            System.out.println(FROGGY_PLAYERS);
        }, MinecraftClient.getInstance());
    }
}
