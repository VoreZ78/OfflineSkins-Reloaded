package lain.mods.skins.init.fabric.mixins;

import net.minecraft.client.gui.hud.PlayerListHud;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @ModifyVariable(method = "method_1919", at = @At(value = "STORE", opcode = Opcodes.ISTORE, ordinal = 0), require = 0, remap = false)
    private boolean offlineskins$forceFlag(boolean result) {
        return true;
    }

}
