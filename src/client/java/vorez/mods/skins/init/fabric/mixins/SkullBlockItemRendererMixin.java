package vorez.mods.skins.init.fabric.mixins;

import com.mojang.authlib.GameProfile;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinRenderCache.class)
public abstract class SkullBlockItemRendererMixin {

    @Inject(
            method = "getOrDefault",
            at = @At("HEAD"),
            cancellable = true
    )
    private void offlineskins$useCustomPlayerHeadSkin(
            ResolvableProfile resolvableProfile,
            CallbackInfoReturnable<PlayerSkinRenderCache.RenderInfo> cir
    ) {
        if (!FabricOfflineSkinsReloaded.PLAYERHEADS) {
            return;
        }

        GameProfile profile = resolvableProfile.partialProfile();

        Identifier loc = FabricOfflineSkinsReloaded.getLocationSkin(
                profile,
                null
        );

        if (loc == null) {
            return;
        }

        String type = FabricOfflineSkinsReloaded.getSkinType(
                profile,
                null
        );

        PlayerModelType model = PlayerModelType.byLegacyServicesName(type);

        PlayerSkin playerSkin = PlayerSkin.insecure(
                new ClientAsset.DownloadedTexture(loc, loc.toString()),
                null,
                null,
                model
        );

        PlayerSkinRenderCache cache = (PlayerSkinRenderCache)(Object)this;

        cir.setReturnValue(
                cache.new RenderInfo(
                        profile,
                        playerSkin,
                        resolvableProfile.skinPatch()
                )
        );
    }
}