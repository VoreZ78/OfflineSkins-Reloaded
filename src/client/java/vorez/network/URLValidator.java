package vorez.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.impl.CustomServersList.CustomServersList;
import vorez.mods.skins.impl.YaclSettings;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.net.URI;
import java.net.URISyntaxException;

public final class URLValidator {

    private static String lastCheckedUrl = "";
    private static boolean lastRequireAuto = false;
    private static URLStatus lastResult = URLStatus.INVALID_URL;
    private static long lastCheckTime = 0;

    private URLValidator() {
    }

    public static URLStatus validate(String url, boolean requireAuto) {
        ConfigOptions config = FabricOfflineSkinsReloaded.getRuntimeConfig();

        if (url == null || url.isBlank()) {
            return URLStatus.URL_EMPTY;
        }

        if (url.matches(".*[\\p{L}&&[^a-zA-Z]].*")) {
            return URLStatus.INVALID_URL;
        }

        String skinUrl = YaclSettings.getSkinUrl();
        String capeUrl = YaclSettings.getCapeUrl();
        CustomServersList preset = YaclSettings.getCurrentPreset();

        if (preset == CustomServersList.GITHUB && (skinUrl.startsWith("http") || capeUrl.startsWith("http"))) {
            return URLStatus.IS_RAW_GITHUB;
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
            lastResult = URLStatus.FAIL;
            return lastResult;
        }

        try {
            String parsed = requireAuto
                    ? url.replace("%auto%", "testplyaer.png")
                    : url;

            URI uri = new URI(parsed);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                lastResult = URLStatus.INVALID_URL;
                return lastResult;
            }

            if (scheme.equalsIgnoreCase("http")) {
                if (config != null && !config.allowHTTP) {
                    lastResult = URLStatus.HTTP_DENIED;
                    return lastResult;
                }
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                lastResult = URLStatus.INVALID_URL;
                return lastResult;
            }

            if (host.equalsIgnoreCase("example.com") || host.endsWith(".example.com")) {
                lastResult = URLStatus.IS_EXAMPLE_COM;
                return lastResult;
            }

            lastResult = URLStatus.SUCCESS;

            return lastResult;

        } catch (URISyntaxException e) {
            lastResult = URLStatus.INVALID_URL;
            return lastResult;
        }
    }

    public static void showCheckResult(String title, URLStatus result) {
        Minecraft client = Minecraft.getInstance();

        Component message = switch (result) {
            case NO_INTERNET ->
                    Component.translatable("no.internet");

            case HTTP_DENIED ->
                    Component.translatable("use.of.http.is.denied");

            case CUSTOM_SERVER_DISABLED ->
                    Component.translatable("error.custom-server-disabled");

            case IS_EXAMPLE_COM ->
                    Component.translatable("toast.offlineskins.example");

            case IS_RAW_GITHUB ->
                    Component.translatable("toast.offlineskins.raw.githubuser");

            case URL_EMPTY ->
                    Component.translatable("toast.offlineskins.url.empty");

            case SUCCESS ->
                    Component.translatable("toast.offlineskins.success");

            case UNSTABLE_CONNECTION ->
                    Component.translatable("toast.offlineskins.unstable");

            case NO_ACCESS ->
                    Component.translatable("toast.offlineskins.no-access");

            case FAIL ->
                    Component.translatable("toast.offlineskins.fail");

            case INVALID_URL ->
                    Component.translatable("toast.offlineskins.invalid_url");

            case ERROR_404 ->
                    Component.translatable("toast.offlineskins.error404");

            case OFFLINE ->
                    Component.translatable("toast.offlineskins.offline");

            case NO_RESPONSE ->
                    Component.translatable("toast.offlineskins.no_response");

            case DOMAIN_NOT_FOUND ->
                    Component.translatable("toast.offlineskins.domain-not-found");
        };

        SystemToast.add(
                client.gui.toastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal(title),
                message
        );
    }
}