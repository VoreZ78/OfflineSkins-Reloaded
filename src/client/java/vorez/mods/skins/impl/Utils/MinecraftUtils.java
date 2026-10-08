package vorez.mods.skins.impl.Utils;

import com.mojang.authlib.minecraft.SessionService;
import net.minecraft.client.Minecraft;

import java.net.Proxy;

public class MinecraftUtils {

    public static Proxy getProxy() {
        return Minecraft.getInstance().getProxy();
    }

    public static SessionService getSessionService() {
        return Minecraft.getInstance().services().sessionService();
    }
}