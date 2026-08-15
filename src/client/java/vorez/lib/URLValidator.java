package vorez.lib;

import vorez.mods.skins.impl.specifications.URLCheck;
import net.minecraft.network.chat.Component;
import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public final class URLValidator {

    private static String lastCheckedUrl = "";
    private static boolean lastRequireAuto = false;
    private static URLCheck lastResult = URLCheck.INVALID_URL;
    private static long lastCheckTime = 0;

    private URLValidator() {
    }

    public static URLCheck validate(String url, boolean requireAuto) {
        if (url == null || url.isBlank()) {
            return URLCheck.INVALID_URL;
        }
        long currentTime = System.currentTimeMillis();

        if (url.equals(lastCheckedUrl)
                && requireAuto == lastRequireAuto
                && currentTime - lastCheckTime < 150) {
            return lastResult;
        }
        lastCheckedUrl = url;
        lastRequireAuto = requireAuto;
        lastCheckTime = currentTime;

        if (requireAuto && !url.contains("%auto%")) {
            lastResult = URLCheck.FAIL;
            return lastResult;
        }
        try {
            String parsed = requireAuto
                    ? url.replace("%auto%", "testplayer")
                    : url;

            URI uri = new URI(parsed);
            String scheme = uri.getScheme();

            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http")
                            && !scheme.equalsIgnoreCase("https"))) {

                lastResult = URLCheck.INVALID_URL;
                return lastResult;
            }

            if (scheme.equalsIgnoreCase("http")) {
                ConfigOptions config = FabricOfflineSkinsReloaded.loadConfigSnapshot();

                if (config != null && !config.allowHTTP) {
                    lastResult = URLCheck.HTTP_DENIED;
                    return lastResult;
                }
            }

            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                lastResult = URLCheck.INVALID_URL;
                return lastResult;
            }
            if (host.equalsIgnoreCase("example.com")
                    || host.endsWith(".example.com")) {

                lastResult = URLCheck.IS_EXAMPLE_COM;
                return lastResult;
            }
            lastResult = URLCheck.SUCCESS;
            return lastResult;

        } catch (URISyntaxException e) {

            lastResult = URLCheck.INVALID_URL;
            return lastResult;
        }
    }
    public static Optional<Component> getError(URLCheck check) {
        return switch (check) {
            case HTTP_DENIED ->
                    Optional.of(Component.translatable("use.of.http.is.denied"));

            case CUSTOM_SERVER_DISABLE ->
                    Optional.of(Component.translatable("custom.server.disabled"));

            case IS_EXAMPLE_COM ->
                    Optional.of(Component.translatable("is.example.com"));

            case SUCCESS,
                 STABLE_CONNECTION ->
                    Optional.empty();

            case FAIL ->
                    Optional.of(Component.translatable("error.fail"));

            case OFFLINE ->
                    Optional.of(Component.translatable("error.offline"));

            case NO_RESPONSE ->
                    Optional.of(Component.translatable("error.no.response"));

            case INVALID_URL ->
                    Optional.of(Component.translatable("error.invalid.url"));

            case ERROR_404 ->
                    Optional.of(Component.translatable("error.404"));

            case UNSTABLE_CONNECTION ->
                    Optional.of(Component.translatable("error.unstable.connection"));

            case INVALID_SKIN ->
                    Optional.of(Component.translatable("error.invalid.skin"));

            case INVALID_CAPE ->
                    Optional.of(Component.translatable("error.invalid.cape"));
        };
    }
}