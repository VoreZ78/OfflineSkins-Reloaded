package vorez.mods.skins.init.fabric.mixins;

import com.mojang.authlib.GameProfile;
import vorez.mods.skins.impl.fabric.SkinUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true, require = 1)
    private void offlineskins$getSkinTextures(CallbackInfoReturnable<SkinTextures> info) {
        SkinTextures textures = SkinUtils.textures(getProfile());
        if (textures != null) {
            info.setReturnValue(textures);
        }
    }
}
