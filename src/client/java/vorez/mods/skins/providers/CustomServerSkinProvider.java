package vorez.mods.skins.providers;

import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.impl.Shared;
import vorez.mods.skins.impl.SkinData;
import vorez.mods.skins.impl.fabric.ImageUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CustomServerSkinProvider implements ISkinProvider {

    private Function<ByteBuffer, ByteBuffer> _filter;
    private String _host;

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);

        SharedPool.execute(() -> {
            if (Shared.isOfflinePlayer(profile.getPlayerID(), profile.getPlayerName())) {

                Shared.downloadSkin(
                        String.format("%s/skins/%s", _host, profile.getPlayerName()),
                        Runnable::run
                ).thenAccept(optional -> {
                    optional.ifPresent(data -> {
                        if (ImageUtils.validateData(data)) {
                            skin.put(data, ImageUtils.judgeSkinType(data));
                        }
                    });
                });

            } else {

                Shared.downloadSkin(
                                String.format("%s/skins/%s", _host, profile.getPlayerID()),
                                Runnable::run
                        ).handle((result, throwable) -> {
                            if (result != null && result.isPresent()) {
                                return CompletableFuture.completedFuture(result);
                            }

                            return Shared.downloadSkin(
                                    String.format("%s/skins/%s", _host, profile.getPlayerName()),
                                    Runnable::run
                            );
                        }).thenCompose(Function.identity())
                        .thenAccept(optional -> {
                            optional.ifPresent(data -> {
                                if (ImageUtils.validateData(data)) {
                                    skin.put(data, ImageUtils.judgeSkinType(data));
                                }
                            });
                        });

            }
        });

        return skin;
    }

    public CustomServerSkinProvider setHost(String host) {
        _host = host;
        return this;
    }

    public CustomServerSkinProvider withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        return this;
    }
}