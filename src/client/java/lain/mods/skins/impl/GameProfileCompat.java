package lain.mods.skins.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;

import java.util.UUID;

public final class GameProfileCompat {

    private GameProfileCompat() {
    }

    public static UUID id(GameProfile profile) {
        return profile == null ? null : profile.getId();
    }

    public static String name(GameProfile profile) {
        return profile == null ? null : profile.getName();
    }

    public static PropertyMap properties(GameProfile profile) {
        return profile == null ? null : profile.getProperties();
    }

}
