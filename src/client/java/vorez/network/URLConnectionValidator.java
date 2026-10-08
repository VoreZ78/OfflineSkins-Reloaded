package vorez.network;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import vorez.lib.SharedPool;
import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CompletableFuture;

public final class URLConnectionValidator {

    private static final Logger LOG = LogUtils.getLogger();
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final long UNSTABLE_THRESHOLD_MS = 4500;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static final String ELY_BY_SKIN_URL =
            "http://skinsystem.ely.by/skins/%auto%";

    private static final String ELY_BY_SKIN_TEST_URL =
            "https://skinsystem.ely.by/skins/TestPlayerVoreZ.png";

    private URLConnectionValidator() {
    }

    public static URLStatus checkConnection(String url, boolean useServer, boolean cape) {
        if (!useServer) {
            return URLStatus.CUSTOM_SERVER_DISABLED;
        }

        if (SmartInternetCheck.shouldBlockRequests()) {
            return URLStatus.NO_INTERNET;
        }

        ConfigOptions config = FabricOfflineSkinsReloaded.getRuntimeConfig();

        boolean elyBy = !cape
                && ELY_BY_SKIN_URL.equals(url)
                && ELY_BY_SKIN_URL.equals(config.linkCustomServerSkin);

        String checkedUrl;

        if (elyBy) {
            checkedUrl = ELY_BY_SKIN_TEST_URL;
        } else {
            checkedUrl = url.replace("%auto%", "agent");
        }

        URI uri;

        try {
            uri = URI.create(checkedUrl);
        } catch (IllegalArgumentException e) {
            return URLStatus.INVALID_URL;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                || host == null
                || host.isBlank()) {
            return URLStatus.INVALID_URL;
        }

        if ("http".equalsIgnoreCase(scheme) && !config.allowHTTP) {
            return URLStatus.HTTP_DENIED;
        }

        URLStatus dnsResult = checkDns(host);

        if (dnsResult != URLStatus.SUCCESS) {
            return dnsResult;
        }

        URLStatus tcpResult = checkTcp(uri, host);

        if (tcpResult != URLStatus.SUCCESS) {
            return tcpResult;
        }

        if (elyBy) {
            return evaluateSingle(sendRequest(uri, "GET"));
        }

        CompletableFuture<RequestResult> headFuture = requestAsync(uri, "HEAD");

        CompletableFuture<RequestResult> getFuture = requestAsync(uri, "GET");

        RequestResult head = headFuture.join();
        RequestResult get = getFuture.join();

        return evaluateRequests(head, get);
    }

    private static URLStatus checkDns(String host) {
        try {
            InetAddress.getAllByName(host);
            return URLStatus.SUCCESS;
        } catch (UnknownHostException e) {
            return URLStatus.DOMAIN_NOT_FOUND;
        } catch (Exception e) {
            return URLStatus.NO_RESPONSE;
        }
    }

    private static URLStatus checkTcp(URI uri, String host) {
        int port = uri.getPort();

        if (port == -1) {
            port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }

        InetAddress[] addresses;

        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return URLStatus.DOMAIN_NOT_FOUND;
        }

        long deadline = System.nanoTime() + CONNECT_TIMEOUT_MS * 1_000_000L;

        for (InetAddress address : addresses) {
            long remaining = deadline - System.nanoTime();

            if (remaining <= 0) {
                break;
            }

            int timeout = (int) Math.min(remaining / 1_000_000L, CONNECT_TIMEOUT_MS);

            if (timeout <= 0) {
                timeout = 1;
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(address, port), timeout);
                return URLStatus.SUCCESS;
            } catch (IOException ignored) {
            }
        }

        return URLStatus.OFFLINE;
    }

    private static CompletableFuture<RequestResult> requestAsync(URI uri, String method) {
        CompletableFuture<RequestResult> future = new CompletableFuture<>();

        try {
            SharedPool.execute(() -> {
                try {
                    future.complete(sendRequest(uri, method));
                } catch (Exception e) {
                    future.complete(new RequestResult(
                            null,
                            0,
                            URLStatus.NO_RESPONSE
                    ));
                }
            });
        } catch (RuntimeException e) {
            future.complete(new RequestResult(
                    null,
                    0,
                    URLStatus.NO_RESPONSE
            ));
        }

        return future;
    }

    private static RequestResult sendRequest(URI uri, String method) {
        HttpURLConnection connection = null;
        long start = System.nanoTime();

        try {
            URL target = uri.toURL();

            connection = (HttpURLConnection) target.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(method);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Connection", "close");
            connection.setUseCaches(false);

            int code = connection.getResponseCode();
            long elapsed = elapsedMillis(start);

            return new RequestResult(
                    code,
                    elapsed,
                    null
            );

        } catch (SocketTimeoutException e) {
            return new RequestResult(
                    null,
                    elapsedMillis(start),
                    URLStatus.NO_RESPONSE
            );

        } catch (UnknownHostException e) {
            return new RequestResult(
                    null,
                    elapsedMillis(start),
                    URLStatus.DOMAIN_NOT_FOUND
            );

        } catch (ConnectException e) {
            return new RequestResult(
                    null,
                    elapsedMillis(start),
                    URLStatus.OFFLINE
            );

        } catch (IllegalArgumentException e) {
            return new RequestResult(
                    null,
                    elapsedMillis(start),
                    URLStatus.INVALID_URL
            );

        } catch (IOException e) {
            return new RequestResult(
                    null,
                    elapsedMillis(start),
                    URLStatus.NO_RESPONSE
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static URLStatus evaluateSingle(RequestResult result) {
        if (!result.hasHttpResponse()) {
            return result.transportResult();
        }

        ConfigOptions config = FabricOfflineSkinsReloaded.getRuntimeConfig();

        int code = result.code();

        if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            if (config.logInfo) {
                LOG.warn("[OfflineSkins-Reloaded] HTTP response not found, HTTP CODE {}", code);
            }
            return URLStatus.ERROR_404;
        }

        if (isAccessDenied(code)) {
            if (config.logInfo) {
                LOG.warn("[OfflineSkins-Reloaded] HTTP response access denied: HTTP CODE {}", code);
            }
            return URLStatus.NO_ACCESS;
        }

        if (isServerError(code)) {
            if (config.logInfo) {
                LOG.warn("[OfflineSkins-Reloaded] HTTP response server didn't answer: HTTP CODE {}", code);
            }
            return URLStatus.NO_RESPONSE;
        }

        if (result.isUnstable()) {
            if (config.logInfo) {
                LOG.warn("[OfflineSkins-Reloaded] HTTP response unstable: HTTP CODE {}", code);
            }
            return URLStatus.UNSTABLE_CONNECTION;
        }

        if (config.logInfo) {
            LOG.info("[OfflineSkins-Reloaded] HTTP response success: HTTP CODE {}", code);
        }
        return URLStatus.SUCCESS;
    }

    private static URLStatus evaluateRequests(RequestResult head, RequestResult get) {
        boolean headResponded = head.hasHttpResponse();
        boolean getResponded = get.hasHttpResponse();

        if (!headResponded && !getResponded) {
            ConfigOptions config =  FabricOfflineSkinsReloaded.getRuntimeConfig();
            if (head.transportResult() == URLStatus.DOMAIN_NOT_FOUND || get.transportResult() == URLStatus.DOMAIN_NOT_FOUND) {
                if (config.logInfo) {
                    LOG.warn("[OfflineSkins-Reloaded] Domain not found: HEAD {}, GET {}", head.transportResult(), get.transportResult());
                }
                return URLStatus.DOMAIN_NOT_FOUND;
            }

            return URLStatus.OFFLINE;
        }

        boolean headUnsupported = headResponded && isMethodUnsupported(head.code());
        boolean getUnsupported = getResponded && isMethodUnsupported(get.code());

        /*
         * GET is the primary method.
         */
        if (getResponded && !getUnsupported) {
            return evaluateGet(get);
        }

        /*
         * GET is unavailable, so HEAD is the only usable method.
         */
        if (headResponded && !headUnsupported) {
            return evaluateHead(head);
        }

        return URLStatus.NO_RESPONSE;
    }

    private static URLStatus evaluateGet(RequestResult get) {
        int code = get.code();

        if (isAccessDenied(code)) {
            return URLStatus.NO_ACCESS;
        }

        if (isServerError(code)) {
            return URLStatus.NO_RESPONSE;
        }

        if (get.isUnstable()) {
            return URLStatus.UNSTABLE_CONNECTION;
        }

        return URLStatus.SUCCESS;
    }

    private static URLStatus evaluateHead(RequestResult head) {
        if (!head.hasHttpResponse()) {
            return head.transportResult();
        }

        int code = head.code();

        if (isAccessDenied(code)) {
            return URLStatus.NO_ACCESS;
        }

        if (isServerError(code)) {
            return URLStatus.NO_RESPONSE;
        }

        if (isMethodUnsupported(code)) {
            return URLStatus.NO_RESPONSE;
        }

        if (head.isUnstable()) {
            return URLStatus.UNSTABLE_CONNECTION;
        }

        return URLStatus.SUCCESS;
    }

    private static boolean isMethodUnsupported(int code) {
        return code == HttpURLConnection.HTTP_BAD_METHOD || code == HttpURLConnection.HTTP_NOT_IMPLEMENTED;
    }

    private static boolean isAccessDenied(int code) {
        return code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN;
    }

    private static boolean isServerError(int code) {
        return code >= 500 && code < 600;
    }

    private static long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private record RequestResult(
            Integer code,
            long elapsedMillis,
            URLStatus transportResult
    ) {
        boolean hasHttpResponse() {
            return code != null;
        }

        boolean isUnstable() {
            return hasHttpResponse()
                    && elapsedMillis >= UNSTABLE_THRESHOLD_MS
                    && elapsedMillis < READ_TIMEOUT_MS;
        }
    }
}