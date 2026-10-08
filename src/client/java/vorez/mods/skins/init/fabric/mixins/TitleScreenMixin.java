package vorez.mods.skins.init.fabric.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vorez.mods.skins.impl.YaclSettings;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private static final ResourceLocation BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "offlineskins-reloaded",
                    "maintitlescreenbutton.png"
            );

    @Unique
    private static final ResourceLocation BUTTON_HIGHLIGHT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "offlineskins-reloaded",
                    "maintitlescreenbuttonhighlighting.png"
            );

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void offlineSkinsTitleScreenButton(CallbackInfo ci) {
        if (this.minecraft == null) {
            return;
        }

        int x = this.width / 2 - 132;
        int y = this.height / 4 + 55;

        this.addRenderableWidget(
                new ConfigButton(
                        x,
                        y,
                        button -> this.minecraft.setScreen(
                                YaclSettings.createConfigScreen(this)
                        )
                )
        );
    }
    private static class ConfigButton extends Button {

        private ConfigButton(int x, int y, OnPress onPress) {
            super(
                    x,
                    y,
                    30,
                    30,
                    Component.empty(),
                    onPress,
                    DEFAULT_NARRATION
            );
        }
        @Override
        protected void renderWidget(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            ResourceLocation texture = this.isHovered()
                    ? BUTTON_HIGHLIGHT_TEXTURE
                    : BUTTON_TEXTURE;

            int alpha = ARGB.as8BitChannel(this.alpha);
            int color = ARGB.color(alpha, 255, 255, 255);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    this.getX(),
                    this.getY(),
                    0.0F,
                    0.0F,
                    30,
                    30,
                    100,
                    100,
                    100,
                    100,
                    color
            );
        }
    }
}