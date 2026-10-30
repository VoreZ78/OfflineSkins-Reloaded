package vorez.mods.skins.init.neoforge.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vorez.mods.skins.init.neoforge.NeoForgeOfflineSkinsReloaded;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private static RenderType offlineSkinsResolveSkullRenderType(
            RenderType original,
            SkullBlock.Type type,
            ResolvableProfile profile
    ) {
        if (type != SkullBlock.Types.PLAYER || profile == null) {
            return original;
        }

        ResourceLocation loc = NeoForgeOfflineSkinsReloaded.getUnofficialLocationSkin(
                profile.gameProfile()
        );

        if (loc != null) {
            return RenderType.entityTranslucent(loc);
        }

        return original;
    }
}
