package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.menu.PlayerShopMenu
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.CoinType
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/**
 * Buyer-side screen for the Player Shop block.
 * Reuses the same visual style as GymShopScreen.
 */
class PlayerShopScreen(
    menu: PlayerShopMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<PlayerShopMenu>(menu, playerInventory, title) {

    private val BACKGROUND   = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/shop_background.png")
    private val ITEM_BG      = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/shop_item_background.png")
    private val SCROLLER_BG  = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/scroller_background.png")
    private val SCROLLER     = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/scroller.png")
    private val PREVIEW_BG   = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/item_preview_background.png")
    private val QTY_BG       = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/quantity_text_background.png")
    private val MINUS_BTN    = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/quantity_text_minus_button.png")
    private val PLUS_BTN     = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/quantity_text_plus_button.png")
    private val BUY_BTN      = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/buy_button.png")

    private var scrollProgress   = 0f
    private var isDragging       = false
    private var selectedIndex    = 0
    private var quantity         = 1

    init { imageWidth = 184; imageHeight = 180; inventoryLabelY = imageHeight - 94 }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) { /* suppressed */ }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos; val y = topPos
        graphics.blit(BACKGROUND, x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight)

        val offers = menu.offers
        // List with scissor
        graphics.enableScissor(x + 8, y + 1, x + 8 + 77, y + 1 + 80)
        val totalH = if (offers.isEmpty()) 0 else offers.size * 23 - 1
        val maxScrollY = (totalH - 80).coerceAtLeast(0)
        val scrollY = (scrollProgress * maxScrollY).toInt()

        for (i in offers.indices) {
            val offer = offers[i]
            val itemY = y + 2 + i * 23 - scrollY
            val vOff = if (i == selectedIndex) 22f else 0f
            graphics.blit(ITEM_BG, x + 8, itemY, 0f, vOff, 77, 22, 77, 44)
            graphics.renderItem(offer.resultItem, x + 11, itemY + 3)
            graphics.renderItemDecorations(font, offer.resultItem, x + 11, itemY + 3)
            // Cost icon
            val costIcon = if (offer.priceCCC > 0L) ItemStack(com.howlite.items.CobbleCoins.COBBLE_COPPER_COIN.get()) else offer.costItem
            graphics.renderItem(costIcon, x + 56, itemY + 3)
            // Price text
            val priceStr = if (offer.priceCCC > 0L) WalletOverlay.formatCompact(offer.priceCCC) else offer.costCount.toString()
            val canAfford = canAfford(offer, 1)
            val priceW = font.width(priceStr)
            graphics.drawString(font, priceStr, x + 54 - priceW, itemY + 7, if (canAfford) 0x3F3F3F else 0xFF5555, false)
        }
        graphics.disableScissor()

        // Scroller
        graphics.blit(SCROLLER_BG, x + 86, y + 1, 0f, 0f, 8, 81, 8, 81)
        val thumbY = y + 1 + (scrollProgress * (81 - 11)).toInt()
        graphics.blit(SCROLLER, x + 87, thumbY, 0f, 0f, 6, 11, 6, 11)

        // Preview + buy panel
        if (offers.isNotEmpty() && selectedIndex in offers.indices) {
            val offer = offers[selectedIndex]
            graphics.blit(PREVIEW_BG, x + 120, y + 6, 0f, 0f, 32, 32, 32, 32)
            graphics.renderItem(offer.resultItem, x + 128, y + 14)
            graphics.renderItemDecorations(font, offer.resultItem, x + 128, y + 14)

            graphics.blit(QTY_BG, x + 125, y + 43, 0f, 0f, 22, 12, 22, 12)
            val minusV = if (isHover(mouseX, mouseY, x + 114, y + 43, 11, 12)) 12f else 0f
            graphics.blit(MINUS_BTN, x + 114, y + 43, 0f, minusV, 11, 12, 11, 24)
            val plusV = if (isHover(mouseX, mouseY, x + 147, y + 43, 11, 12)) 12f else 0f
            graphics.blit(PLUS_BTN, x + 147, y + 43, 0f, plusV, 11, 12, 11, 24)
            val qtyStr = quantity.toString()
            graphics.drawString(font, qtyStr, x + 125 + (22 - font.width(qtyStr)) / 2, y + 45, 0xFFFFFF, false)

            val canBuy = canAfford(offer, quantity) && offer.availableStock >= quantity
            val buyV = if (canBuy && isHover(mouseX, mouseY, x + 100, y + 59, 73, 17)) 17f else 0f
            graphics.blit(BUY_BTN, x + 100, y + 59, 0f, buyV, 73, 17, 73, 34)
            val buyText = Component.translatable("cobblemongymodyssey.shop.buy").string
            graphics.drawString(font, buyText, x + 100 + (73 - font.width(buyText)) / 2, y + 59 + 4, if (canBuy) 0xFFFFFF else 0xAAAAAA, canBuy)

            // Stock label in preview panel
            val stockText = "Stock: ${offer.availableStock}"
            graphics.drawString(font, stockText, x + 100 + (73 - font.width(stockText)) / 2, y + 74, 0x555555, false)

            // Owner label
            val ownerText = "§7${menu.ownerName}"
            graphics.drawString(font, ownerText, x + 100 + (73 - font.width(ownerText)) / 2, y + 85, 0xFFFFFF, false)
        }

        // Balance
        val balText = WalletOverlay.formatCompact(ClientWalletCache.balance)
        graphics.drawString(font, balText, x + 92 - font.width(balText), y + 85, 0x3F3F3F, false)

        // Shop name header
        val nameText = menu.shopName
        graphics.drawString(font, nameText, x + (imageWidth - font.width(nameText)) / 2, y - 10, 0xFFFFFF, true)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = leftPos; val y = topPos

        // List click
        if (isHover(mouseX.toInt(), mouseY.toInt(), x + 8, y + 1, 77, 80)) {
            val totalH = if (menu.offers.isEmpty()) 0 else menu.offers.size * 23 - 1
            val maxScrollY = (totalH - 80).coerceAtLeast(0)
            val clickY = mouseY.toInt() - (y + 1) + (scrollProgress * maxScrollY).toInt()
            val idx = clickY / 23
            if (idx in menu.offers.indices && clickY % 23 < 22) {
                selectedIndex = idx; quantity = 1
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
        }

        if (isHover(mouseX.toInt(), mouseY.toInt(), x + 86, y + 1, 8, 81)) {
            isDragging = true; updateScroll(mouseY); return true
        }

        if (menu.offers.isNotEmpty() && selectedIndex in menu.offers.indices) {
            val offer = menu.offers[selectedIndex]

            if (isHover(mouseX.toInt(), mouseY.toInt(), x + 114, y + 43, 11, 12)) {
                val d = if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) 10 else 1
                quantity = (quantity - d).coerceAtLeast(1)
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
            if (isHover(mouseX.toInt(), mouseY.toInt(), x + 147, y + 43, 11, 12)) {
                val d = if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) 10 else 1
                quantity = (quantity + d).coerceAtMost(offer.availableStock.coerceAtLeast(1)).coerceAtMost(999)
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
            if (isHover(mouseX.toInt(), mouseY.toInt(), x + 100, y + 59, 73, 17)) {
                if (canAfford(offer, quantity) && offer.availableStock >= quantity) {
                    val id = selectedIndex * 1000 + (quantity - 1)
                    minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, id)
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f))
                } else {
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0f))
                }
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = false; return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dx: Double, dy: Double): Boolean {
        if (isDragging) { updateScroll(mouseY); return true }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, sx: Double, sy: Double): Boolean {
        if (menu.offers.size > 3) {
            val totalH = menu.offers.size * 23 - 1
            val max = (totalH - 80).coerceAtLeast(0)
            if (max > 0) {
                val cur = scrollProgress * max
                scrollProgress = ((cur - sy.toFloat() * 15f).coerceIn(0f, max.toFloat())) / max
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, sx, sy)
    }

    private fun updateScroll(my: Double) {
        val travel = 81 - 11
        scrollProgress = (((my - topPos - 1 - 5.5) / travel).coerceIn(0.0, 1.0)).toFloat()
    }

    private fun isHover(mx: Int, my: Int, rx: Int, ry: Int, rw: Int, rh: Int) =
        mx >= rx && mx < rx + rw && my >= ry && my < ry + rh

    private fun canAfford(offer: PlayerShopMenu.ShopOfferView, qty: Int): Boolean {
        if (offer.priceCCC > 0L) return ClientWalletCache.balance >= offer.priceCCC * qty
        // Barter: check inventory
        val item = offer.costItem.item
        var found = 0
        for (slot in menu.slots) if (slot.item.item == item) found += slot.item.count
        return found >= offer.costCount * qty
    }
}
