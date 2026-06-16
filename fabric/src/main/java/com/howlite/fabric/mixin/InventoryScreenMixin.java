package com.howlite.fabric.mixin;

import com.howlite.client.screen.InventoryWalletButton;
import com.howlite.client.screen.WalletOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin sur InventoryScreen (Fabric) pour :
 * 1. Injecter le bouton wallet dans la zone à droite du slot de résultat de craft.
 * 2. Rendre l'overlay wallet par-dessus l'inventaire.
 * 3. Intercepter les clics souris pour l'overlay.
 *
 * Position du bouton : à droite du slot résultat de craft (index 0 de crafting output),
 * entre ce slot et l'inventaire principal.
 * En vanilla, le slot résultat craft est à (guiLeft+154, guiTop+28).
 * On place le bouton à (guiLeft+161, guiTop+44) — à droite et légèrement plus bas.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<net.minecraft.world.inventory.InventoryMenu> {

    protected InventoryScreenMixin(net.minecraft.world.inventory.InventoryMenu menu,
                                   net.minecraft.world.entity.player.Inventory inv,
                                   net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Bouton wallet : à droite du slot craft result, entre craft et inventaire
        // guiLeft + 154 (craft result x) + 18 + 2 (margin) = guiLeft + 174
        // guiTop + 28 (craft result y) + 8 = guiTop + 36
        int btnX = this.leftPos + 161;
        int btnY = this.topPos + 44;
        this.addRenderableWidget(new InventoryWalletButton(btnX, btnY, 12, 12));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        WalletOverlay.INSTANCE.render(graphics, this.leftPos, this.topPos, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (WalletOverlay.INSTANCE.mouseClicked(this.leftPos, this.topPos, mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }
}
