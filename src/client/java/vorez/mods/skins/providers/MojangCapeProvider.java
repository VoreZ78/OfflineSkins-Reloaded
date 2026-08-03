package vorez.mods.skins.providers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.impl.Shared;
import vorez.mods.skins.impl.SkinData;
import vorez.mods.skins.impl.fabric.ImageUtils;
import vorez.mods.skins.impl.fabric.MinecraftUtils;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class MojangCapeProvider implements ISkinProvider {

    private Function<ByteBuffer, ByteBuffer> _filter;

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);
        SharedPool.execute(() -> {
            if (!Shared.isOfflinePlayer(profile.getPlayerID(), profile.getPlayerName())) {
                MinecraftProfileTexture texture = MinecraftUtils.getSessionService().getTextures((GameProfile) profile.getOriginal()).cape();
                if (texture != null) {
                    Shared.downloadSkin(texture.getUrl(), Runnable::run)
                            .thenAccept(optional -> {
                                optional.ifPresent(data -> {
                                    if (ImageUtils.validateData(data)) {
                                        skin.put(data, "cape");
                                    }
                                });
                            });
                }
            }
        });
        return skin;
    }

    public MojangCapeProvider withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        return this;
    }

}
