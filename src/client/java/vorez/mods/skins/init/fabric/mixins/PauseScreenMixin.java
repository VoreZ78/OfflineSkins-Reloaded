package vorez.mods.skins.init.fabric.mixins;

import vorez.mods.skins.impl.YaclSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    private static final Identifier BUTTON_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "offlineskins-reloaded",
                    "mainTitleScreenButton.png"
            );

    private static final Identifier BUTTON_HIGHLIGHT_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "offlineskins-reloaded",
                    "mainTitleScreenButtonHighlighting.png"
            );

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void vorez$addConfigButton(CallbackInfo ci) {
        if (this.minecraft == null) {
            return;
        }

        int x = this.width / 2 - 15;
        int y = this.height / 4 - 30;

        this.addRenderableWidget(
                new ConfigButton(
                        x,
                        y,
                        button -> this.minecraft.gui.setScreen(
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
        protected void extractContents(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            Identifier texture = this.isHovered()
                    ? BUTTON_HIGHLIGHT_TEXTURE
                    : BUTTON_TEXTURE;

            int alpha = ARGB.as8BitChannel(this.getAlpha());
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