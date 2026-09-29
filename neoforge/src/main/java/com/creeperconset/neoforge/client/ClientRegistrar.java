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

package com.creeperconset.neoforge.client;

import com.creeperconset.CreeperConsentState;
import com.creeperconset.payload.ConsentRequestPayload;
import com.creeperconset.payload.FriendlyCreeperPayload;
import com.creeperconset.payload.RemoveFriendlyCreeperPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(value = CreeperConsentState.MOD_ID, dist = Dist.CLIENT)
public class ClientRegistrar {
    public ClientRegistrar(IEventBus modBus) {
        modBus.addListener(this::onRegisterPayloadHandlers);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CreeperConsentState.MOD_ID).versioned("1.0");
        registrar.playToClient(ConsentRequestPayload.TYPE, ConsentRequestPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (CreeperConsentState.isFriendlyCreeper(payload.creeperUuid())) {
                    return;
                }
                CreeperConsentState.removeFriendlyCreeper(payload.creeperUuid());
                if (CreeperConsentState.setClientPendingCreeper(payload.creeperUuid())) {
                    Minecraft.getInstance().setScreen(new ConsentScreen(payload.creeperUuid()));
                }
            });
        });
        registrar.playToClient(FriendlyCreeperPayload.TYPE, FriendlyCreeperPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                CreeperConsentState.addFriendlyCreeper(payload.creeperUuid());
            });
        });

        registrar.playToClient(RemoveFriendlyCreeperPayload.TYPE, RemoveFriendlyCreeperPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                CreeperConsentState.removeFriendlyCreeper(payload.creeperUuid());
            });
        });
    }
}
