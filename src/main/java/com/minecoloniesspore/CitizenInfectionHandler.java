package com.minecoloniesspore;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Handles Fungal Infection for Minecolonies Citizens:
 * 1. Citizens can contract the Mycelium infection when damaged by Spore mobs or standing on fungal blocks.
 * 2. Infected Citizens show physical symptoms (spore particles, slowness, weakness).
 * 3. If an infected Citizen dies, they transform into an infected Spore entity retaining their name,
 *    which is recognized as a valid infection conversion inside the colony.
 */
public class CitizenInfectionHandler {

    public static final String NBT_SPORE_INFECTED = "SporeInfected";
    public static final String NBT_SPORE_INFECT_TIME = "SporeInfectTime";
    public static final String NBT_SPORE_CONVERSION = "SporeInfectionConversion";
    public static final ResourceLocation MYCELIUM_EFFECT_ID = ResourceLocation.fromNamespaceAndPath("spore", "mycelium_ef");

    /**
     * Infects a citizen with the Spore mycelium infection.
     */
    public static void infectCitizen(AbstractEntityCitizen citizen) {
        if (citizen.level().isClientSide()) return;

        MobEffect mycelium = BuiltInRegistries.MOB_EFFECT.get(MYCELIUM_EFFECT_ID);
        if (mycelium != null) {
            citizen.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mycelium), 1200, 0));
        }

        citizen.getPersistentData().putBoolean(NBT_SPORE_INFECTED, true);
        citizen.getPersistentData().putLong(NBT_SPORE_INFECT_TIME, citizen.level().getGameTime());

        if (citizen.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, citizen.getX(), citizen.getY() + 1.0D, citizen.getZ(), 18, 0.3D, 0.5D, 0.3D, 0.05D);
            sl.playSound(null, citizen.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.NEUTRAL, 0.7F, 0.5F);
        }

        MinecoloniesSporeIntegration.LOGGER.info("Citizen {} caught fungal infection!", citizen.getName().getString());
    }

    /**
     * Checks if a citizen is currently infected.
     */
    public static boolean isCitizenInfected(AbstractEntityCitizen citizen) {
        MobEffect mycelium = BuiltInRegistries.MOB_EFFECT.get(MYCELIUM_EFFECT_ID);
        boolean hasEffect = mycelium != null && citizen.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mycelium));
        boolean hasTag = citizen.getPersistentData().getBoolean(NBT_SPORE_INFECTED);
        return hasEffect || hasTag;
    }

    /**
     * Listens for damage dealt to citizens by Spore mobs or spore effects.
     */
    @SubscribeEvent
    public static void onCitizenDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof AbstractEntityCitizen citizen) {
            Entity attacker = event.getSource().getEntity();
            if (attacker == null) {
                attacker = event.getSource().getDirectEntity();
            }

            boolean fromSpore = false;
            if (attacker != null) {
                ResourceLocation attackerKey = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
                if (attackerKey != null && "spore".equals(attackerKey.getNamespace())) {
                    fromSpore = true;
                }
            }

            String msgId = event.getSource().getMsgId();
            if (msgId != null && (msgId.contains("spore") || msgId.contains("infect") || msgId.contains("mycelium"))) {
                fromSpore = true;
            }

            if (fromSpore) {
                infectCitizen(citizen);
            }
        }
    }

    /**
     * Handles infection progression, symptoms, environmental infection from fungal blocks,
     * and potential recovery if cured.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractEntityCitizen citizen)) {
            return;
        }

        if (citizen.level().isClientSide() || !citizen.isAlive()) {
            return;
        }

        // 1. Environmental infection: standing on fungal blocks
        if (citizen.tickCount % 40 == 0) {
            BlockPos groundPos = citizen.blockPosition().below();
            BlockState blockState = citizen.level().getBlockState(groundPos);
            ResourceLocation blockKey = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
            if (blockKey != null && "spore".equals(blockKey.getNamespace())) {
                if (citizen.getRandom().nextFloat() < 0.25F) {
                    infectCitizen(citizen);
                }
            }
        }

        // 2. Active infection symptoms & particles
        if (isCitizenInfected(citizen)) {
            MobEffect mycelium = BuiltInRegistries.MOB_EFFECT.get(MYCELIUM_EFFECT_ID);
            boolean hasEffect = mycelium != null && citizen.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mycelium));

            // Check if cured (e.g. drank milk or doctor cured, effect gone for over 100 ticks)
            if (!hasEffect && citizen.getPersistentData().contains(NBT_SPORE_INFECT_TIME)) {
                long infectTime = citizen.getPersistentData().getLong(NBT_SPORE_INFECT_TIME);
                if (citizen.level().getGameTime() - infectTime > 1200) {
                    citizen.getPersistentData().remove(NBT_SPORE_INFECTED);
                    citizen.getPersistentData().remove(NBT_SPORE_INFECT_TIME);
                    return;
                }
            }

            // Infection symptoms: spore particles, slowness and weakness
            if (citizen.tickCount % 20 == 0 && citizen.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.MYCELIUM, citizen.getX(), citizen.getY() + 1.2D, citizen.getZ(), 4, 0.2D, 0.3D, 0.2D, 0.02D);
                citizen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                citizen.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            }
        }
    }

    /**
     * When an infected citizen dies, transform them into a Spore infected entity!
     */
    @SubscribeEvent
    public static void onCitizenDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof AbstractEntityCitizen citizen)) {
            return;
        }

        if (citizen.level().isClientSide() || !(citizen.level() instanceof ServerLevel sl)) {
            return;
        }

        Entity killer = event.getSource().getEntity();
        if (killer == null) killer = event.getSource().getDirectEntity();
        boolean killedBySpore = false;
        if (killer != null) {
            ResourceLocation killerKey = BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType());
            if (killerKey != null && "spore".equals(killerKey.getNamespace())) {
                killedBySpore = true;
            }
        }

        if (isCitizenInfected(citizen) || killedBySpore) {
            transformCitizenToInfected(citizen, sl);
        }
    }

    /**
     * Converts a deceased citizen into an infected entity with matching profession and identity.
     */
    private static void transformCitizenToInfected(AbstractEntityCitizen citizen, ServerLevel sl) {
        boolean isGuard = false;
        try {
            var jobHandler = citizen.getCitizenJobHandler();
            if (jobHandler != null && jobHandler.getColonyJob() != null) {
                var job = jobHandler.getColonyJob();
                isGuard = job.isGuard();
                if (!isGuard && job.getJobRegistryEntry() != null) {
                    String jobKey = job.getJobRegistryEntry().getKey().toString().toLowerCase();
                    if (jobKey.contains("guard") || jobKey.contains("knight") ||
                        jobKey.contains("ranger") || jobKey.contains("gunner") ||
                        jobKey.contains("mage") || jobKey.contains("druid")) {
                        isGuard = true;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Select entity type: Guard -> spore:knight or spore:inf_human; Civilian -> spore:inf_villager or spore:inf_human
        String primaryId = isGuard ? "spore:knight" : "spore:inf_villager";
        EntityType<?> targetType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(primaryId));
        if (targetType == null) {
            targetType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("spore:inf_human"));
        }
        if (targetType == null) {
            return;
        }

        Entity infectedEntity = targetType.create(sl);
        if (infectedEntity == null) {
            return;
        }

        // Set position and rotation to match the deceased citizen
        infectedEntity.moveTo(citizen.getX(), citizen.getY(), citizen.getZ(), citizen.getYRot(), citizen.getXRot());

        // Retain citizen custom name
        Component citizenName = citizen.getCustomName() != null ? citizen.getCustomName() : citizen.getName();
        infectedEntity.setCustomName(Component.literal("Infected " + citizenName.getString()));
        infectedEntity.setCustomNameVisible(true);

        // Mark persistent conversion tag so ColonySpawnBlocker permits this infection inside the colony
        infectedEntity.getPersistentData().putBoolean(NBT_SPORE_CONVERSION, true);

        if (infectedEntity instanceof Mob mob) {
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(citizen.blockPosition()), MobSpawnType.CONVERSION, null);
        }

        // Spawn into the world
        sl.addFreshEntity(infectedEntity);

        // Dramatic infection burst effects
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, citizen.getX(), citizen.getY() + 1.0D, citizen.getZ(), 35, 0.4D, 0.8D, 0.4D, 0.08D);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, citizen.getX(), citizen.getY() + 0.5D, citizen.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.04D);
        sl.playSound(null, citizen.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.2F, 0.7F);

        MinecoloniesSporeIntegration.LOGGER.info("Citizen {} succumbed to infection and transformed into {}!", citizenName.getString(), primaryId);
    }
}
