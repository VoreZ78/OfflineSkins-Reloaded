package vorez.mods.skins.init.fabric.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import vorez.mods.skins.impl.fabric.SkinUtils;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    private PlayerSkin offlineskins$getSkinTextures(
            PlayerSkin original
    ) {
        PlayerSkin textures = SkinUtils.textures(getProfile());

        if (textures != null) {
            return textures;
        }

        return original;
    }
}