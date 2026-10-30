package vorez.mods.skins.api.interfaces;

import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.init.neoforge.NeoForgeOfflineSkinsReloaded;

import java.net.URI;

public interface IHttpBoolean {

    default boolean isHttpAllowed(String url) {
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme())) {
                return true;
            }
            ConfigOptions config = NeoForgeOfflineSkinsReloaded.getRuntimeConfig();

            return config.allowHTTP;

        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
