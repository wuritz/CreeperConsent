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
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(SwellGoal.class)
public class SwellGoalMixin {
    @Shadow @Final
    private Creeper creeper;

    private boolean butNobodyCame = false;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void onCanUse(CallbackInfoReturnable<Boolean> ci) {
        if (!creeper.level().isClientSide()) {
            UUID uuid = creeper.getUUID();
            long gameTime = creeper.level().getGameTime();
            if (CreeperConsentState.isCreeperDenied(uuid, gameTime)) {
                ci.setReturnValue(false);
                if (!butNobodyCame) {
                    butNobodyCame = true;
                    CreeperConsentState.LOGGER.debug("But nobody came.");
                }
            }
        }
    }
}
