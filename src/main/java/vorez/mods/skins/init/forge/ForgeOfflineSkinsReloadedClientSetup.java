package vorez.mods.skins.init.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import vorez.mods.skins.impl.YaclSettings;

@Mod.EventBusSubscriber(modid = "offlineskins_reloaded", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeOfflineSkinsReloadedClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MinecraftForge.registerConfigScreen(
                        YaclSettings::createConfigScreen
                )
        );
    }

}