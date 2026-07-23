package lain.mods.skins.init.fabric.mixins;

import lain.mods.skins.init.fabric.FabricOfflineSkins;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockEntityRenderer.class)
public abstract class SkullBlockEntityRendererMixin {

    @Inject(method = "method_65832", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void offlineskins$getRenderLayer(SkullBlock.SkullType type, ProfileComponent profile, CallbackInfoReturnable<RenderLayer> info) {
        if (FabricOfflineSkins.PLAYERHEADS && type == SkullBlock.Type.PLAYER && profile != null && profile.gameProfile() != null) {
            Identifier loc = FabricOfflineSkins.getLocationSkin(profile.gameProfile(), null);
            if (loc != null) {
                info.setReturnValue(SkullBlockEntityRenderer.getTranslucentRenderLayer(loc));
            }
        }
    }
}
