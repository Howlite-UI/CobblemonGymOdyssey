package com.howlite.fabric.mixin;

import com.howlite.client.screen.InventoryWalletButton;
import com.howlite.client.screen.WalletOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen {

    protected CreativeModeInventoryScreenMixin(net.minecraft.world.inventory.AbstractContainerMenu menu,
                                               net.minecraft.world.entity.player.Inventory inv,
                                               net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int btnX = this.leftPos + this.imageWidth;
        int btnY = this.topPos + 16;
        this.addRenderableWidget(new InventoryWalletButton(btnX, btnY, 8, 20));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (((CreativeModeInventoryScreen)(Object)this).isInventoryOpen()) {
            WalletOverlay.INSTANCE.render(graphics, this.leftPos, this.topPos, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (((CreativeModeInventoryScreen)(Object)this).isInventoryOpen()) {
            if (WalletOverlay.INSTANCE.mouseClicked(this.leftPos, this.topPos, mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (((CreativeModeInventoryScreen)(Object)this).isInventoryOpen()) {
            if (WalletOverlay.INSTANCE.isHovering(this.leftPos, this.topPos, mouseX, mouseY)) {
                cir.setReturnValue(true);
            }
        }
    }
}
