package com.sudarshanmod;

import com.sudarshanmod.entity.ChakraEntity;
import com.sudarshanmod.item.SudarshanChakraItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SudarshanMod implements ModInitializer {

    public static final String MOD_ID = "sudarshanmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ── Item ──────────────────────────────────────────────────────────────
    public static final Item SUDARSHAN_CHAKRA = new SudarshanChakraItem(
            new Item.Settings().maxCount(1).fireproof()
    );

    // ── Entity type ───────────────────────────────────────────────────────
    public static final EntityType<ChakraEntity> CHAKRA_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "chakra_entity"),
            FabricEntityTypeBuilder.<ChakraEntity>create(SpawnGroup.MISC, ChakraEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5f, 0.1f))
                    .build()
    );

    // ── Sounds ────────────────────────────────────────────────────────────
    public static final SoundEvent CHAKRA_THROW = registerSound("chakra_throw");
    public static final SoundEvent CHAKRA_HIT   = registerSound("chakra_hit");
    public static final SoundEvent CHAKRA_RETURN = registerSound("chakra_return");

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of(MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    @Override
    public void onInitialize() {
        // Register item
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "sudarshan_chakra"), SUDARSHAN_CHAKRA);

        LOGGER.info("Sudarshan Chakra mod initialised — Jai Shri Vishnu!");
    }
}
