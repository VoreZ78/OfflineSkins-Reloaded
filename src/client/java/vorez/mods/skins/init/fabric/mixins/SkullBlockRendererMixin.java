package vorez.mods.skins.init.fabric.mixins;

import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SkullBlock.Types;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {

    @Inject(method = "resolveSkullRenderType", at = @At("RETURN"), cancellable = true, require = 1)
    private void offlineskins$resolveSkullRenderType(
            SkullBlock.Type type,
            SkullBlockEntity entity,
            CallbackInfoReturnable<RenderType> cir
    ) {
        if (!FabricOfflineSkinsReloaded.PLAYERHEADS || type != Types.PLAYER) {
            return;
        }

        ResolvableProfile ownerProfile = entity.getOwnerProfile();
        if (ownerProfile == null) {
            return;
        }

        Identifier loc = FabricOfflineSkinsReloaded.getLocationSkin(ownerProfile.partialProfile(), null);
        if (loc != null) {
            cir.setReturnValue(RenderTypes.entityTranslucent(loc));
        }
    }
}
