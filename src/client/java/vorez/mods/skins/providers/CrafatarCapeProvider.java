package vorez.mods.skins.providers;

import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.impl.Shared;
import vorez.mods.skins.impl.SkinData;
import vorez.mods.skins.impl.fabric.ImageUtils;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class CrafatarCapeProvider implements ISkinProvider {

    private Function<ByteBuffer, ByteBuffer> _filter;

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);

        SharedPool.execute(() -> {
            if (!Shared.isOfflinePlayer(profile.getPlayerID(), profile.getPlayerName())) {
                Shared.downloadSkin(
                        String.format("https://crafatar.com/capes/%s", profile.getPlayerID()),
                        Runnable::run
                ).thenAccept(optional -> {
                    optional.ifPresent(data -> {
                        if (ImageUtils.validateData(data)) {
                            skin.put(data, "cape");
                        }
                    });
                });
            }
        });

        return skin;
    }

    public CrafatarCapeProvider withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        return this;
    }

}