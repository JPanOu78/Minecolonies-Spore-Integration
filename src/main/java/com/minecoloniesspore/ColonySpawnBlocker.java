package com.minecoloniesspore;

import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Prevents any hostile monsters and Spore infected entities from spawning
 * inside claimed colony territory, UNLESS the spawn is a result of infection/conversion.
 */
public class ColonySpawnBlocker {

    /**
     * Checks if a spawn is an allowed infection conversion.
     */
    public static boolean isAllowedInfectionSpawn(Mob mob, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.CONVERSION) {
            return true;
        }
        if (mob != null && mob.getPersistentData().getBoolean("SporeInfectionConversion")) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the entity is a restricted hostile mob or from the Spore mod.
     */
    public static boolean isRestrictedMob(Mob mob) {
        if (mob == null) return false;

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (key != null && "spore".equals(key.getNamespace())) {
            return true;
        }
        if (mob instanceof Enemy) {
            return true;
        }
        if (mob.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }
        return false;
    }

    /**
     * Checks if coordinates reside inside any active Minecolonies colony territory.
     */
    public static boolean isPosInColony(ServerLevel level, BlockPos pos) {
        try {
            IColonyManager manager = IColonyManager.getInstance();
            if (manager != null) {
                return manager.isCoordinateInAnyColony(level, pos);
            }
        } catch (Exception e) {
            MinecoloniesSporeIntegration.LOGGER.warn("Failed checking colony coordinates: {}", e.getMessage());
        }
        return false;
    }

    @SubscribeEvent
    public static void onCheckSpawnPosition(MobSpawnEvent.PositionCheck event) {
        Mob mob = event.getEntity();
        MobSpawnType spawnType = event.getSpawnType();

        // 1. Allow infection conversions
        if (isAllowedInfectionSpawn(mob, spawnType)) {
            return;
        }

        // 2. Block wild/natural spawns of restricted mobs inside colonies
        if (isRestrictedMob(mob)) {
            if (event.getLevel() instanceof ServerLevelAccessor sla) {
                ServerLevel level = sla.getLevel();
                BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
                if (isPosInColony(level, pos)) {
                    event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        MobSpawnType spawnType = event.getSpawnType();

        // 1. Allow infection conversions
        if (isAllowedInfectionSpawn(mob, spawnType)) {
            return;
        }

        // 2. Block finalized spawn inside colonies
        if (isRestrictedMob(mob)) {
            if (event.getLevel() instanceof ServerLevelAccessor sla) {
                ServerLevel level = sla.getLevel();
                BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
                if (isPosInColony(level, pos)) {
                    event.setSpawnCancelled(true);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) {
            return; // Never despawn entities that already exist and were loaded from world save
        }

        if (event.getEntity() instanceof Mob mob) {
            // 1. Allow infection conversions
            if (isAllowedInfectionSpawn(mob, null)) {
                return;
            }

            // 2. Extra safety: cancel if restricted mob attempts to join inside colony boundaries
            if (isRestrictedMob(mob) && event.getLevel() instanceof ServerLevel level) {
                BlockPos pos = mob.blockPosition();
                if (isPosInColony(level, pos)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}

