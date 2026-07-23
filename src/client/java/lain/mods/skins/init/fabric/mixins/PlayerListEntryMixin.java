package lain.mods.skins.init.fabric.mixins;

import com.mojang.authlib.GameProfile;
import lain.mods.skins.impl.fabric.SkinUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Shadow(remap = false)
    public abstract GameProfile method_2966();

    @Inject(method = "method_52810", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void offlineskins$getSkinTextures(CallbackInfoReturnable<SkinTextures> info) {
        SkinTextures textures = SkinUtils.textures(method_2966());
        if (textures != null) {
            info.setReturnValue(textures);
        }
    }
}
