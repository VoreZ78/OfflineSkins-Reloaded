package vorez.mods.skins.impl.Utils;

import vorez.network.SmartInternetCheck;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

public final class RangeUtils {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int HEADER_SIZE = 24;

    private RangeUtils() {
        throw new Error("NoInstance");
    }

    public static Optional<byte[]> getPngHeader(String resource, Proxy proxy) {
        if (SmartInternetCheck.shouldBlockRequests())
            return Optional.empty();

        HttpURLConnection connection = null;

        try {
            URL url = new URI(resource).toURL();

            connection = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Range", "bytes=0-23");
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Connection", "close");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL)
                return Optional.empty();

            try (InputStream input = connection.getInputStream()) {
                byte[] data = input.readNBytes(HEADER_SIZE);

                if (data.length != HEADER_SIZE)
                    return Optional.empty();

                return Optional.of(data);
            }
        } catch (Throwable ignored) {
            return Optional.empty();
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }
}