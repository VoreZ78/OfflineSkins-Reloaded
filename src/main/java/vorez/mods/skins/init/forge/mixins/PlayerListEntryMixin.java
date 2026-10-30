package vorez.mods.skins.init.forge.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import vorez.mods.skins.impl.Utils.SkinUtils;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @ModifyReturnValue(method = "getSkinLocation", at = @At("RETURN"))
    private ResourceLocation offlineSkinsGetSkinLocation(ResourceLocation original) {
        ResourceLocation custom = SkinUtils.textures(getProfile());
        return custom != null ? custom : original;
    }

    @ModifyReturnValue(method = "getCapeLocation", at = @At("RETURN"))
    private ResourceLocation offlineSkinsGetCapeLocation(ResourceLocation original) {
        ResourceLocation custom = SkinUtils.capeTexture(getProfile());
        return custom != null ? custom : original;
    }

    @ModifyReturnValue(method = "getModelName", at = @At("RETURN"))
    private String offlineSkinsGetModelName(String original) {
        String custom = SkinUtils.model(getProfile());
        return custom != null ? custom : original;
    }
}