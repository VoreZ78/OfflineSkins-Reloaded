package vorez.mods.skins.impl.fabric;

import vorez.mods.skins.impl.SkinData;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageUtils {

    public static String judgeSkinType(byte[] data) {
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w == h * 2)
                return "default";
            if (w == h) {
                int r = Math.max(w / 64, 1);
                if (((image.getPixel(55 * r, 20 * r) & 0xFF000000) >>> 24) == 0)
                    return "slim";
                return "default";
            }
            return "unknown";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    public static ByteBuffer legacyFilter(ByteBuffer buffer) {
        try {
            ByteBuffer readBuffer = buffer.asReadOnlyBuffer();
            readBuffer.rewind();

            try (NativeImage input = NativeImage.read(readBuffer)) {
                int width = input.getWidth();
                int height = input.getHeight();

                if ((width == 22 && height == 17) || (width == 64 && height == 32 && !isSkinRequest(buffer))) {
                    buffer.rewind();
                    return buffer;
                }

                try (NativeImage output = new NativeImage(width, width, true)) {
                    int r = Math.max(width / 64, 1);
                    boolean f = width == height * 2;
                    output.copyFrom(input);
                    if (f) {
                        output.fillRect(0 * r, 32 * r, 64 * r, 32 * r, 0);
                        output.copyRect(4 * r, 16 * r, 16 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(8 * r, 16 * r, 16 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(0 * r, 20 * r, 24 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(4 * r, 20 * r, 16 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(8 * r, 20 * r, 8 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(12 * r, 20 * r, 16 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(44 * r, 16 * r, -8 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(48 * r, 16 * r, -8 * r, 32 * r, 4 * r, 4 * r, true, false);
                        output.copyRect(40 * r, 20 * r, 0 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(44 * r, 20 * r, -8 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(48 * r, 20 * r, -16 * r, 32 * r, 4 * r, 12 * r, true, false);
                        output.copyRect(52 * r, 20 * r, -8 * r, 32 * r, 4 * r, 12 * r, true, false);
                    }

                    setAreaOpaque(output, 0 * r, 0 * r, 32 * r, 16 * r);
                    if (f)
                        setAreaTransparent(output, 32 * r, 0 * r, 64 * r, 32 * r);
                    setAreaOpaque(output, 0 * r, 16 * r, 64 * r, 32 * r);
                    setAreaOpaque(output, 16 * r, 48 * r, 48 * r, 64 * r);

                    return imageToBuffer(output);
                }
            }
        } catch (Throwable t) {
            buffer.rewind();
            return buffer;
        }
    }

    private static boolean isSkinRequest(ByteBuffer buffer) {
        return true;
    }

    private static ByteBuffer imageToBuffer(NativeImage image) throws IOException {
        Path path = Files.createTempFile(null, null);
        try {
            image.writeToFile(path);
            return SkinData.toBuffer(Files.readAllBytes(path));
        } finally {
            File file = path.toFile();
            if (file.exists() && !file.delete())
                file.deleteOnExit();
        }
    }

    private static void setAreaOpaque(NativeImage image, int x, int y, int width, int height) {
        int endX = Math.min(width, image.getWidth());
        int endY = Math.min(height, image.getHeight());
        for (int i = x; i < endX; ++i)
            for (int j = y; j < endY; ++j)
                image.setPixel(i, j, image.getPixel(i, j) | -16777216);
    }

    private static void setAreaTransparent(NativeImage image, int x, int y, int width, int height) {
        int endX = Math.min(width, image.getWidth());
        int endY = Math.min(height, image.getHeight());

        for (int i = x; i < endX; ++i)
            for (int j = y; j < endY; ++j)
                if ((image.getPixel(i, j) >> 24 & 255) < 128)
                    return;

        for (int l = x; l < endX; ++l)
            for (int i1 = y; i1 < endY; ++i1)
                image.setPixel(l, i1, image.getPixel(l, i1) & 16777215);
    }

    public static boolean validateData(byte[] data) {
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
            return image != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean validateSkin(byte[] data, boolean allowHd) {
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
            int width = image.getWidth();
            int height = image.getHeight();

            // Vanilla skin only
            if (!allowHd) {
                return width == 64 && (height == 32 || height == 64);
            }

            // HD skin
            if (width < 64 || height < 32) {
                return false;
            }

            if (width % 64 != 0) {
                return false;
            }

            int scale = width / 64;

            return height == 32 * scale || height == 64 * scale;

        } catch (Throwable t) {
            System.err.println(
                    "[OfflineSkins-Reloaded] Failed to validate skin: "
                            + t.getClass().getName()
                            + ": "
                            + t.getMessage()
            );
            return false;
        }
    }

    public static boolean validateCape(byte[] data, boolean allowHd) {
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
            int width = image.getWidth();
            int height = image.getHeight();

            // Legacy cape format
            if (width == 22 && height == 17) {
                return true;
            }

            // Vanilla cape
            if (!allowHd) {
                return width == 64 && height == 32;
            }

            // HD cape
            if (width < 64 || height < 32) {
                return false;
            }

            if (width % 64 != 0) {
                return false;
            }

            int scale = width / 64;

            return height == 32 * scale;

        } catch (Throwable t) {
            System.err.println(
                    "[OfflineSkins-Reloaded] Failed to validate cape: "
                            + t.getClass().getName()
                            + ": "
                            + t.getMessage()
            );
            return false;
        }
    }
}

