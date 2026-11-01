package com.minecoloniesspore;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenDiseaseHandler;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.datalistener.DiseasesListener;
import com.minecolonies.core.datalistener.model.Disease;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Field;

/**
 * Handles Fungal Infection for Minecolonies Citizens:
 * 1. Citizens can contract the Mycelium infection when damaged by Spore mobs or standing on fungal blocks.
 * 2. Mycelium status naturally expires and clears on its own after normal duration (no hospital strictly required).
 * 3. If a citizen goes to or is inside the Hospital, the Doctor profession cures them immediately using 1 Ice.
 * 4. Ice (minecraft:ice) is registered as a required cure item for the Hospital.
 * 5. Players can also directly cure an infected citizen with 1 Ice.
 * 6. If an infected citizen succumbs and dies, they transform into an infected Spore entity (inf_villager).
 */
public class CitizenInfectionHandler {

    public static final String NBT_SPORE_INFECTED = "SporeInfected";
    public static final String NBT_SPORE_INFECT_TIME = "SporeInfectTime";
    public static final String NBT_SPORE_CONVERSION = "SporeInfectionConversion";
    public static final ResourceLocation MYCELIUM_EFFECT_ID = ResourceLocation.fromNamespaceAndPath("spore", "mycelium_ef");
    public static final ResourceLocation MYCELIUM_DISEASE_ID = ResourceLocation.fromNamespaceAndPath("minecolonies", "mycelium");

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

        // Assign Minecolonies disease so Hospital and Doctor know about the patient and required Ice
        if (citizen.getCitizenData() != null) {
            ICitizenDiseaseHandler dh = citizen.getCitizenData().getCitizenDiseaseHandler();
            if (dh != null && !dh.isSick()) {
                Disease disease = DiseasesListener.getDisease(MYCELIUM_DISEASE_ID);
                if (disease != null) {
                    if (!dh.setDisease(disease)) {
                        try {
                            Field f = dh.getClass().getDeclaredField("disease");
                            f.setAccessible(true);
                            f.set(dh, disease);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (citizen.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, citizen.getX(), citizen.getY() + 1.0D, citizen.getZ(), 18, 0.3D, 0.5D, 0.3D, 0.05D);
            sl.playSound(null, citizen.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.NEUTRAL, 0.7F, 0.5F);
        }

        MinecoloniesSporeIntegration.LOGGER.info("Citizen {} caught fungal infection (Mycelium)!", citizen.getName().getString());
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
     * Cures a citizen from the Mycelium infection.
     */
    public static void cureCitizen(AbstractEntityCitizen citizen, String reason) {
        if (citizen.level().isClientSide()) return;

        // 1. Remove Spore potion effect & symptoms
        MobEffect mycelium = BuiltInRegistries.MOB_EFFECT.get(MYCELIUM_EFFECT_ID);
        if (mycelium != null) {
            citizen.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mycelium));
        }
        citizen.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        citizen.removeEffect(MobEffects.WEAKNESS);

        // 2. Clear infection NBT tags
        citizen.getPersistentData().remove(NBT_SPORE_INFECTED);
        citizen.getPersistentData().remove(NBT_SPORE_INFECT_TIME);

        // 3. Clear Minecolonies disease state
        if (citizen.getCitizenData() != null) {
            ICitizenDiseaseHandler dh = citizen.getCitizenData().getCitizenDiseaseHandler();
            if (dh != null && dh.isSick()) {
                dh.cure();
            }
        }

        // 4. Visual effects and sounds
        if (citizen.level() instanceof ServerLevel sl) {
            if ("doctor".equals(reason) || "player".equals(reason)) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, citizen.getX(), citizen.getY() + 1.2D, citizen.getZ(), 18, 0.3D, 0.5D, 0.3D, 0.05D);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, citizen.getX(), citizen.getY() + 1.0D, citizen.getZ(), 25, 0.4D, 0.6D, 0.4D, 0.03D);
                sl.playSound(null, citizen.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                MinecoloniesSporeIntegration.LOGGER.info("Citizen {} cured from Mycelium by {} using Ice!", citizen.getName().getString(), reason);
            } else {
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, citizen.getX(), citizen.getY() + 1.0D, citizen.getZ(), 10, 0.2D, 0.4D, 0.2D, 0.02D);
                sl.playSound(null, citizen.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8F, 1.4F);
                MinecoloniesSporeIntegration.LOGGER.info("Citizen {} Mycelium infection expired naturally after normal time.", citizen.getName().getString());
            }
        }
    }

    /**
     * Tries to cure an infected citizen if they are inside an active Hospital that has a Doctor and 1 Ice in stock.
     */
    private static boolean tryHospitalDoctorCure(AbstractEntityCitizen citizen) {
        if (citizen.getCitizenColonyHandler() == null) return false;
        IColony colony = citizen.getCitizenColonyHandler().getColonyOrRegister();
        if (colony == null) return false;

        IBuilding building = colony.getServerBuildingManager().getBuilding(citizen.blockPosition());
        if (building instanceof BuildingHospital hospital) {
            if (hospital.getAllAssignedCitizen().isEmpty()) {
                return false;
            }

            for (IItemHandler handler : hospital.getHandlers()) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (stack.is(Items.ICE)) {
                        ItemStack extracted = handler.extractItem(slot, 1, false);
                        if (!extracted.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Prevents Mycelium infection periodic damage from hurting Citizens.
     * Canceling this damage stops citizens from stuttering while walking and
     * prevents them from being kicked / bounced out of beds when sleeping.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCitizenIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof AbstractEntityCitizen citizen) {
            DamageSource source = event.getSource();
            String msgId = source.getMsgId();

            boolean isMyceliumDamage = (msgId != null && (msgId.contains("mycelium") || msgId.contains("overtake")))
                    || source.is(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("spore", "mycelium_overtake")));

            if (isMyceliumDamage) {
                event.setCanceled(true);
                event.setAmount(0.0F);
            }
        }
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
     * Handles infection progression, natural expiration over time, hospital cure,
     * and symptoms.
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

        // 2. Active infection management
        if (citizen.getPersistentData().getBoolean(NBT_SPORE_INFECTED)) {
            MobEffect mycelium = BuiltInRegistries.MOB_EFFECT.get(MYCELIUM_EFFECT_ID);
            boolean hasEffect = mycelium != null && citizen.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mycelium));

            // Case A: The Doctor at the Hospital cured the citizen via standard Minecolonies AI
            if (citizen.getCitizenData() != null) {
                ICitizenDiseaseHandler dh = citizen.getCitizenData().getCitizenDiseaseHandler();
                if (dh != null && !dh.isSick()) {
                    cureCitizen(citizen, "doctor");
                    return;
                }
            }

            // Case B: Natural recovery: The Mycelium status has run its normal duration and disappeared on its own.
            // Citizens are not forced to visit the Hospital.
            if (!hasEffect) {
                cureCitizen(citizen, "natural");
                return;
            }

            // Case C: Citizen entered Hospital building with an active Doctor and Ice in stock -> instant cure!
            if (citizen.tickCount % 20 == 0) {
                if (tryHospitalDoctorCure(citizen)) {
                    cureCitizen(citizen, "doctor");
                    return;
                }
            }

            // Infection symptoms: spore particles, slowness and weakness (only when awake)
            if (citizen.tickCount % 20 == 0 && citizen.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.MYCELIUM, citizen.getX(), citizen.getY() + 1.2D, citizen.getZ(), 4, 0.2D, 0.3D, 0.2D, 0.02D);
                if (!citizen.isSleeping()) {
                    citizen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                    citizen.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
                }
            }
        }
    }

    /**
     * Allows player to cure an infected citizen by directly right-clicking with 1 Ice.
     */
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof AbstractEntityCitizen citizen) {
            if (citizen.level().isClientSide()) return;

            if (isCitizenInfected(citizen)) {
                ItemStack heldItem = event.getItemStack();
                if (heldItem.is(Items.ICE)) {
                    if (!event.getEntity().isCreative()) {
                        heldItem.shrink(1);
                    }
                    cureCitizen(citizen, "player");
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
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
     * Converts a deceased citizen into an infected villager entity with matching identity.
     */
    private static void transformCitizenToInfected(AbstractEntityCitizen citizen, ServerLevel sl) {
        String primaryId = "spore:inf_villager";
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

        infectedEntity.moveTo(citizen.getX(), citizen.getY(), citizen.getZ(), citizen.getYRot(), citizen.getXRot());

        Component citizenName = citizen.getCustomName() != null ? citizen.getCustomName() : citizen.getName();
        infectedEntity.setCustomName(Component.literal("Infected " + citizenName.getString()));
        infectedEntity.setCustomNameVisible(true);

        infectedEntity.getPersistentData().putBoolean(NBT_SPORE_CONVERSION, true);

        if (infectedEntity instanceof Mob mob) {
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(citizen.blockPosition()), MobSpawnType.CONVERSION, null);
        }

        sl.addFreshEntity(infectedEntity);

        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, citizen.getX(), citizen.getY() + 1.0D, citizen.getZ(), 35, 0.4D, 0.8D, 0.4D, 0.08D);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, citizen.getX(), citizen.getY() + 0.5D, citizen.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.04D);
        sl.playSound(null, citizen.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.2F, 0.7F);

        MinecoloniesSporeIntegration.LOGGER.info("Citizen {} succumbed to infection and transformed into {}!", citizenName.getString(), primaryId);
    }
}
