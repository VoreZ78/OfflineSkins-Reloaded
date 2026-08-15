package vorez.lib;

import vorez.mods.skins.impl.specifications.URLCheck;
import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;

public final class URLConnectionValidator {

    private static final int CONNECT_TIMEOUT_MS = 1000;
    private static final int READ_TIMEOUT_MS = 1000;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static String lastUrl = "";
    private static boolean lastReplaceAuto;
    private static boolean lastAllowHTTP;
    private static URLCheck lastResult = URLCheck.INVALID_URL;
    private static long lastTime;

    private URLConnectionValidator() {
    }

    public static URLCheck checkConnection(
            String url,
            boolean replaceAuto,
            boolean useServer,
            boolean cape
    ) {
        /*
         * If the Custom Server is disabled, do not perform any further checks.
         */
        if (!useServer) {
            return URLCheck.CUSTOM_SERVER_DISABLE;
        }

        if (url == null || url.isBlank()) {
            return URLCheck.INVALID_URL;
        }

        String checkedUrl = replaceAuto
                ? url.replace("%auto%", "testplayer")
                : url;

        try {
            URI uri = URI.create(checkedUrl);
            String scheme = uri.getScheme();

            /*
             * Read the HTTP permission directly from the config file.
             */
            ConfigOptions config = FabricOfflineSkinsReloaded.loadConfigSnapshot();
            boolean allowHTTP = config != null && config.allowHTTP;

            /*
             * If HTTP is disabled, do not create a network connection.
             */
            if ("http".equalsIgnoreCase(scheme) && !allowHTTP) {
                lastUrl = url;
                lastReplaceAuto = replaceAuto;
                lastAllowHTTP = allowHTTP;
                lastResult = URLCheck.HTTP_DENIED;
                lastTime = System.currentTimeMillis();

                return lastResult;
            }

            /*
             * Check the cache only after checking the HTTP permission.
             * This prevents a cached SUCCESS from being returned after
             * HTTP has been disabled.
             */
            long now = System.currentTimeMillis();

            if (url.equals(lastUrl)
                    && replaceAuto == lastReplaceAuto
                    && allowHTTP == lastAllowHTTP
                    && now - lastTime < 2000) {
                return lastResult;
            }

            lastUrl = url;
            lastReplaceAuto = replaceAuto;
            lastAllowHTTP = allowHTTP;
            lastTime = now;

            HttpURLConnection connection = null;

            try {
                URL target = uri.toURL();

                connection = (HttpURLConnection) target.openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);

                int code = connection.getResponseCode();

                if (code == HttpURLConnection.HTTP_OK) {
                    String type = connection.getContentType();

                    if (type == null || !type.startsWith("image/")) {
                        lastResult = cape
                                ? URLCheck.INVALID_CAPE
                                : URLCheck.INVALID_SKIN;
                        return lastResult;
                    }

                    lastResult = URLCheck.SUCCESS;
                    return lastResult;
                }

                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    lastResult = replaceAuto
                            ? URLCheck.SUCCESS
                            : URLCheck.ERROR_404;
                    return lastResult;
                }

                if (code >= 500) {
                    lastResult = URLCheck.OFFLINE;
                    return lastResult;
                }

                if (code >= 400) {
                    lastResult = URLCheck.NO_RESPONSE;
                    return lastResult;
                }

                lastResult = URLCheck.SUCCESS;
                return lastResult;

            } catch (SocketTimeoutException e) {
                lastResult = URLCheck.NO_RESPONSE;

            } catch (UnknownHostException e) {
                lastResult = URLCheck.INVALID_URL;

            } catch (ConnectException e) {
                lastResult = URLCheck.OFFLINE;

            } catch (IllegalArgumentException e) {
                lastResult = URLCheck.INVALID_URL;

            } catch (IOException e) {
                lastResult = URLCheck.NO_RESPONSE;

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return lastResult;

        } catch (IllegalArgumentException e) {
            lastResult = URLCheck.INVALID_URL;
            return lastResult;
        }
    }
}