package caeruleusTait.world.preview.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PreviewCacheLoadingScreen extends Screen {
    protected PreviewCacheLoadingScreen(Component component) {
        super(component);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(Minecraft.getInstance().font, title, width / 2, height / 2, 0xFFFFFF);
    }
}
