package vorez.mods.skins.init.fabric.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

@Mixin(CustomHeadLayer.class)
public abstract class SkullBlockWornRendererMixin {
        @ModifyReturnValue(method = "resolveSkullRenderType", at = @At("RETURN"))
        private RenderType offlineSkinsWornPlayerHead(
                RenderType original,
                LivingEntityRenderState livingEntityRenderState,
                SkullBlock.Type type
        ) {
                if (type != SkullBlock.Types.PLAYER) {
                    return original;
                }

                ResolvableProfile resolvableProfile = livingEntityRenderState.wornHeadProfile;

                if (resolvableProfile == null) {
                    return original;
                }

                ResourceLocation loc = FabricOfflineSkinsReloaded.getUnofficialLocationSkin(
                    resolvableProfile.partialProfile()
                );

                if (loc != null) {
                    return RenderType.entityTranslucent(loc);
                }

                return original;
        }
}
