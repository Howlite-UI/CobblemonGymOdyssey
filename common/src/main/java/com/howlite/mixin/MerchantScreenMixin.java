package com.howlite.mixin;

import com.howlite.client.JohtoShopBackButton;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    public MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        boolean isJohtoShop = this.title != null && 
                             this.title.getContents() instanceof TranslatableContents &&
                             ((TranslatableContents) this.title.getContents()).getKey().equals("cobblemongymodyssey.shop.johto.title");

        if (isJohtoShop) {
            // Position the back button to stick out of the left side of the screen
            // MerchantScreen background width is 276.
            // X position: leftPos - 26
            // Y position: topPos + 10
            int buttonX = this.leftPos - 26;
            int buttonY = this.topPos + 10;

            this.addRenderableWidget(new JohtoShopBackButton(buttonX, buttonY, () -> {
                // Send C2S packet to open the Badge Case
                NetworkManager.sendToServer(
                    ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "open_badge_case"),
                    new net.minecraft.network.RegistryFriendlyByteBuf(
                        Unpooled.buffer(),
                        net.minecraft.client.Minecraft.getInstance().level.registryAccess()
                    )
                );
            }));
        }
    }
}
