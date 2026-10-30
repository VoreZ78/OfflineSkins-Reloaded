package vorez.mods.skins.init.neoforge.mixins;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vorez.mods.skins.init.neoforge.NeoForgeOfflineSkinsReloaded;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerListHudMixin {

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0))
    private boolean offlineSkinsForceFlag(boolean result) {
        return NeoForgeOfflineSkinsReloaded.PLAYERHEADS;
    }
}
