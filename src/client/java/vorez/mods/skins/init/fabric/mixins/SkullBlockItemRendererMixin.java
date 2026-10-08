package vorez.mods.skins.init.fabric.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

@Mixin(PlayerHeadSpecialRenderer.class)
public abstract class SkullBlockItemRendererMixin{

    @ModifyReturnValue(
            method = "extractArgument(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/renderer/special/PlayerHeadSpecialRenderer$PlayerHeadRenderInfo;",
            at = @At("RETURN")
    )
    private PlayerHeadSpecialRenderer.PlayerHeadRenderInfo offlineSkinsUseCustomPlayerHeadSkin(
            PlayerHeadSpecialRenderer.PlayerHeadRenderInfo original,
            ItemStack itemStack
    ) {
        ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);

        if (resolvableProfile == null) {
            return original;
        }

        ResourceLocation loc = FabricOfflineSkinsReloaded.getUnofficialLocationSkin(
                resolvableProfile.gameProfile()
        );

        if (loc != null) {
            return new PlayerHeadSpecialRenderer.PlayerHeadRenderInfo(
                    RenderType.entityTranslucent(loc)
            );
        }

        return original;
    }
}