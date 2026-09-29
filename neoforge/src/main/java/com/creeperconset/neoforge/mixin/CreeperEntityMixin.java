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

package com.creeperconset.neoforge.mixin;

import com.creeperconset.CreeperConsentState;
import com.creeperconset.common.mixin.MobAccessor;
import com.creeperconset.neoforge.CreeperConsentNeoMod;
import com.creeperconset.payload.RemoveFriendlyCreeperPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Creeper.class)
public class CreeperEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        if (!creeper.level().isClientSide()) {
            UUID uuid = creeper.getUUID();
            long gameTime = creeper.level().getGameTime();

            if (CreeperConsentState.isCreeperDenied(uuid, gameTime)) {
                creeper.setPersistenceRequired();
                creeper.setSwellDir(-1);
                if (creeper.tickCount % 100 == 0
                        && creeper.level().getNearestPlayer(creeper, 10.0) != null) {
                    ((ServerLevel) creeper.level()).sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            creeper.getX(), creeper.getY() + 1.0, creeper.getZ(),
                            5, 0.5, 0.5, 0.5, 0.0
                    );
                }
                if (creeper.tickCount % 40 == 0
                        && CreeperConsentState.isInFleePeriod(uuid, gameTime)
                        && creeper.level().getNearestPlayer(creeper, 10.0) != null) {
                    ((ServerLevel) creeper.level()).sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            creeper.getX(), creeper.getY() + 1.0, creeper.getZ(),
                            3, 0.5, 0.5, 0.5, 0.0
                    );
                }
            } else if (CreeperConsentState.clearExpiredDenial(uuid, gameTime)) {
                creeper.setCustomName(null);
                creeper.setCustomNameVisible(false);
                RemoveFriendlyCreeperPayload removalPayload = new RemoveFriendlyCreeperPayload(uuid);
                for (ServerPlayer player : ((ServerLevel) creeper.level()).getServer().getPlayerList().getPlayers()) {
                    player.connection.send(removalPayload);
                }
                ((ServerLevel) creeper.level()).getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("Creeper's cooling off period has expired. Renegotiating terms."),
                        false
                );
            }
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        Creeper creeper = (Creeper) (Object) this;
        if (!creeper.level().isClientSide()) {
            UUID uuid = creeper.getUUID();
            long gameTime = creeper.level().getGameTime();
            if (CreeperConsentState.isCreeperDenied(uuid, gameTime)) {
                ((ServerLevel) creeper.level()).sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        creeper.getX(), creeper.getY() + 1.0, creeper.getZ(),
                        8, 0.5, 0.5, 0.5, 0.0
                );
                ci.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    @Inject(method = "ignite", at = @At("HEAD"), cancellable = true)
    private void onIgnite(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        if (!creeper.level().isClientSide()) {
            UUID uuid = creeper.getUUID();
            long gameTime = creeper.level().getGameTime();
            if (CreeperConsentState.isCreeperDenied(uuid, gameTime)) {
                CreeperConsentState.LOGGER.debug("But nobody came.");
                ci.cancel();
            }
        }
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegisterGoals(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        ((MobAccessor) creeper).getGoalSelector().addGoal(2, new AvoidEntityGoal<>(
                creeper, Player.class, 10.0f, 1.0, 1.2,
                livingEntity -> {
                    UUID uuid = creeper.getUUID();
                    long gameTime = creeper.level().getGameTime();
                    return CreeperConsentState.isCreeperDenied(uuid, gameTime)
                            && CreeperConsentState.isInFleePeriod(uuid, gameTime);
                }
        ));
    }

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void onExplode(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;

        if (!creeper.level().isClientSide()) {
            UUID uuid = creeper.getUUID();
            long gameTime = creeper.level().getGameTime();

            if (CreeperConsentState.isCreeperDenied(uuid, gameTime)) {
                ci.cancel();
                return;
            }

            if (CreeperConsentState.clearExpiredDenial(uuid, gameTime)) {
                creeper.setCustomName(null);
                creeper.setCustomNameVisible(false);
            }

            ServerPlayer nearestPlayer = (ServerPlayer) creeper.level()
                    .getNearestPlayer(creeper, 10.0);

            if (nearestPlayer != null) {
                CreeperConsentNeoMod.requestConsent(creeper, nearestPlayer);
                ci.cancel();
            }
        }
    }
}
