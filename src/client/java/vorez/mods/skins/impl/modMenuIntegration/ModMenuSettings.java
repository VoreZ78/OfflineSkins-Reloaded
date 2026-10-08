package vorez.mods.skins.impl.modMenuIntegration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import vorez.mods.skins.impl.YaclSettings;

public class ModMenuSettings implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return YaclSettings::createConfigScreen;
    }
}