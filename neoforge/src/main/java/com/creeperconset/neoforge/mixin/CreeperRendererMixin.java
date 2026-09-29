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
import com.creeperconset.FriendlyCreeperRenderState;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreeperRenderer.class)
public class CreeperRendererMixin {
    @Unique
    private static final Identifier FRIENDLY_CREEPER_TEXTURE = Identifier.fromNamespaceAndPath(CreeperConsentState.MOD_ID, "textures/entity/creeper/friendly_creeper.png");

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Creeper;Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;F)V", at = @At("TAIL"))
    private void onExtractRenderState(Creeper creeper, CreeperRenderState state, float delta, CallbackInfo ci) {
        ((FriendlyCreeperRenderState) state).setFriendly(CreeperConsentState.isFriendlyCreeper(creeper.getUUID()));
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void onGetTextureLocation(CreeperRenderState state, CallbackInfoReturnable<Identifier> ci) {
        if (((FriendlyCreeperRenderState) state).isFriendly()) {
            ci.setReturnValue(FRIENDLY_CREEPER_TEXTURE);
        }
    }
}
