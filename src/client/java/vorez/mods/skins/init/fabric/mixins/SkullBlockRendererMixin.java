package vorez.mods.skins.init.fabric.mixins;

import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {

    @Inject(
            method = "getRenderType",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void offlineskins$resolveSkullRenderType(
            SkullBlock.Type type,
            ResolvableProfile resolvableProfile,
            CallbackInfoReturnable<RenderType> cir
    ) {
        if (!FabricOfflineSkinsReloaded.PLAYERHEADS || type != SkullBlock.Types.PLAYER) {
            return;
        }

        if (resolvableProfile == null) {
            return;
        }

        ResourceLocation loc = FabricOfflineSkinsReloaded.getLocationSkin(
                resolvableProfile.gameProfile(),
                null
        );

        if (loc != null) {
            cir.setReturnValue(RenderType.entityTranslucent(loc));
        }
    }
}