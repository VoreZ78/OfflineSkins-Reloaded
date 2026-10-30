package vorez.mods.skins.impl;

import vorez.mods.skins.impl.CustomServersList.CustomServersList;

import java.util.Arrays;

public class ConfigOptions {
    public String HintDoNotMakeInvalid;

    public boolean useCachedSkin;
    public boolean useCachedCape;

    public boolean rememberSkin;
    public boolean rememberCape;

    public String HintSmartInternetCheck;
    public boolean smartInternetCheck;

    public String HintMojang;
    public boolean useMojang;

    public String HintCrafatar;
    public boolean useCrafatar;

    public String HintCustomServer;
    public boolean useCustomServer;

    public boolean allowHTTP;

    public CustomServersList customServersList;

    public String linkCustomServerSkin;
    public String linkCustomServerCape;

    public boolean allowHDPlayers;
    public String HintMaxHDResolution;
    public int maxHDResolution;

    public String HintDisablePlayerHeads;
    public boolean disablePlayerHeads;

    public int appearanceDelay;
    public boolean smoothInitialization;

    public String HintLogInfo;
    public boolean logInfo;

    public String[] copyLinks =  new String[10];

    public ConfigOptions defaultOptions() {
        HintDoNotMakeInvalid = "Warning: Manual editing requires valid JSON. If the file contains invalid JSON, the entire configuration will be reset to default values.";

        useCachedSkin = true;
        useCachedCape = true;

        rememberSkin = false;
        rememberCape = false;

        HintMojang = "Uses the official Minecraft skin and cape provider";
        useMojang = true;

        HintSmartInternetCheck = "Stops futile attempts to connect to image provider servers when there is no internet connection";
        smartInternetCheck = true;

        useCustomServer = false;
        HintMaxHDResolution = "The maximum resolutions of HD images from custom servers. 128 = 128x128, 256 = 256x256, 512 = 512x512, 1024 =  1024x1024, 2048 =  2048x2048, 4096 = 4096x4096. -1 = any";
        maxHDResolution = 128;

        allowHTTP = true;
        allowHDPlayers = false;

        customServersList = CustomServersList.CUSTOM;

        HintCustomServer = "Custom URLs for skins and capes";
        linkCustomServerSkin = "https://example.com/skins/%auto%";
        linkCustomServerCape = "https://example.com/capes/%auto%";

        HintDisablePlayerHeads = "Disables the heads in the tab menu";
        disablePlayerHeads = false;

        appearanceDelay = 1000;
        smoothInitialization = true;

        HintCrafatar = "Uses Crafatar as a fallback skin/cape provider";
        useCrafatar = false;

        HintLogInfo = "Displays warnings in the logs";
        logInfo = true;

        Arrays.fill(copyLinks, "");

        return this;
    }

    public boolean validate() {
        boolean any = false;

        if (copyLinks == null) {
            copyLinks = new String[10];
            Arrays.fill(copyLinks, "");
            any = true;
        } else {
            for (int i = 0; i < copyLinks.length; i++) {
                if (copyLinks[i] == null) {
                    copyLinks[i] = "";
                    any = true;
                }
            }
        }

        if (customServersList == null) {
            customServersList = CustomServersList.CUSTOM;
            any = true;
        }

        if (linkCustomServerSkin == null) {
            linkCustomServerSkin = "https://example.com/skins/%auto%";
            any = true;
        }

        if (linkCustomServerCape == null) {
            linkCustomServerCape = "https://example.com/capes/%auto%";
            any = true;
        }

        if (appearanceDelay < 0 || appearanceDelay > 3000) {
            appearanceDelay = Math.clamp(appearanceDelay, 0, 3000);
            any = true;
        }

        if (maxHDResolution != 128
                && maxHDResolution != 256
                && maxHDResolution != 512
                && maxHDResolution != 1024
                && maxHDResolution != 2048
                && maxHDResolution != 4096
                && maxHDResolution != -1) {

            maxHDResolution = 128;
            any = true;
        }
        
        return any;
    }

    public ConfigOptions hints() {
        HintDoNotMakeInvalid = "Warning: Manual editing requires valid JSON. If the file contains invalid JSON, the entire configuration will be reset to default values.";

        HintSmartInternetCheck = "Stops futile attempts to connect to image provider servers when there is no internet connection";

        HintMojang = "Uses the official Minecraft skin and cape provider";

        HintCrafatar = "Uses Crafatar as a fallback skin/cape provider";

        HintCustomServer = "Custom URLs for skins and capes";

        HintMaxHDResolution = "The maximum resolutions of HD images from custom servers. 128 = 128x128, 256 = 256x256, 512 = 512x512, 1024 = 1024x1024, 2048 = 2048x2048, 4096 = 4096x4096, -1 = any";

        HintDisablePlayerHeads = "Disables the heads in the tab menu";

        HintLogInfo = "Displays warnings in the logs";

        return this;
    }
}
