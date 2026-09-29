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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CreeperConsentSavedData extends SavedData {

    final Map<UUID, Long> deniedCreepers = new HashMap<>();
    final Map<UUID, Long> fleeCreepers = new HashMap<>();
    final Set<UUID> friendlyCreepers = new HashSet<>();

    private static final Codec<Map<String, Long>> STRING_LONG_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG);

    public static final Codec<CreeperConsentSavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            STRING_LONG_MAP_CODEC.fieldOf("DeniedCreepers").forGetter(d -> {
                Map<String, Long> map = new HashMap<>();
                d.deniedCreepers.forEach((uuid, expiry) -> map.put(uuid.toString(), expiry));
                return map;
            }),
            STRING_LONG_MAP_CODEC.fieldOf("FleeCreepers").forGetter(d -> {
                Map<String, Long> map = new HashMap<>();
                d.fleeCreepers.forEach((uuid, expiry) -> map.put(uuid.toString(), expiry));
                return map;
            }),
            Codec.STRING.listOf().fieldOf("FriendlyCreepers").forGetter(d ->
                new ArrayList<>(d.friendlyCreepers.stream().map(UUID::toString).toList())
            )
        ).apply(instance, CreeperConsentSavedData::new)
    );

    public static final SavedDataType<CreeperConsentSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CreeperConsentState.MOD_ID, "consent_data"),
            CreeperConsentSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private CreeperConsentSavedData() {}

    private CreeperConsentSavedData(Map<String, Long> denied, Map<String, Long> flee, List<String> friendly) {
        denied.forEach((key, value) -> this.deniedCreepers.put(UUID.fromString(key), value));
        flee.forEach((key, value) -> this.fleeCreepers.put(UUID.fromString(key), value));
        friendly.forEach(s -> this.friendlyCreepers.add(UUID.fromString(s)));
        CreeperConsentState.LOGGER.info("Loaded consent data: {} denied, {} friendly",
                this.deniedCreepers.size(), this.friendlyCreepers.size());
    }

    public boolean isCreeperDenied(UUID uuid, long gameTime) {
        Long expiry = deniedCreepers.get(uuid);
        return expiry != null && gameTime < expiry;
    }

    public boolean clearExpiredDenial(UUID uuid, long gameTime) {
        Long expiry = deniedCreepers.get(uuid);
        if (expiry == null) return false;
        if (gameTime >= expiry) {
            deniedCreepers.remove(uuid);
            fleeCreepers.remove(uuid);
            friendlyCreepers.remove(uuid);
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isInFleePeriod(UUID uuid, long gameTime) {
        Long expiry = fleeCreepers.get(uuid);
        return expiry != null && gameTime < expiry;
    }

    public void addDeniedCreeper(UUID uuid, long expiry) {
        deniedCreepers.put(uuid, expiry);
        setDirty();
    }

    public void addFleeCreeper(UUID uuid, long expiry) {
        fleeCreepers.put(uuid, expiry);
        setDirty();
    }

    public void addFriendlyCreeper(UUID uuid) {
        friendlyCreepers.add(uuid);
        setDirty();
    }

    public boolean isFriendlyCreeper(UUID uuid) {
        return friendlyCreepers.contains(uuid);
    }

    public void removeFriendlyCreeper(UUID uuid) {
        friendlyCreepers.remove(uuid);
        setDirty();
    }

    public Collection<UUID> getFriendlyCreepers() {
        return friendlyCreepers;
    }
}
