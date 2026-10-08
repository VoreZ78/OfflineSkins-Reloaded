package vorez.network;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public final class SmartInternetCheck {
    private static volatile boolean online;
    private static volatile boolean checked;
    private SmartInternetCheck() {
    }

    public static boolean isOnline() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static CompletableFuture<Boolean> check() {
        return CompletableFuture.supplyAsync(() -> {
            online = isOnline();
            checked = true;
            return online;
        });
    }

    public static boolean shouldBlockRequests() {
        return checked && !online;
    }

    public static boolean getStatus() {
        return online;
    }

    public static void reset() {
        online = true;
        checked = false;
    }
}