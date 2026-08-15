package vorez.mods.skins.init.fabric.mixins;

import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerHeadSpecialRenderer.class)
public abstract class SkullBlockItemRendererMixin {

    @Inject(
            method = "createAndCacheIfTextureIsUnpacked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void offlineskins$useCustomPlayerHeadSkin(
            ResolvableProfile resolvableProfile,
            CallbackInfoReturnable<PlayerHeadSpecialRenderer.PlayerHeadRenderInfo> cir
    ) {
        if (!FabricOfflineSkinsReloaded.PLAYERHEADS) {
            return;
        }

        ResourceLocation loc = FabricOfflineSkinsReloaded.getLocationSkin(
                resolvableProfile.gameProfile(),
                null
        );

        if (loc != null) {
            cir.setReturnValue(
                    new PlayerHeadSpecialRenderer.PlayerHeadRenderInfo(
                            RenderType.entityTranslucent(loc)
                    )
            );
            return;
        }

        cir.setReturnValue(null);
    }
}