package vorez.mods.skins.providers;

import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.impl.Shared;
import vorez.mods.skins.impl.SkinData;
import vorez.mods.skins.impl.Utils.ImageUtils;
import vorez.mods.skins.providers.latestCached.SelectedImageStore;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CachedCapeProvider implements ISkinProvider {
    private final File _dirN;
    private final File _dirU;
    private static final SelectedImageStore selected =
            new SelectedImageStore(Path.of(".", "cachedImages", "capes"));
    private static final Set<String> ignoredPlayers = ConcurrentHashMap.newKeySet();
    private Function<ByteBuffer, ByteBuffer> _filter;

    public CachedCapeProvider(Path workDir) {
        _dirN = new File(workDir.toFile(), "capes");
        if (!_dirN.isDirectory() && !_dirN.mkdirs())
            throw new IllegalStateException("Failed to create directory: " + _dirN);

        _dirU = new File(_dirN, "uuid");
        if (!_dirU.isDirectory() && !_dirU.mkdirs())
            throw new IllegalStateException("Failed to create directory: " + _dirU);
    }

    @Override
    public ISkin getSkin(IPlayerProfile profile) {
        SkinData skin = new SkinData();
        if (_filter != null) {
            skin.setSkinFilter(_filter);
        }

        String playerName = profile.getPlayerName();
        String playerUUID = String.valueOf(profile.getPlayerUUID());

        if (ignoredPlayers.contains(playerName)) {
            return skin;
        }

        SharedPool.execute(() -> {
            byte[] data = null;
            Path selectedPath = getSelectedCape(playerName);

            if (selectedPath != null)
                data = readFile(selectedPath);

            if (data == null && !Shared.isOfflinePlayer(profile.getPlayerUUID(), playerName))
                data = readFile(_dirU, "%s.png", playerUUID.replaceAll("-", ""));

            if (data == null && !Shared.isBlank(playerName))
                data = readFile(_dirN, "%s.png", playerName);

            if (data != null)
                skin.put(data, "cape");
        });

        return skin;
    }

    private byte[] readFile(Path file) {
        byte[] contents;
        if ((contents = Shared.readFile(file.toFile(), null, null)) != null && ImageUtils.validateData(contents))
            return contents;
        return null;
    }

    private byte[] readFile(File dir, String filename) {
        byte[] contents;
        if ((contents = Shared.readFile(new File(dir, filename), null, null)) != null && ImageUtils.validateData(contents))
            return contents;
        return null;
    }

    public static void setSelectedCape(String playerName, Path capePath) {
        ignoredPlayers.remove(playerName);
        selected.setSelected(playerName, capePath);
    }

    public static void setRememberCape(boolean rememberCape) {
        selected.setRemember(rememberCape);
    }

    public static void refreshSelectedCape(String playerName) {
        selected.refreshSelected(playerName);
    }

    public static void resetCape(String playerName) {
        selected.clear();
        ignoredPlayers.add(playerName);
    }

    public static Path getSelectedCape(String playerName) {
        if (ignoredPlayers.contains(playerName)) {
            return null;
        }

        return selected.getSelected(playerName);
    }

    public static String getSelectedCapeSource() {
        return selected.getSelectedSource();
    }

    public static boolean isSelectedCape(String source) {
        return selected.isSelected(source);
    }

    private byte[] readFile(File dir, String filename, Object... args) {
        return readFile(dir, String.format(filename, args));
    }

    public CachedCapeProvider withFilter(Function<ByteBuffer, ByteBuffer> filter) {
        _filter = filter;
        return this;
    }
}