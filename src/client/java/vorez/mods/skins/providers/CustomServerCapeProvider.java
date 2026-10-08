package vorez.mods.skins.providers;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import vorez.lib.HDImagesNotAllowed;
import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IHttpBoolean;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.impl.Shared;
import vorez.mods.skins.impl.SkinData;
import vorez.mods.skins.impl.Utils.ImageUtils;
import vorez.mods.skins.impl.Utils.MinecraftUtils;
import vorez.mods.skins.impl.Utils.RangeUtils;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

public class CustomServerCapeProvider implements ISkinProvider, IHttpBoolean {
    private final ConfigOptions getLog = FabricOfflineSkinsReloaded.getRuntimeConfig();
    private static final Logger LOG = LogUtils.getLogger();
    private Function<ByteBuffer, ByteBuffer> _filter;
    private String _host;
    private boolean _allowHd = false;
    private int _maxHDResolution = 256;

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
                        return;
                    }
                    Optional<byte[]> header = RangeUtils.getPngHeader(url, MinecraftUtils.getProxy());

                    if (header.isEmpty())
                        return;

                    ImageUtils.ImageSize size = ImageUtils.getImageSize(header.get());

                    if (size == null)
                        return;

                    if (!ImageUtils.isCapeResolutionAllowed(
                            size.width(),
                            size.height(),
                            _allowHd,
                            _maxHDResolution
                    )) {
                        skin.put(HDImagesNotAllowed.cape(), "cape");

                        if (getLog.logInfo) {
                            LOG.warn("[OfflineSkins-Reloaded] Rejected cape for {} because image resolution is not allowed.", profile.getPlayerName());
                        }

                        return;
                    }

                    Shared.downloadImage(url, Runnable::run).thenAccept(optional -> optional.ifPresent(data -> {
                        if (!ImageUtils.validateData(data)) {
                            LOG.error("[OfflineSkins-Reloaded] Rejected cape for {} because it failed image validation.", profile.getPlayerName());
                            return;
                        }

                        skin.put(data, "cape");
                    }));
                }
            }
        });
        return skin;
    }

    private String replaceValues(String host, IPlayerProfile profile) {
        String name = profile.getPlayerName();
        return host.replace("%auto%", name + ".png");
    }

    public CustomServerCapeProvider setHost(String host) {
        _host = host;
        return this;
    }

    public CustomServerCapeProvider setAllowHd(boolean allowHd) {
        this._allowHd = allowHd;
        return this;
    }

    public  CustomServerCapeProvider setMaxHDResolution(int maxHDResolution) {
        this._maxHDResolution = maxHDResolution;
        return this;
    }

    public CustomServerCapeProvider withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        return this;
    }
}