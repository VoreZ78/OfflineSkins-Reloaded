package vorez.mods.skins.init.fabric.mixins;

import com.mojang.authlib.GameProfile;
import vorez.mods.skins.impl.fabric.SkinUtils;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class) // В MojMap класс называется PlayerInfo
public abstract class PlayerListEntryMixin {

    // В MojMap метод получения профиля называется getProfile
    @Shadow
    public abstract GameProfile getProfile();

    // Перехватываем официальный метод getSkin() вместо обфусцированного method_52810
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void offlineskins$getSkinTextures(CallbackInfoReturnable<PlayerSkin> info) {
        // Убедитесь, что ваш SkinUtils.textures возвращает актуальный PlayerSkin
        PlayerSkin textures = SkinUtils.textures(getProfile());
        if (textures != null) {
            info.setReturnValue(textures);
        }
    }
}
