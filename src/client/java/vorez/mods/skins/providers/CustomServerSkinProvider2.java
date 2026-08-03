package vorez.mods.skins.providers;

import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.impl.Shared;
import vorez.mods.skins.impl.SkinData;
import vorez.mods.skins.impl.fabric.ImageUtils;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Function;

public class CustomServerSkinProvider2 implements ISkinProvider {

    private Function<ByteBuffer, ByteBuffer> _filter;
    private String _host;

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);
        SharedPool.execute(() -> {
            if (_host != null && !_host.isEmpty()) {
                String url = replaceValues(_host, profile);
                if (!_host.equals(url)) {
                    Shared.downloadSkin(url, Runnable::run)
                            .thenAccept(optional -> {
                                optional.ifPresent(data -> {
                                    if (ImageUtils.validateData(data)) {
                                        skin.put(data, ImageUtils.judgeSkinType(data));
                                    }
                                });
                            });
                }
            }
        });
        return skin;
    }

    private String replaceValues(String host, IPlayerProfile profile) {
        return replaceValues(host, profile, "%name%", "%uuid%", "%auto%");
    }

    private String replaceValues(String host, IPlayerProfile profile, String nameKey, String uuidKey, String autoKey) {
        UUID id = profile.getPlayerID();
        String name = profile.getPlayerName();
        boolean isOffline = Shared.isOfflinePlayer(id, name);
        return host.replace(nameKey, name).replace(uuidKey, id.toString()).replace(autoKey, isOffline ? name : id.toString());
    }

    public CustomServerSkinProvider2 setHost(String host) {
        _host = host;
        return this;
    }

    public CustomServerSkinProvider2 withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        return this;
    }

}
