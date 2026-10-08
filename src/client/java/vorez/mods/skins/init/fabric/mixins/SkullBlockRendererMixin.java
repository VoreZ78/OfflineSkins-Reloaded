package vorez.mods.skins.init.fabric.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {

    @ModifyReturnValue(method = "resolveSkullRenderType", at = @At("RETURN"))
    private static RenderType offlineSkinsResolveSkullRenderType(
            RenderType original,
            SkullBlock.Type type,
            SkullBlockEntity blockEntity
    ) {
        if (type != SkullBlock.Types.PLAYER) {
            return original;
        }

        ResolvableProfile resolvableProfile = blockEntity.getOwnerProfile();

        if (resolvableProfile == null) {
            return original;
        }

        Identifier loc = FabricOfflineSkinsReloaded.getUnofficialLocationSkin(
                resolvableProfile.partialProfile()
        );

        if (loc != null) {
            return RenderTypes.entityTranslucent(loc);
        }

        return original;
    }
}