package vorez.mods.skins.init.fabric.mixins;

import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
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

    @Inject(method = "renderSkull", at = @At("RETURN"), cancellable = true, require = 1)
    private static void offlineskins$renderSkull(SkullBlock.SkullType type, SkullBlockEntity blockEntity, CallbackInfoReturnable<RenderLayer> info) {
        if (FabricOfflineSkinsReloaded.PLAYERHEADS && type == SkullBlock.Type.PLAYER) {
            ProfileComponent profile = blockEntity.getOwner();
            if (profile != null && profile.getGameProfile() != null) {
                Identifier loc = FabricOfflineSkinsReloaded.getLocationSkin(profile.getGameProfile(), null);
                if (loc != null) {
                    info.setReturnValue(SkullBlockEntityRenderer.getTranslucentRenderLayer(loc));
                }
            }
        }
    }
}
