package vorez.mods.skins.init.fabric.mixins;

import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
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

    @Inject(method = "getRenderLayer", at = @At("RETURN"), cancellable = true, require = 1)
    private static void offlineskins$getRenderLayer(SkullBlock.SkullType type, ProfileComponent profile, CallbackInfoReturnable<RenderLayer> info) {
        if (FabricOfflineSkinsReloaded.PLAYERHEADS && type == SkullBlock.Type.PLAYER && profile != null && profile.gameProfile() != null) {
            Identifier loc = FabricOfflineSkinsReloaded.getLocationSkin(profile.gameProfile(), null);
            if (loc != null) {
                info.setReturnValue(SkullBlockEntityRenderer.getTranslucentRenderLayer(loc));
            }
        }
    }
}
