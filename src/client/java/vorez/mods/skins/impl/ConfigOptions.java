package vorez.mods.skins.impl;

import com.sun.jna.platform.win32.ShTypes;

public class ConfigOptions {

    public String HintMojang;
    public boolean useMojang;
    public String HintCustomServer;
    public boolean useCustomServer;
    public String linkCustomServer;
    public String HintCustomServer2;
    public boolean useCustomServer2;
    public String linkCustomServer2Skin;
    public String linkCustomServer2Cape;
    public String HintDisablePlayerHeads;
    public boolean disablePlayerHeads;
    public String HintCrafatar;
    public boolean useCrafatar;
    /**
     * @return self with all options revert to default.
     */
    public ConfigOptions defaultOptions() {
        // Uses the official Minecraft skin and cape provider.
        HintMojang = "Uses the official Minecraft skin and cape provider";
        useMojang = true;

        // Custom servers, such as Ely.by, TLauncher, or others, can provide skins and capes.
        HintCustomServer = "Custom servers, such as Ely.by, TLauncher, or others, can provide skins and capes";
        useCustomServer = false;
        linkCustomServer = "http://example.com";

        useCustomServer2 = false;

        // Custom URLs for skins and capes.
        HintCustomServer2 = "Custom URLs for skins and capes";
        linkCustomServer2Skin = "http://example.com/skins/%auto%";
        linkCustomServer2Cape = "http://example.com/capes/%auto%";

        HintDisablePlayerHeads = "Disables the heads in the tab menu";
        disablePlayerHeads = false;

        // Uses Crafatar as a fallback skin/cape provider.
        HintCrafatar = "Uses Crafatar as a fallback skin/cape provider";
        useCrafatar = false;

        return this;
    }

    /**
     * @return true if changed.
     */
    public boolean validate() {
        boolean any = false;

        if (linkCustomServer == null) {
            linkCustomServer = "http://example.com";
            any = true;
        }
        if (linkCustomServer2Skin == null) {
            linkCustomServer2Skin = "http://example.com/skins/%auto%";
            any = true;
        }
        if (linkCustomServer2Cape == null) {
            linkCustomServer2Cape = "http://example.com/capes/%auto%";
            any = true;
        }

        return any;
    }

}
