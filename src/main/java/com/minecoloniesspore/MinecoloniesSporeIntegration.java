package com.minecoloniesspore;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Minecolonies x Spore Integration Mod
 * Enables all Minecolonies guard professions to proactively recognize,
 * target, and attack Fungal Infection: Spore infected entities.
 */
@Mod(MinecoloniesSporeIntegration.MODID)
public class MinecoloniesSporeIntegration {
    public static final String MODID = "minecolonies_spore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecoloniesSporeIntegration(IEventBus modEventBus) {
        LOGGER.info("Minecolonies x Spore Integration initialized successfully!");
    }
}

