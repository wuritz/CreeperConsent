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

package com.creeperconsent.fabric;

import com.creeperconsent.CreeperConsentState;
import com.creeperconsent.fabric.client.ConsentScreen;
import com.creeperconsent.payload.ConsentRequestPayload;
import com.creeperconsent.payload.FriendlyCreeperPayload;
import com.creeperconsent.payload.RemoveFriendlyCreeperPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class CreeperConsentModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CreeperConsentState.LOGGER.info("Creeper Consent Mod initialized (Client)");
        registerClientNetworking();
    }

    public static void registerClientNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(ConsentRequestPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (CreeperConsentState.isFriendlyCreeper(payload.creeperUuid())) {
                    return;
                }
                CreeperConsentState.removeFriendlyCreeper(payload.creeperUuid());
                if (CreeperConsentState.setClientPendingCreeper(payload.creeperUuid())) {
                    context.client().setScreen(new ConsentScreen(payload.creeperUuid()));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(FriendlyCreeperPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                CreeperConsentState.addFriendlyCreeper(payload.creeperUuid());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RemoveFriendlyCreeperPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                CreeperConsentState.removeFriendlyCreeper(payload.creeperUuid());
            });
        });
    }

}
