package vorez.mods.skins.providers.latestCached;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SelectedImageStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().create();

    private final Path baseDirectory;
    private final Path selectedDirectory;
    private final Path metadataFile;

    private SelectedData data;
    private String sessionName;
    private Path sessionSource;
    private boolean remember = true;

    public SelectedImageStore(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.selectedDirectory = baseDirectory.resolve("selected");
        this.metadataFile = selectedDirectory.resolve("selected.json");

        createDirectories();
        load();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(baseDirectory);
            Files.createDirectories(selectedDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create selected directory: " + selectedDirectory, e);
        }
    }

    private synchronized void load() {
        if (!Files.isRegularFile(metadataFile)) {
            data = new SelectedData();
            return;
        }

        try (Reader reader = Files.newBufferedReader(metadataFile)) {
            data = GSON.fromJson(reader, SelectedData.class);

            if (data == null) {
                data = new SelectedData();
            }

        } catch (Exception e) {
            data = new SelectedData();
        }
    }

    private synchronized void save() {
        try {
            Path temp = metadataFile.resolveSibling("selected.json.tmp");

            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(data, writer);
            }

            Files.move(
                    temp,
                    metadataFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {
            throw new IllegalStateException("Failed to save selected metadata: " + metadataFile, e);
        }
    }

    public synchronized void setRemember(boolean remember) {
        this.remember = remember;
    }

    public synchronized boolean isRemember() {
        return remember;
    }

    public synchronized void setSelected(String playerName, Path source) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        if (source == null || !Files.isRegularFile(source)) {
            return;
        }

        Path normalizedBase = baseDirectory
                .toAbsolutePath()
                .normalize();

        Path normalizedSource = source
                .toAbsolutePath()
                .normalize();

        if (!normalizedSource.startsWith(normalizedBase)) {
            return;
        }

        if (!remember) {
            deleteSelected(data.name);
            deleteMetadata();

            data = new SelectedData();
            sessionName = playerName;
            sessionSource = normalizedSource;
            return;
        }

        String relativeSource = normalizedBase
                .relativize(normalizedSource)
                .toString()
                .replace('\\', '/');

        sessionName = null;
        sessionSource = null;

        deleteSelected(data.name);

        Path target = selectedDirectory.resolve(playerName + ".png");

        try {
            Files.copy(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy selected image: " + source, e);
        }

        data.name = playerName;
        data.source = relativeSource;

        save();
    }

    public synchronized Path getSelected(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        if (sessionName != null
                && sessionName.equals(playerName)
                && sessionSource != null
                && Files.isRegularFile(sessionSource)) {
            return sessionSource;
        }

        if (data.name == null || data.name.isBlank()) {
            return null;
        }

        if (data.source == null || data.source.isBlank()) {
            return null;
        }

        if (!data.name.equals(playerName)) {
            return null;
        }

        Path selectedFile = selectedDirectory.resolve(data.name + ".png");

        if (!Files.isRegularFile(selectedFile)) {
            return null;
        }

        return selectedFile;
    }

    public synchronized void renameSelected(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        if (sessionName != null && sessionSource != null) {
            sessionName = playerName;
            return;
        }

        if (!remember) {
            return;
        }

        if (data.name == null || data.name.isBlank()) {
            return;
        }

        if (data.name.equals(playerName)) {
            return;
        }

        Path selectedFile = selectedDirectory.resolve(data.name + ".png");

        if (!Files.isRegularFile(selectedFile)) {
            return;
        }

        Path renamed = selectedDirectory.resolve(playerName + ".png");

        try {
            Files.move(
                    selectedFile,
                    renamed,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            return;
        }

        data.name = playerName;
        save();
    }

    public synchronized String getSelectedSource() {
        if (sessionSource != null) {
            return sessionSource
                    .getFileName()
                    .toString();
        }

        return data.source;
    }

    public synchronized boolean isSelected(String source) {
        if (source == null) {
            return false;
        }

        if (sessionSource != null) {
            return sessionSource
                    .getFileName()
                    .toString()
                    .equals(source);
        }

        if (data.source == null) {
            return false;
        }

        return data.source.equals(source);
    }

    public synchronized void clear() {
        deleteSelected(data.name);

        data = new SelectedData();
        sessionName = null;
        sessionSource = null;

        save();
    }

    public synchronized void refreshSelected(String playerName) {
        if (!remember) {
            return;
        }

        if (playerName == null || playerName.isBlank()) {
            return;
        }

        if (data.name == null || data.name.isBlank()) {
            return;
        }

        if (data.source == null || data.source.isBlank()) {
            return;
        }

        Path source = baseDirectory.resolve(data.source)
                .toAbsolutePath()
                .normalize();

        Path base = baseDirectory
                .toAbsolutePath()
                .normalize();

        if (!source.startsWith(base)) {
            return;
        }

        if (!Files.isRegularFile(source)) {
            return;
        }

        setSelected(playerName, source);
    }

    private void deleteSelected(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(selectedDirectory.resolve(playerName + ".png"));
        } catch (IOException ignored) {

        }
    }

    private void deleteMetadata() {
        try {
            Files.deleteIfExists(metadataFile);
        } catch (IOException ignored) {

        }
    }

    public static class SelectedData {
        public String name;
        public String source;
    }
}