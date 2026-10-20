package vorez.mods.skins.init.fabric.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vorez.mods.skins.impl.Utils.SkinUtils;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void offlineSkinsGetSkinTextures(CallbackInfoReturnable<PlayerSkin> info) {
        PlayerSkin custom = SkinUtils.textures(getProfile());

        if (custom != null) {
            info.setReturnValue(custom);
        }
    }
}