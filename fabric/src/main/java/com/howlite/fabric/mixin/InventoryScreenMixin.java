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

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<net.minecraft.world.inventory.InventoryMenu> {

    protected InventoryScreenMixin(net.minecraft.world.inventory.InventoryMenu menu,
                                   net.minecraft.world.entity.player.Inventory inv,
                                   net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int btnX = this.leftPos + 161;
        int btnY = this.topPos + 44;
        this.addRenderableWidget(new InventoryWalletButton(btnX, btnY, 14, 17));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        WalletOverlay.INSTANCE.render(graphics, this.leftPos, this.topPos, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (WalletOverlay.INSTANCE.mouseClicked(this.leftPos, this.topPos, mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (WalletOverlay.INSTANCE.isHovering(this.leftPos, this.topPos, mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(net.minecraft.world.inventory.Slot slot, int slotId, int mouseButton, net.minecraft.world.inventory.ClickType clickType, CallbackInfo ci) {
        if (WalletOverlay.INSTANCE.isOpen() && clickType == net.minecraft.world.inventory.ClickType.QUICK_MOVE) {
            if (slot != null && slot.hasItem()) {
                net.minecraft.world.item.ItemStack stack = slot.getItem();
                if (isCoin(stack.getItem())) {
                    com.howlite.wallet.WalletNetwork.INSTANCE.sendDepositSlot(slotId);
                    ci.cancel();
                }
            }
        }
    }

    private boolean isCoin(net.minecraft.world.item.Item item) {
        return item == com.howlite.items.CobbleCoins.INSTANCE.getCOBBLE_COPPER_COIN().get() ||
               item == com.howlite.items.CobbleCoins.INSTANCE.getCOBBLE_SILVER_COIN().get() ||
               item == com.howlite.items.CobbleCoins.INSTANCE.getCOBBLE_GOLD_COIN().get() ||
               item == com.howlite.items.CobbleCoins.INSTANCE.getCOBBLE_PLATINUM_COIN().get();
    }
}
