/*
 * Copyright (c) 2025 Viktor Milivojević
 * CreeperConsent is licensed under Mulan PubL v2.
 * You can use this software according to the terms and conditions of the Mulan PubL v2.
 * You may obtain a copy of Mulan PubL v2 at:
 *     http://license.coscl.org.cn/MulanPubL-2.0
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PubL v2 for more details.
 */

package com.creeperconset.neoforge;

import com.creeperconset.CreeperConsentSavedData;
import com.creeperconset.CreeperConsentState;
import com.creeperconset.payload.ConsentRequestPayload;
import com.creeperconset.payload.ConsentResponsePayload;
import com.creeperconset.payload.FriendlyCreeperPayload;
import com.creeperconset.payload.RemoveFriendlyCreeperPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Mod(CreeperConsentState.MOD_ID)
public class CreeperConsentNeoMod {
    public CreeperConsentNeoMod(IEventBus modBus) {
        CreeperConsentState.LOGGER.info("Creeper Consent Mod initialized (NeoForge)");
        loadNames();
        modBus.addListener(this::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    private void onServerStarted(ServerStartedEvent event) {
        CreeperConsentSavedData data = event.getServer().getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(CreeperConsentSavedData.TYPE);
        CreeperConsentState.setSavedData(data);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            for (UUID uuid : CreeperConsentState.getFriendlyCreepers()) {
                serverPlayer.connection.send(new FriendlyCreeperPayload(uuid));
            }
        }
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CreeperConsentState.MOD_ID).versioned("1.0");
        registrar.playToServer(ConsentResponsePayload.TYPE, ConsentResponsePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                handleConsentResponse(payload.creeperUuid(), payload.allowed(), (ServerPlayer) context.player());
            });
        });
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            registrar.playToClient(ConsentRequestPayload.TYPE, ConsentRequestPayload.CODEC, (payload, context) -> {});
            registrar.playToClient(FriendlyCreeperPayload.TYPE, FriendlyCreeperPayload.CODEC, (payload, context) -> {});
            registrar.playToClient(RemoveFriendlyCreeperPayload.TYPE, RemoveFriendlyCreeperPayload.CODEC, (payload, context) -> {});
        }
    }

    private static void loadNames() {
        try (var in = CreeperConsentNeoMod.class.getResourceAsStream("/namelist.json")) {
            if (in == null) {
                CreeperConsentState.LOGGER.warn("namelist.json not found, friendly creepers will not have names");
                return;
            }
            String[] loaded = new Gson().fromJson(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    String[].class
            );
            CreeperConsentState.setNames(loaded);
            CreeperConsentState.LOGGER.info("Loaded {} names for friendly creepers", loaded.length);
        } catch (Exception e) {
            CreeperConsentState.LOGGER.error("Failed to load namelist", e);
        }
    }

    public static void requestConsent(Creeper creeper, ServerPlayer player) {
        UUID creeperUuid = creeper.getUUID();
        long gameTime = creeper.level().getGameTime();

        long cooldownExpiry = CreeperConsentState.getPlayerDenyCooldownExpiry(player.getUUID());
        if (gameTime < cooldownExpiry) {
            CreeperConsentState.LOGGER.info("Player {} on deny cooldown, auto-denying creeper {}", player.getName().getString(), creeperUuid);
            denyAndFriendlyCreeper(creeper, player, gameTime);
            return;
        }

        if (!CreeperConsentState.markRequested(creeperUuid)) {
            return;
        }

        CreeperConsentState.addAwaitingConsent(creeperUuid, creeper);

        ConsentRequestPayload payload = new ConsentRequestPayload(creeperUuid);
        player.connection.send(payload);

        CreeperConsentState.LOGGER.info("Sent consent request to player {} for creeper {}", player.getName().getString(), creeperUuid);
    }

    private static void handleConsentResponse(UUID creeperUuid, boolean allowed, ServerPlayer player) {
        Creeper creeper = CreeperConsentState.removeAwaitingConsent(creeperUuid);
        CreeperConsentState.removeRequested(creeperUuid);

        if (creeper == null || creeper.isRemoved()) {
            CreeperConsentState.LOGGER.warn("Creeper {} no longer exists", creeperUuid);
            return;
        }

        if (allowed) {
            CreeperConsentState.LOGGER.info("Player {} granted consent for explosion", player.getName().getString());
            creeper.level().explode(
                    creeper,
                    creeper.getX(),
                    creeper.getY(),
                    creeper.getZ(),
                    3.0f,
                    Level.ExplosionInteraction.MOB
            );
            creeper.discard();
        } else {
            CreeperConsentState.LOGGER.info("Player {} denied consent", player.getName().getString());
            long gameTime = creeper.level().getGameTime();
            CreeperConsentState.recordPlayerDeny(player.getUUID(), gameTime);
            denyAndFriendlyCreeper(creeper, player, gameTime);
            denyNearbyCreepers(creeper, player);
        }
    }

    private static void denyAndFriendlyCreeper(Creeper creeper, ServerPlayer player, long gameTime) {
        UUID uuid = creeper.getUUID();
        CreeperConsentState.addDeniedCreeper(uuid, gameTime + CreeperConsentState.DENIAL_DURATION_TICKS);
        CreeperConsentState.addFleeCreeper(uuid, gameTime + CreeperConsentState.FLEE_DURATION_TICKS);
        CreeperConsentState.addFriendlyCreeper(uuid);
        player.connection.send(new FriendlyCreeperPayload(uuid));

        ((ServerLevel) creeper.level()).sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                creeper.getX(), creeper.getY() + 1.0, creeper.getZ(),
                8, 0.5, 0.5, 0.5, 0.0
        );

        String name = CreeperConsentState.getRandomName();
        if (name != null) {
            creeper.setCustomName(Component.literal(name));
            creeper.setCustomNameVisible(true);
        }

        if (creeper.getRandom().nextFloat() < 0.01f) {
            player.sendSystemMessage(Component.literal("Creeper's explosion request denied. STAND STILL! COMPLY WITH THE LAW! IN CONCLU-SION..."));
        } else {
            player.sendSystemMessage(Component.literal("Creeper has been denied consent. It will respect your boundaries for 3 days."));
        }
    }

    private static void denyNearbyCreepers(Creeper handledCreeper, ServerPlayer player) {
        double radius = CreeperConsentState.BATCH_DENY_RADIUS;
        long gameTime = handledCreeper.level().getGameTime();
        int count = 0;
        for (Creeper other : ((ServerLevel) player.level()).getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(radius))) {
            if (other == handledCreeper || other.isRemoved()) continue;
            if (CreeperConsentState.isCreeperDenied(other.getUUID(), gameTime)) continue;
            denyAndFriendlyCreeper(other, player, gameTime);
            count++;
        }
        if (count > 0) {
            CreeperConsentState.LOGGER.info("Denied {} nearby creepers for player {}", count, player.getName().getString());
        }
    }
}
