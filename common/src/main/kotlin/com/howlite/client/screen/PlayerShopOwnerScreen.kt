package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.blocks.PlayerShopOffer
import com.howlite.menu.PlayerShopOwnerMenu
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class PlayerShopOwnerScreen(
    menu: PlayerShopOwnerMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<PlayerShopOwnerMenu>(menu, playerInventory, title) {

    private val BACKGROUND       = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_background.png")
    private val ADD_OFFER_BTN    = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_addoffer_button.png")
    private val CREATE_OFFER_BTN = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_createoffer_button.png")
    private val PLUS_BTN         = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_plus_button.png")
    private val MINUS_BTN        = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_minus_button.png")
    private val OFFER_CARD       = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_offer_card.png")
    private val SCROLLER_BG      = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_scrollbar.png")
    private val SCROLLER         = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_scroller.png")
    private val SLOT_BG          = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_slot.png")
    private val TEXTZONE         = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/playershop/playershop_editor_textzone.png")

    private var selectedOfferIdx = -1
    private var scroll = 0f
    private var isDragging = false

    private var editResult   = ItemStack.EMPTY
    private var editPriceCCC = 0L
    private var editCostItem = ItemStack.EMPTY
    private var editCostCount = 1

    private lateinit var priceField: EditBox
    private lateinit var costCountField: EditBox
    private lateinit var shopNameField: EditBox
    private lateinit var whitelistField: EditBox

    private var editorMode = EditorMode.COIN

    enum class EditorMode { COIN, BARTER }

    init {
        imageWidth  = 232
        imageHeight = 284
        inventoryLabelY = 192
    }

    override fun init() {
        super.init()

        val x = leftPos; val y = topPos

        // Shop name text box in the top-left of the top bar
        shopNameField = EditBox(font, x + 10, y + 3, 120, 9, Component.literal("Shop name"))
        shopNameField.setMaxLength(32)
        shopNameField.value = menu.shopName
        shopNameField.setBordered(false)
        addRenderableWidget(shopNameField)

        // Price field (shifted 20px right): 137 + 20 = 157
        priceField = EditBox(font, x + 157, y + 24, 52, 8, Component.literal("Price CCC"))
        priceField.setMaxLength(18)
        priceField.value = "0"
        priceField.setBordered(false)
        priceField.setFilter { it.all { c -> c.isDigit() } }
        addRenderableWidget(priceField)

        // Cost count (shifted 20px right): 137 + 20 = 157
        costCountField = EditBox(font, x + 157, y + 59, 26, 8, Component.literal("Count"))
        costCountField.setMaxLength(4)
        costCountField.value = "1"
        costCountField.setBordered(false)
        costCountField.setFilter { it.all { c -> c.isDigit() } }
        addRenderableWidget(costCountField)

        // Whitelist field inside the custom textzone background
        whitelistField = EditBox(font, x + 130, y + 89, 52, 8, Component.literal("Player name"))
        whitelistField.setMaxLength(16)
        whitelistField.setBordered(false)
        whitelistField.setHint(Component.literal("Trust"))
        addRenderableWidget(whitelistField)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Suppressed as requested
    }

    override fun renderBg(graphics: GuiGraphics, pt: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos; val y = topPos

        // Draw main custom background texture
        graphics.blit(BACKGROUND, x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight)

        // Rename Button (Just render the text, no blue rectangle background)
        val isRenameHover = isHover(mouseX, mouseY, x + 135, y + 2, 40, 11)
        val renameColor = if (isRenameHover) 0xFFFFFF else 0xAAAAAA
        graphics.drawString(font, "Rename", x + 135, y + 4, renameColor, false)

        // ── Left Panel: Offers List (shifted 10px right) ──
        // x + 14 + 10 = x + 24. Scissor ends at y + 88 to prevent overlap with the button at y + 88.
        graphics.enableScissor(x + 24, y + 20, x + 86, y + 88)
        val totalH = if (menu.offers.isEmpty()) 0 else menu.offers.size * 22 - 1
        val maxSY = (totalH - 68).coerceAtLeast(0)
        val scrollY = (scroll * maxSY).toInt()

        for (i in menu.offers.indices) {
            val offer = menu.offers[i]
            val itemY = y + 20 + i * 22 - scrollY
            val isSelected = i == selectedOfferIdx
            val isRowHover = isHover(mouseX, mouseY, x + 24, itemY, 62, 20)

            // Draw offer card texture
            graphics.blit(OFFER_CARD, x + 24, itemY, 0f, 0f, 62, 20, 62, 20)
            if (isSelected) {
                // Draw green selection border around the card
                graphics.fill(x + 24, itemY, x + 86, itemY + 1, 0xFF00FF99.toInt())
                graphics.fill(x + 24, itemY + 19, x + 86, itemY + 20, 0xFF00FF99.toInt())
                graphics.fill(x + 24, itemY, x + 25, itemY + 20, 0xFF00FF99.toInt())
                graphics.fill(x + 85, itemY, x + 86, itemY + 20, 0xFF00FF99.toInt())
            } else if (isRowHover) {
                // White highlight
                graphics.fill(x + 24, itemY, x + 86, itemY + 20, 0x22FFFFFF)
            }

            // Render offer result item
            graphics.renderItem(offer.resultItem, x + 26, itemY + 2)
            graphics.renderItemDecorations(font, offer.resultItem, x + 26, itemY + 2)

            // Display price compact on the card
            val priceStr = if (offer.isCoinOffer) WalletOverlay.formatCompact(offer.priceCCC) else "${offer.costCount}x"
            graphics.drawString(font, priceStr, x + 46, itemY + 6, 0xFFFFFF, false)
        }
        graphics.disableScissor()

        // Scrollbar (shifted 20px right, 13px down): 80 + 20 = 100, 20 + 13 = 33
        graphics.blit(SCROLLER_BG, x + 100, y + 33, 0f, 0f, 6, 52, 6, 52)
        val thumbY = y + 33 + (scroll * (52 - 11)).toInt()
        graphics.blit(SCROLLER, x + 100, thumbY, 0f, 0f, 6, 11, 6, 11)

        // "+ Add offer" button (shifted 10px right, 6px up): 14 + 10 = 24, 94 - 6 = 88
        val isAddHover = isHover(mouseX, mouseY, x + 24, y + 88, 62, 10)
        val addV = if (isAddHover) 10f else 0f
        graphics.blit(ADD_OFFER_BTN, x + 24, y + 88, 0f, addV, 62, 10, 62, 20)
        val addText = "+ Add Offer"
        graphics.drawString(font, addText, x + 24 + (62 - font.width(addText)) / 2, y + 88 + 1, 0xFFFFFF, false)

        // ── Right Panel: Offer Editor (shifted 20px right) ──
        // ex = 108 + 20 = 128
        val ex = x + 128

        // 1. Result slot
        graphics.blit(SLOT_BG, ex, y + 23, 0f, 0f, 16, 16, 16, 16)
        if (!editResult.isEmpty) {
            graphics.renderItem(editResult, ex, y + 23)
            graphics.renderItemDecorations(font, editResult, ex, y + 23)
        }

        // Price textzone background (shifted 20px right): 135 + 20 = 155
        if (editorMode == EditorMode.COIN) {
            priceField.visible = true
            costCountField.visible = false
            graphics.blit(TEXTZONE, x + 155, y + 23, 0f, 0f, 56, 10, 56, 10)
            graphics.drawString(font, "Price:", x + 155, y + 14, 0xAAAAAA, false)
        } else {
            priceField.visible = false
            costCountField.visible = true

            // Cost slot
            graphics.blit(SLOT_BG, ex, y + 58, 0f, 0f, 16, 16, 16, 16)
            if (!editCostItem.isEmpty) {
                graphics.renderItem(editCostItem, ex, y + 58)
                graphics.renderItemDecorations(font, editCostItem, ex, y + 58)
            }

            // Qty textzone background (shifted 20px right): 135 + 20 = 155
            graphics.blit(TEXTZONE, x + 155, y + 58, 0f, 0f, 56, 10, 56, 10)
            graphics.drawString(font, "Qty:", x + 155, y + 49, 0xAAAAAA, false)
        }

        // Toggle Mode text button
        val modeStr = if (editorMode == EditorMode.COIN) "[§aCoin§r] Barter" else "Coin [§aBarter§r]"
        val isModeHover = isHover(mouseX, mouseY, ex, y + 43, 100, 9)
        val modeColor = if (isModeHover) 0xFFFFFF else 0xAAAAAA
        graphics.drawString(font, modeStr, ex, y + 43, modeColor, false)

        // Save Button (green custom button)
        val isSaveHover = isHover(mouseX, mouseY, ex, y + 75, 56, 10)
        val saveV = if (isSaveHover) 10f else 0f
        graphics.blit(CREATE_OFFER_BTN, ex, y + 75, 0f, saveV, 56, 10, 56, 20)
        val saveText = "Save"
        graphics.drawString(font, saveText, ex + (56 - font.width(saveText)) / 2, y + 75 + 1, 0xFFFFFF, false)

        // Delete Button (red box) (shifted 20px right): 60 + 20 = 80
        if (selectedOfferIdx >= 0) {
            val isDelHover = isHover(mouseX, mouseY, ex + 60, y + 75, 45, 10)
            val delColor = if (isDelHover) 0xFFAA2222.toInt() else 0xFF881111.toInt()
            graphics.fill(ex + 60, y + 75, ex + 105, y + 85, delColor)
            graphics.drawString(font, "Delete", ex + 60 + (45 - font.width("Delete")) / 2, y + 76, 0xFFFFFF, false)
        }

        // Whitelist textzone background (shifted 20px right, 6px up): ex = x + 128, y + 94 - 6 = y + 88
        graphics.blit(TEXTZONE, x + 128, y + 88, 0f, 0f, 56, 10, 56, 10)

        // Whitelist buttons (shifted 20px right, 6px up): 60 + 20 = 80 -> ex + 80, 72 + 20 = 92 -> ex + 92
        val isAddW = isHover(mouseX, mouseY, ex + 60, y + 88, 10, 10)
        val addWV = if (isAddW) 10f else 0f
        graphics.blit(PLUS_BTN, ex + 60, y + 88, 0f, addWV, 10, 10, 10, 20)

        val isRemW = isHover(mouseX, mouseY, ex + 72, y + 88, 10, 10)
        val remWV = if (isRemW) 10f else 0f
        graphics.blit(MINUS_BTN, ex + 72, y + 88, 0f, remWV, 10, 10, 10, 20)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = leftPos; val y = topPos
        val mx = mouseX.toInt(); val my = mouseY.toInt()

        // Shop rename
        if (isHover(mx, my, x + 135, y + 2, 40, 11)) {
            sendRename()
            return true
        }

        // Offer list click (shifted 10px right)
        if (isHover(mx, my, x + 24, y + 20, 62, 72)) {
            val totalH = if (menu.offers.isEmpty()) 0 else menu.offers.size * 22 - 1
            val maxSY = (totalH - 72).coerceAtLeast(0)
            val scrollY = (scroll * maxSY).toInt()
            val clickY = my - (y + 20) + scrollY
            val idx = clickY / 22
            if (idx in menu.offers.indices) {
                selectedOfferIdx = idx
                loadOfferIntoEditor(menu.offers[idx])
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
        }

        // "+ Add offer" button click (shifted 10px right, 6px up)
        if (isHover(mx, my, x + 24, y + 88, 62, 10)) {
            selectedOfferIdx = -1
            clearEditor()
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            return true
        }

        // ex = 108 + 20 = 128
        val ex = x + 128

        // Result item slot drag (virtual slot)
        if (isHover(mx, my, ex, y + 23, 16, 16)) {
            val carried = minecraft?.player?.containerMenu?.carried ?: ItemStack.EMPTY
            if (!carried.isEmpty) {
                editResult = carried.copy()
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0f))
                return true
            }
        }

        // Barter cost item slot drag (virtual slot)
        if (editorMode == EditorMode.BARTER && isHover(mx, my, ex, y + 58, 16, 16)) {
            val carried = minecraft?.player?.containerMenu?.carried ?: ItemStack.EMPTY
            if (!carried.isEmpty) {
                editCostItem = carried.copy()
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0f))
                return true
            }
        }

        // Mode toggle
        if (isHover(mx, my, ex, y + 43, 100, 9)) {
            editorMode = if (editorMode == EditorMode.COIN) EditorMode.BARTER else EditorMode.COIN
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            return true
        }

        // Save offer
        if (isHover(mx, my, ex, y + 75, 56, 10)) {
            sendSaveOffer()
            return true
        }

        // Delete offer
        if (selectedOfferIdx >= 0 && isHover(mx, my, ex + 60, y + 75, 45, 10)) {
            sendDeleteOffer()
            return true
        }

        // Whitelist add (shifted 20px right, 6px up)
        if (isHover(mx, my, ex + 60, y + 88, 10, 10)) {
            sendWhitelist(true)
            return true
        }

        // Whitelist remove (shifted 20px right, 6px up)
        if (isHover(mx, my, ex + 72, y + 88, 10, 10)) {
            sendWhitelist(false)
            return true
        }

        // Scroller dragging (shifted 20px right, 13px down)
        if (isHover(mx, my, x + 100, y + 33, 6, 52)) {
            isDragging = true
            updateScroll(mouseY)
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dx: Double, dy: Double): Boolean {
        if (isDragging) {
            updateScroll(mouseY)
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy)
    }

    private fun updateScroll(my: Double) {
        scroll = (((my - topPos - 33 - 5.5) / (52 - 11)).coerceIn(0.0, 1.0)).toFloat()
    }

    private fun loadOfferIntoEditor(offer: PlayerShopOffer) {
        editResult   = offer.resultItem.copy()
        editPriceCCC = offer.priceCCC
        editCostItem = offer.costItem.copy()
        editCostCount = offer.costCount
        priceField.value = offer.priceCCC.toString()
        costCountField.value = offer.costCount.toString()
        editorMode   = if (offer.isCoinOffer) EditorMode.COIN else EditorMode.BARTER
    }

    private fun clearEditor() {
        editResult = ItemStack.EMPTY
        editPriceCCC = 0L
        editCostItem = ItemStack.EMPTY
        editCostCount = 1
        priceField.value = "0"
        costCountField.value = "1"
    }

    private fun sendSaveOffer() {
        if (editResult.isEmpty) return
        val priceCCC = if (editorMode == EditorMode.COIN) priceField.value.toLongOrNull() ?: 0L else 0L
        val costCount = costCountField.value.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val costItem  = if (editorMode == EditorMode.BARTER) editCostItem else ItemStack.EMPTY

        val mc = minecraft ?: return
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level?.registryAccess() ?: return)
        buf.writeBlockPos(menu.shopPos)
        buf.writeInt(selectedOfferIdx)
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, editResult)
        buf.writeLong(priceCCC)
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, costItem)
        buf.writeInt(costCount)
        NetworkManager.sendToServer(ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "player_shop_save_offer"), buf)
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_YES, 1.0f))

        val newOffer = PlayerShopOffer(editResult.copy(), priceCCC, costItem.copy(), costCount)
        if (selectedOfferIdx < 0 || selectedOfferIdx >= menu.offers.size) {
            if (menu.offers.size < 16) menu.offers.add(newOffer)
        } else {
            menu.offers[selectedOfferIdx] = newOffer
        }
    }

    private fun sendDeleteOffer() {
        val mc = minecraft ?: return
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level?.registryAccess() ?: return)
        buf.writeBlockPos(menu.shopPos)
        buf.writeInt(selectedOfferIdx)
        NetworkManager.sendToServer(ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "player_shop_delete_offer"), buf)
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0f))

        if (selectedOfferIdx in menu.offers.indices) {
            menu.offers.removeAt(selectedOfferIdx)
            selectedOfferIdx = -1
            clearEditor()
        }
    }

    private fun sendRename() {
        val name = shopNameField.value.trim().ifBlank { return }
        val mc = minecraft ?: return
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level?.registryAccess() ?: return)
        buf.writeBlockPos(menu.shopPos)
        buf.writeUtf(name)
        NetworkManager.sendToServer(ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "player_shop_rename"), buf)
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
    }

    private fun sendWhitelist(add: Boolean) {
        val name = whitelistField.value.trim().ifBlank { return }
        val mc = minecraft ?: return
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level?.registryAccess() ?: return)
        buf.writeBlockPos(menu.shopPos)
        buf.writeUtf(name)
        buf.writeBoolean(add)
        NetworkManager.sendToServer(ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "player_shop_whitelist"), buf)
        whitelistField.value = ""
    }

    private fun isHover(mx: Int, my: Int, rx: Int, ry: Int, rw: Int, rh: Int) =
        mx >= rx && mx < rx + rw && my >= ry && my < ry + rh

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) { // Escape key
            this.onClose()
            return true
        }
        if (shopNameField.keyPressed(keyCode, scanCode, modifiers) || shopNameField.canConsumeInput()) {
            return true
        }
        if (priceField.keyPressed(keyCode, scanCode, modifiers) || priceField.canConsumeInput()) {
            return true
        }
        if (costCountField.keyPressed(keyCode, scanCode, modifiers) || costCountField.canConsumeInput()) {
            return true
        }
        if (whitelistField.keyPressed(keyCode, scanCode, modifiers) || whitelistField.canConsumeInput()) {
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}
