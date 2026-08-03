package vorez.mods.skins.init.fabric.mixins;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerListHudMixin {

    @ModifyVariable(method = "extractRenderState", at = @At(value = "STORE", opcode = Opcodes.ISTORE, ordinal = 0), require = 1)
    private boolean offlineskins$forceFlag(boolean result) {
        return true;
    }

}
