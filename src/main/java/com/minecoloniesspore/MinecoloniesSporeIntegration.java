package com.minecoloniesspore;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Minecolonies x Spore Integration Mod
 * 
 * Features:
 * 1. Colony Hostile Tag: Registers all Spore infected entities into `#minecolonies:hostile`
 *    so all guard professions proactively target and attack them.
 * 2. Colony Territory Spawn Blocker: Completely prevents any hostile monster or Spore mob
 *    from spawning naturally inside claimed colony territory, UNLESS it is an infection conversion.
 * 3. Citizen Infection System: Citizens can catch the Fungal Infection from Spore mobs or blocks,
 *    show sickness symptoms, and upon death, transform into infected Spore entities with their original identity.
 */
@Mod(MinecoloniesSporeIntegration.MODID)
public class MinecoloniesSporeIntegration {
    public static final String MODID = "minecolonies_spore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecoloniesSporeIntegration(IEventBus modEventBus) {
        LOGGER.info("Minecolonies x Spore Integration initializing...");

        // Register gameplay event listeners on NeoForge game event bus
        NeoForge.EVENT_BUS.register(ColonySpawnBlocker.class);
        NeoForge.EVENT_BUS.register(CitizenInfectionHandler.class);

        LOGGER.info("Minecolonies x Spore Integration listeners registered successfully!");
    }
}
