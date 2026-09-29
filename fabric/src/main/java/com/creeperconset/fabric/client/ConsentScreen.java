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

package com.creeperconset.fabric.client;

import com.creeperconset.CreeperConsentState;
import com.creeperconset.payload.ConsentResponsePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class ConsentScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;

    private final UUID creeperUuid;

    public ConsentScreen(UUID creeperUuid) {
        super(Component.translatable("creeperconset.screen.title"));
        this.creeperUuid = creeperUuid;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable("creeperconset.button.accept"),
                button -> handleConsent(true)
        ).bounds(
                centerX - BUTTON_WIDTH - BUTTON_SPACING / 2,
                centerY + 30,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        ).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("creeperconset.button.deny"),
                button -> handleConsent(false)
        ).bounds(
                centerX + BUTTON_SPACING / 2,
                centerY + 30,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        ).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);

        graphics.centeredText(
                this.font,
                Component.translatable("creeperconset.screen.title"),
                this.width / 2,
                this.height / 2 - 50,
                0xFFFFFFFF
        );

        graphics.centeredText(
                this.font,
                Component.translatable("creeperconset.screen.message"),
                this.width / 2,
                this.height / 2 - 20,
                0xFFFFFFFF
        );

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void handleConsent(boolean allowed) {
        ConsentResponsePayload payload = new ConsentResponsePayload(this.creeperUuid, allowed);
        ClientPlayNetworking.send(payload);

        CreeperConsentState.LOGGER.info("Sent consent response: {}", allowed);

        CreeperConsentState.clearClientPendingCreeper(this.creeperUuid);

        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
