package vorez.mods.skins.init.fabric.mixins;

import com.mojang.authlib.GameProfile;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinRenderCache.RenderInfo.class)
public abstract class SkullBlockItemRenderedMixin {

    @Shadow
    private GameProfile gameProfile;

    @Inject(
            method = "renderType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void offlineskins$useCustomPlayerHeadSkin(
            CallbackInfoReturnable<RenderType> cir
    ) {
        if (!FabricOfflineSkinsReloaded.PLAYERHEADS) {
            return;
        }

        ResourceLocation loc = FabricOfflineSkinsReloaded.getLocationSkin(
                this.gameProfile,
                null
        );

        if (loc != null) {
            cir.setReturnValue(RenderType.entityTranslucent(loc));
        }
    }
}