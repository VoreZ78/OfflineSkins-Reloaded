package vorez.lib;

import java.io.IOException;
import java.io.InputStream;

public final class HDImagesNotAllowed {
    private HDImagesNotAllowed() {
    }

    private static byte[] load(String path) {
        try (InputStream in = HDImagesNotAllowed.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + path, e);
        }
    }

    public static byte[] skin() {
        return load("/assets/offlineskins-reloaded/SkinHDNotAllowed.png");
    }

    public static byte[] cape() {
        return load("/assets/offlineskins-reloaded/CapeHDNotAllowed.png");
    }
}