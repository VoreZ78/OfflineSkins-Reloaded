package vorez.mods.skins.impl.fabric;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;

import java.net.Proxy;

public class MinecraftUtils {

    public static Proxy getProxy() {
        return Minecraft.getInstance().getProxy();
    }

    public static MinecraftSessionService getSessionService() {
        YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(getProxy());
        return authService.createMinecraftSessionService();
    }
}