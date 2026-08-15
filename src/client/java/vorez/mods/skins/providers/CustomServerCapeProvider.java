package vorez.mods.skins.providers;

import vorez.lib.HDImagesNotAllowed;
import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.impl.Shared;
import vorez.mods.skins.impl.SkinData;
import vorez.mods.skins.impl.fabric.ImageUtils;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class CustomServerCapeProvider implements ISkinProvider {

    private Function<ByteBuffer, ByteBuffer> _filter;
    private String _host;
    private boolean _allowHd = false;

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);
        SharedPool.execute(() -> {
            if (_host != null && !_host.isEmpty()) {
                String url = replaceValues(_host, profile);
                if (!_host.equals(url)) {
                    if (!isHttpAllowed(url)) {
                        System.err.println("[OfflineSkins-Reloaded] Blocked HTTP cape request for " + profile.getPlayerName());
                        return;
                    }
                    Shared.downloadSkin(url, Runnable::run).thenAccept(optional -> {
                        optional.ifPresent(data -> {
                            ByteBuffer bufferForValidation = ByteBuffer.wrap(data);
                            if (ImageUtils.validateData(data) && ImageUtils.validateCape(data, _allowHd)) {
                                skin.put(data, ImageUtils.judgeSkinType(data));
                            } else {
                                // Replaces HD cape with Steve exclamation mark
                                skin.put(HDImagesNotAllowed.cape(), "cape");
                                System.err.println("[OfflineSkins-Reloaded] Rejected cape for " + profile.getPlayerName() + " because it failed image validation.");
                            }
                        });
                    });
                }
            }
        });
        return skin;
    }
    private boolean isHttpAllowed(String url) {
        try {
            URI uri = URI.create(url);
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                ConfigOptions config = FabricOfflineSkinsReloaded.loadConfigSnapshot();
                return config != null && config.allowHTTP;
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    private String replaceValues(String host, IPlayerProfile profile) {
        String name = profile.getPlayerName();
        return host.replace("%name%", name)
                .replace("%auto%", name);
    }

    public CustomServerCapeProvider setHost(String host) {
        _host = host;
        return this;
    }

    public CustomServerCapeProvider setAllowHd(boolean allowHd) {
        this._allowHd = allowHd;
        return this;
    }
}