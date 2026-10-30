package vorez.mods.skins.init.forge.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vorez.mods.skins.init.forge.ForgeOfflineSkinsReloaded;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private static RenderType offlineSkinsResolveSkullRenderType(
            RenderType original,
            SkullBlock.Type type,
            GameProfile gameProfile
    ) {
        if (type != SkullBlock.Types.PLAYER || gameProfile == null) {
            return original;
        }

        ResourceLocation loc = ForgeOfflineSkinsReloaded.getUnofficialLocationSkin(gameProfile);

        if (loc != null) {
            return RenderType.entityTranslucent(loc);
        }

        return original;
    }
}
