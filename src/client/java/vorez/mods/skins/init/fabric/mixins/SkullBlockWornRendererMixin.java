package vorez.mods.skins.init.fabric.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
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
                LivingEntityRenderState state,
                SkullBlock.Type type
        ) {
                if (type != SkullBlock.Types.PLAYER) {
                    return original;
                }

                ResolvableProfile resolvableProfile = state.wornHeadProfile;

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
