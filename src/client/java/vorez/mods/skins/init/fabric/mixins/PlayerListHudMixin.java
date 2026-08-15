package vorez.mods.skins.init.fabric.mixins;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerListHudMixin {

    @ModifyVariable(method = "render", at = @At(value = "STORE", opcode = Opcodes.ISTORE, ordinal = 0))
    private boolean offlineskins$forceFlag(boolean result) {
        return FabricOfflineSkinsReloaded.PLAYERHEADS ? true : result;
    }

}
