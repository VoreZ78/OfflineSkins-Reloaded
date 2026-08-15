package vorez.mods.skins.impl.modMenuIntegration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import vorez.mods.skins.impl.YaclSettings;

public class ModMenuSettings implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> YaclSettings.createConfigScreen(parentScreen);
    }
}