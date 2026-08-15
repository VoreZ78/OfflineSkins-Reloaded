package vorez.mods.skins.impl.specifications;

import net.minecraft.network.chat.Component;

public enum CustomServersList {
    ELY_BY("Ely.by", "http://skinsystem.ely.by/skins/%auto%.png", "http://skinsystem.ely.by/cloaks/%auto%.png"),
    CUSTOM("custom.url", "", "");

    private final String displayName;
    private final String skinUrl;
    private final String capeUrl;

    CustomServersList(String displayName, String skinUrl, String capeUrl) {
        this.displayName = displayName;
        this.skinUrl = skinUrl;
        this.capeUrl = capeUrl;
    }

    @Override
    public String toString() {
        return Component.translatable(displayName).getString();
    }

    public String getSkinUrl() {
        return this.skinUrl;
    }

    public String getCapeUrl() {
        return this.capeUrl;
    }

}