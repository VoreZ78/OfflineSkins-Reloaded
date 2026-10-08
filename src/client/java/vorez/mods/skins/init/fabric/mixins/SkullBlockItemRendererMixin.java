package vorez.mods.skins.init.fabric.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

@Mixin(PlayerHeadSpecialRenderer.class)
public abstract class SkullBlockItemRendererMixin {

    @Final
    @Shadow
    private PlayerSkinRenderCache playerSkinRenderCache;

    @ModifyReturnValue(method = "extractArgument(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/renderer/PlayerSkinRenderCache$RenderInfo;", at = @At("RETURN"))
    private PlayerSkinRenderCache.RenderInfo offlineSkinsUseCustomPlayerHeadSkin(PlayerSkinRenderCache.RenderInfo original, ItemStack itemStack) {
        ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);

        if (resolvableProfile == null || original == null) {
            return original;
        }

        Identifier loc = FabricOfflineSkinsReloaded.getUnofficialLocationSkin(
                original.gameProfile()
        );

        if (loc == null) {
            return original;
        }

        PlayerSkin originalSkin = original.playerSkin();

        PlayerSkin playerSkin = PlayerSkin.insecure(
                new ClientAsset.DownloadedTexture(loc, loc.toString()),
                null,
                null,
                originalSkin.model()
        );

        return this.playerSkinRenderCache.new RenderInfo(
                original.gameProfile(),
                playerSkin,
                PlayerSkin.Patch.EMPTY
        );
    }
}