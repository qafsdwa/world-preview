package caeruleusTait.world.preview.client.gui.widgets.lists;

import caeruleusTait.world.preview.client.gui.screens.PreviewContainer;
import caeruleusTait.world.preview.client.gui.widgets.OldStyleImageButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import static caeruleusTait.world.preview.client.gui.screens.PreviewContainer.BUTTONS_TEXTURE;
import static caeruleusTait.world.preview.client.gui.screens.PreviewContainer.BUTTONS_TEX_HEIGHT;
import static caeruleusTait.world.preview.client.gui.screens.PreviewContainer.BUTTONS_TEX_WIDTH;

public class SeedsList extends BaseObjectSelectionList<SeedsList.SeedEntry> {
    private final PreviewContainer previewContainer;
    private final boolean seedCanChange;

    public SeedsList(Minecraft minecraft, PreviewContainer previewContainer) {
        super(minecraft, 100, 100, 0, 0, 24);
        this.previewContainer = previewContainer;
        this.seedCanChange = this.previewContainer.dataProvider().seedIsEditable();
    }

    public SeedEntry createEntry(String seed) {
        return new SeedEntry(this, seed);
    }

    public class SeedEntry extends BaseObjectSelectionList.Entry<SeedEntry> {
        public final SeedsList seedsList;
        public final String seed;
        public final Button moveUpButton;
        public final Button moveDownButton;
        public final Button deleteButton;
        /**
         * Left edge of the button cluster, clicks left of it select this seed
         */
        private final int buttonsLeftInset = 22 + 15 + 15;

        public SeedEntry(SeedsList seedsList, String seed) {
            this.seedsList = seedsList;
            this.seed = seed;
            this.moveUpButton = Button
                    .builder(Component.literal("↑"), x -> seedsList.previewContainer.moveSeed(seed, -1))
                    .size(14, 20)
                    .build();
            this.moveUpButton.setTooltip(Tooltip.create(Component.translatable("world_preview.preview.btn-move-up")));
            this.moveDownButton = Button
                    .builder(Component.literal("↓"), x -> seedsList.previewContainer.moveSeed(seed, +1))
                    .size(14, 20)
                    .build();
            this.moveDownButton.setTooltip(Tooltip.create(Component.translatable("world_preview.preview.btn-move-down")));
            this.deleteButton = new OldStyleImageButton(
                    0, 0, 20, 20, /* x, y, width, height */
                    40, 20, 20, /* xTexStart, yTexStart, yDiffTex */
                    BUTTONS_TEXTURE, BUTTONS_TEX_WIDTH, BUTTONS_TEX_HEIGHT, /* resourceLocation, textureWidth, textureHeight*/
                    this::deleteEntry
            );
            this.moveUpButton.active = seedCanChange;
            this.moveDownButton.active = seedCanChange;
            this.deleteButton.active = seedCanChange;
        }

        private void deleteEntry(Button btn) {
            seedsList.previewContainer.deleteSeed(seed);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.translatable("narrator.select", this.seed);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            guiGraphics.text(seedsList.minecraft.font, seed, getX() + 4, getY() + 6, seedCanChange ? 0xFFFFFFFF : 0xFF999999);
            moveDownButton.setPosition(seedsList.getRowRight() - 22 - 15, getY());
            moveUpButton.setPosition(moveDownButton.getX() - 15, getY());
            deleteButton.setPosition(seedsList.getRowRight() - 22, getY());
            moveUpButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            moveDownButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            deleteButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (!seedCanChange) {
                return true;
            }
            if (moveUpButton.isMouseOver(event.x(), event.y())) {
                moveUpButton.mouseClicked(event, doubleClick);
            }
            if (moveDownButton.isMouseOver(event.x(), event.y())) {
                moveDownButton.mouseClicked(event, doubleClick);
            }
            if (deleteButton.isMouseOver(event.x(), event.y())) {
                deleteButton.mouseClicked(event, doubleClick);
            }
            if (event.button() == 0 && event.x() < seedsList.getRowRight() - buttonsLeftInset) {
                seedsList.setSelected(this);
                seedsList.previewContainer.setSeed(seed);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            } else {
                return false;
            }
        }
    }
}
