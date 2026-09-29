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

package com.creeperconset;

import net.minecraft.world.entity.monster.Creeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CreeperConsentState {
    public static final String MOD_ID = "creeperconset";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final long DENIAL_DURATION_TICKS = 72000L;
    public static final long FLEE_DURATION_TICKS = 6000L;
    public static final double BATCH_DENY_RADIUS = 6.0;
    public static final long DENY_COOLDOWN_TICKS = 200L;

    private static final Map<UUID, Long> playerDenyCooldowns = new ConcurrentHashMap<>();

    private static CreeperConsentSavedData savedData = null;

    private static final Map<UUID, Creeper> awaitingConsent = new ConcurrentHashMap<>();
    private static final Set<UUID> requestedCreepers = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> clientHandledCreepers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> deniedCreepersFallback = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> fleeCreepersFallback = new ConcurrentHashMap<>();
    private static final Set<UUID> friendlyCreepersFallback = ConcurrentHashMap.newKeySet();
    private static UUID clientPendingCreeperUuid = null;

    private static String[] names = new String[0];
    private static final Random RANDOM = new Random();

    public static void setSavedData(CreeperConsentSavedData data) {
        savedData = data;
    }

    public static boolean markRequested(UUID uuid) {
        return requestedCreepers.add(uuid);
    }

    public static void addAwaitingConsent(UUID uuid, Creeper creeper) {
        awaitingConsent.put(uuid, creeper);
    }

    public static Creeper removeAwaitingConsent(UUID uuid) {
        return awaitingConsent.remove(uuid);
    }

    public static void removeRequested(UUID uuid) {
        requestedCreepers.remove(uuid);
    }

    public static Collection<Map.Entry<UUID, Creeper>> getAwaitingConsentEntries() {
        return new ArrayList<>(awaitingConsent.entrySet());
    }

    public static void addDeniedCreeper(UUID uuid, long expiry) {
        if (savedData != null) {
            savedData.addDeniedCreeper(uuid, expiry);
        } else {
            deniedCreepersFallback.put(uuid, expiry);
        }
    }

    public static void addFleeCreeper(UUID uuid, long expiry) {
        if (savedData != null) {
            savedData.addFleeCreeper(uuid, expiry);
        } else {
            fleeCreepersFallback.put(uuid, expiry);
        }
    }

    public static boolean isCreeperDenied(UUID uuid, long gameTime) {
        if (savedData != null) {
            return savedData.isCreeperDenied(uuid, gameTime);
        }
        Long expiry = deniedCreepersFallback.get(uuid);
        if (expiry == null) return false;
        return gameTime < expiry;
    }

    public static boolean clearExpiredDenial(UUID uuid, long gameTime) {
        if (savedData != null) {
            return savedData.clearExpiredDenial(uuid, gameTime);
        }
        Long expiry = deniedCreepersFallback.get(uuid);
        if (expiry == null) return false;
        if (gameTime >= expiry) {
            deniedCreepersFallback.remove(uuid);
            fleeCreepersFallback.remove(uuid);
            friendlyCreepersFallback.remove(uuid);
            return true;
        }
        return false;
    }

    public static boolean isInFleePeriod(UUID uuid, long gameTime) {
        if (savedData != null) {
            return savedData.isInFleePeriod(uuid, gameTime);
        }
        Long expiry = fleeCreepersFallback.get(uuid);
        if (expiry == null) return false;
        if (gameTime < expiry) return true;
        return false;
    }

    public static boolean setClientPendingCreeper(UUID creeperUuid) {
        if (!clientHandledCreepers.add(creeperUuid)) {
            LOGGER.info("Client already handling creeper {}, ignoring duplicate request", creeperUuid);
            return false;
        }

        clientPendingCreeperUuid = creeperUuid;
        LOGGER.info("Client received consent request for creeper {}", creeperUuid);
        return true;
    }

    public static UUID getClientPendingCreeperUuid() {
        return clientPendingCreeperUuid;
    }

    public static void clearClientPendingCreeper(UUID creeperUuid) {
        if (clientPendingCreeperUuid != null && clientPendingCreeperUuid.equals(creeperUuid)) {
            clientPendingCreeperUuid = null;
        }
        clientHandledCreepers.remove(creeperUuid);
    }

    public static void addFriendlyCreeper(UUID uuid) {
        if (savedData != null) {
            savedData.addFriendlyCreeper(uuid);
        } else {
            friendlyCreepersFallback.add(uuid);
        }
    }

    public static boolean isFriendlyCreeper(UUID uuid) {
        if (savedData != null) {
            return savedData.isFriendlyCreeper(uuid);
        }
        return friendlyCreepersFallback.contains(uuid);
    }

    public static void removeFriendlyCreeper(UUID uuid) {
        if (savedData != null) {
            savedData.removeFriendlyCreeper(uuid);
        } else {
            friendlyCreepersFallback.remove(uuid);
        }
    }

    public static Collection<UUID> getFriendlyCreepers() {
        if (savedData != null) {
            return savedData.getFriendlyCreepers();
        }
        return friendlyCreepersFallback;
    }

    public static void recordPlayerDeny(UUID playerUuid, long gameTime) {
        playerDenyCooldowns.put(playerUuid, gameTime + DENY_COOLDOWN_TICKS);
    }

    public static long getPlayerDenyCooldownExpiry(UUID playerUuid) {
        return playerDenyCooldowns.getOrDefault(playerUuid, 0L);
    }

    public static void clearPlayerDenyCooldown(UUID playerUuid) {
        playerDenyCooldowns.remove(playerUuid);
    }

    public static void setNames(String[] newNames) {
        names = newNames;
    }

    public static String getRandomName() {
        if (names.length == 0) return null;
        return names[RANDOM.nextInt(names.length)];
    }
}
