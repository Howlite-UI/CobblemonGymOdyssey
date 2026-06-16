package com.howlite.client;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class JohtoShopBackButton extends AbstractButton {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "cobblemongymodyssey",
        "textures/gui/back_button.png"
    );
    private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "cobblemongymodyssey",
        "textures/gui/back_button_icon.png"
    );

    private final Runnable onPress;

    public JohtoShopBackButton(int x, int y, Runnable onPress) {
        super(x, y, 26, 13, Component.empty());
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (this.onPress != null) {
            this.onPress.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean isHovered = this.isHoveredOrFocused();
        float vOffset = isHovered ? 13.0f : 0.0f;
        
        // Draw the tab button background (26x13)
        graphics.blit(TEXTURE, this.getX(), this.getY(), 0.0f, vOffset, 26, 13, 26, 26);
        
        // Draw the icon centered inside the tab
        // Tab is 26 wide, 13 high. Icon is 21 wide, 11 high.
        graphics.blit(ICON_TEXTURE, this.getX() + 2, this.getY() + 1, 0.0f, 0.0f, 21, 11, 21, 11);
    }
}
