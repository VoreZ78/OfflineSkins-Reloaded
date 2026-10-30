package vorez.mods.skins.init.fabric.mixins;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerListHudMixin {

    @ModifyVariable(method = "extractRenderState", at = @At(value = "STORE", ordinal = 0), name = "showHead")
    private boolean offlineSkinsForceFlag(boolean showHead) {
        return FabricOfflineSkinsReloaded.PLAYERHEADS;
    }
}
