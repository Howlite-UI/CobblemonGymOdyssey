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

    private val ITEM_BG   = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/shop_item_background.png")
    private val SCROLLER_BG  = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/scroller_background.png")
    private val SCROLLER     = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/scroller.png")

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
        imageWidth  = 220
        imageHeight = 256
        inventoryLabelY = 166
    }

    override fun init() {
        super.init()

        val x = leftPos; val y = topPos

        // Shop name at the top
        shopNameField = EditBox(font, x + 8, y + 2, 130, 11, Component.literal("Shop name"))
        shopNameField.setMaxLength(32)
        shopNameField.value = menu.shopName
        shopNameField.setBordered(false)
        addRenderableWidget(shopNameField)

        // Price field in the editor (aligned next to result slot)
        priceField = EditBox(font, x + 132, y + 33, 75, 12, Component.literal("Price CCC"))
        priceField.setMaxLength(18)
        priceField.value = "0"
        priceField.setFilter { it.all { c -> c.isDigit() } }
        addRenderableWidget(priceField)

        // Cost count in the editor (aligned next to barter cost slot)
        costCountField = EditBox(font, x + 132, y + 65, 30, 12, Component.literal("Count"))
        costCountField.setMaxLength(4)
        costCountField.value = "1"
        costCountField.setFilter { it.all { c -> c.isDigit() } }
        addRenderableWidget(costCountField)

        // Whitelist field at the bottom of the editor panel
        whitelistField = EditBox(font, x + 110, y + 98, 75, 12, Component.literal("Player name"))
        whitelistField.setMaxLength(16)
        whitelistField.setHint(Component.literal("Trust Player"))
        addRenderableWidget(whitelistField)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Draw inventory and stock titles
        graphics.drawString(font, playerInventoryTitle, 29, inventoryLabelY - 10, 0x404040, false)
        graphics.drawString(font, Component.literal("Stock"), 29, 104, 0x404040, false)
    }

    private fun drawSlotBg(graphics: GuiGraphics, x: Int, y: Int) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF151515.toInt()) // center
        graphics.fill(x, y, x + 18, y + 1, 0xFF0A0A0A.toInt())  // top border
        graphics.fill(x, y, x + 1, y + 18, 0xFF0A0A0A.toInt())  // left border
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFF3D3D3D.toInt()) // right border
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFF3D3D3D.toInt()) // bottom border
    }

    override fun renderBg(graphics: GuiGraphics, pt: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos; val y = topPos

        // 1. Draw Main Modern Panel (Dark Translucent)
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xEE1A1A1A.toInt())
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF444444.toInt())
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF444444.toInt())
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF444444.toInt())
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF444444.toInt())

        // Section separators
        graphics.fill(x + 1, y + 15, x + imageWidth - 1, y + 16, 0xFF333333.toInt()) // top bar separator
        graphics.fill(x + 103, y + 16, x + 104, y + 112, 0xFF333333.toInt()) // vertical split
        graphics.fill(x + 1, y + 112, x + imageWidth - 1, y + 113, 0xFF333333.toInt()) // middle separator

        // Rename Button in Top Bar
        val isRenameHover = isHover(mouseX, mouseY, x + 142, y + 2, 40, 11)
        val renameColor = if (isRenameHover) 0xFF555577.toInt() else 0xFF333355.toInt()
        graphics.fill(x + 142, y + 2, x + 182, y + 13, renameColor)
        graphics.drawString(font, "Rename", x + 145, y + 4, 0xFFFFFF, false)

        // ── Left Panel: Offers List ──
        graphics.drawString(font, "Offers", x + 8, y + 18, 0x00FF99, false)

        graphics.enableScissor(x + 8, y + 28, x + 90, y + 96)
        val totalH = if (menu.offers.isEmpty()) 0 else menu.offers.size * 20 - 1
        val maxSY = (totalH - 68).coerceAtLeast(0)
        val scrollY = (scroll * maxSY).toInt()

        for (i in menu.offers.indices) {
            val offer = menu.offers[i]
            val itemY = y + 28 + i * 20 - scrollY
            val isSelected = i == selectedOfferIdx
            val isRowHover = isHover(mouseX, mouseY, x + 8, itemY, 82, 19)

            val rowColor = when {
                isSelected -> 0x4400FF99.toInt()
                isRowHover -> 0x22FFFFFF.toInt()
                else -> 0x11FFFFFF.toInt()
            }
            graphics.fill(x + 8, itemY, x + 90, itemY + 19, rowColor)

            // Render result item
            graphics.renderItem(offer.resultItem, x + 10, itemY + 1)
            graphics.renderItemDecorations(font, offer.resultItem, x + 10, itemY + 1)

            // Display price compact
            val priceStr = if (offer.isCoinOffer) WalletOverlay.formatCompact(offer.priceCCC) else "${offer.costCount}x"
            graphics.drawString(font, priceStr, x + 30, itemY + 5, 0xFFFFFF, false)
        }
        graphics.disableScissor()

        // Scroller for offers
        graphics.blit(SCROLLER_BG, x + 92, y + 28, 0f, 0f, 6, 68, 6, 68)
        val thumbY = y + 28 + (scroll * (68 - 8)).toInt()
        graphics.blit(SCROLLER, x + 92, thumbY, 0f, 0f, 6, 8, 6, 8)

        // "+ Add offer" button below list
        val addY = y + 98
        val isAddHover = isHover(mouseX, mouseY, x + 8, addY, 90, 12)
        val addColor = if (isAddHover) 0xFF22AA22.toInt() else 0xFF118811.toInt()
        graphics.fill(x + 8, addY, x + 98, addY + 12, addColor)
        val addStr = "+ Add offer"
        graphics.drawString(font, addStr, x + 8 + (90 - font.width(addStr)) / 2, addY + 2, 0xFFFFFF, false)

        // ── Right Panel: Offer Editor ──
        val ex = x + 110
        graphics.drawString(font, "§lOffer Editor", ex, y + 18, 0x00FF99, false)

        // 1. Result slot (virtual)
        drawSlotBg(graphics, ex, y + 30)
        if (!editResult.isEmpty) {
            graphics.renderItem(editResult, ex + 1, y + 31)
            graphics.renderItemDecorations(font, editResult, ex + 1, y + 31)
        }

        // Toggle Mode
        val modeStr = if (editorMode == EditorMode.COIN) "[§aCoin§r] Barter" else "Coin [§aBarter§r]"
        val isModeHover = isHover(mouseX, mouseY, ex, y + 50, 100, 10)
        val modeColor = if (isModeHover) 0xFFFFFF else 0xAAAAAA
        graphics.drawString(font, modeStr, ex, y + 50, modeColor, false)

        // 2. Barter/Price slots
        if (editorMode == EditorMode.COIN) {
            priceField.visible = true
            costCountField.visible = false
            graphics.drawString(font, "Price (CCC):", x + 132, y + 22, 0xAAAAAA, false)
        } else {
            priceField.visible = false
            costCountField.visible = true

            // Cost slot (virtual)
            drawSlotBg(graphics, ex, y + 62)
            if (!editCostItem.isEmpty) {
                graphics.renderItem(editCostItem, ex + 1, y + 63)
                graphics.renderItemDecorations(font, editCostItem, ex + 1, y + 63)
            }
            graphics.drawString(font, "Qty:", x + 132, y + 55, 0xAAAAAA, false)
        }

        // Save / Delete buttons
        val saveStr = if (selectedOfferIdx < 0) "Create" else "Save"
        val isSaveHover = isHover(mouseX, mouseY, ex, y + 80, 45, 12)
        val saveBtnColor = if (isSaveHover) 0xFF22AA22.toInt() else 0xFF118811.toInt()
        graphics.fill(ex, y + 80, ex + 45, y + 92, saveBtnColor)
        graphics.drawString(font, saveStr, ex + (45 - font.width(saveStr)) / 2, y + 82, 0xFFFFFF, false)

        if (selectedOfferIdx >= 0) {
            val isDelHover = isHover(mouseX, mouseY, x + 160, y + 80, 45, 12)
            val delBtnColor = if (isDelHover) 0xFFAA2222.toInt() else 0xFF881111.toInt()
            graphics.fill(x + 160, y + 80, x + 205, y + 92, delBtnColor)
            graphics.drawString(font, "Delete", x + 160 + (45 - font.width("Delete")) / 2, y + 82, 0xFFFFFF, false)
        }

        // Whitelist buttons
        val isAddW = isHover(mouseX, mouseY, x + 190, y + 98, 10, 12)
        graphics.fill(x + 190, y + 98, x + 200, y + 110, if (isAddW) 0xFF22AA22.toInt() else 0xFF118811.toInt())
        graphics.drawString(font, "+", x + 193, y + 100, 0xFFFFFF, false)

        val isRemW = isHover(mouseX, mouseY, x + 202, y + 98, 10, 12)
        graphics.fill(x + 202, y + 98, x + 212, y + 110, if (isRemW) 0xFFAA2222.toInt() else 0xFF881111.toInt())
        graphics.drawString(font, "-", x + 205, y + 100, 0xFFFFFF, false)

        // ── Bottom Panel: Inventory Slots ──
        val startX = 29
        // Draw slot backgrounds for 27 stock slots
        for (i in 0 until 27) {
            val row = i / 9
            val col = i % 9
            drawSlotBg(graphics, x + startX + col * 18, y + 114 + row * 18)
        }

        // Draw slot backgrounds for player inventory
        for (row in 0..2) {
            for (col in 0..8) {
                drawSlotBg(graphics, x + startX + col * 18, y + 176 + row * 18)
            }
        }

        // Draw slot backgrounds for hotbar
        for (col in 0..8) {
            drawSlotBg(graphics, x + startX + col * 18, y + 234)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = leftPos; val y = topPos
        val mx = mouseX.toInt(); val my = mouseY.toInt()

        // Shop rename
        if (isHover(mx, my, x + 142, y + 2, 40, 11)) {
            sendRename()
            return true
        }

        // Offer list click
        if (isHover(mx, my, x + 8, y + 28, 82, 68)) {
            val totalH = if (menu.offers.isEmpty()) 0 else menu.offers.size * 20 - 1
            val maxSY = (totalH - 68).coerceAtLeast(0)
            val scrollY = (scroll * maxSY).toInt()
            val clickY = my - (y + 28) + scrollY
            val idx = clickY / 20
            if (idx in menu.offers.indices) {
                selectedOfferIdx = idx
                loadOfferIntoEditor(menu.offers[idx])
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
        }

        // "+ Add offer" button click
        if (isHover(mx, my, x + 8, y + 98, 90, 12)) {
            selectedOfferIdx = -1
            clearEditor()
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            return true
        }

        // Result item slot drag (virtual slot)
        if (isHover(mx, my, x + 110, y + 30, 18, 18)) {
            val carried = minecraft?.player?.containerMenu?.carried ?: ItemStack.EMPTY
            if (!carried.isEmpty) {
                editResult = carried.copy()
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0f))
                return true
            }
        }

        // Barter cost item slot drag (virtual slot)
        if (editorMode == EditorMode.BARTER && isHover(mx, my, x + 110, y + 62, 18, 18)) {
            val carried = minecraft?.player?.containerMenu?.carried ?: ItemStack.EMPTY
            if (!carried.isEmpty) {
                editCostItem = carried.copy()
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0f))
                return true
            }
        }

        // Mode toggle
        if (isHover(mx, my, x + 110, y + 50, 100, 10)) {
            editorMode = if (editorMode == EditorMode.COIN) EditorMode.BARTER else EditorMode.COIN
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            return true
        }

        // Save offer
        if (isHover(mx, my, x + 110, y + 80, 45, 12)) {
            sendSaveOffer()
            return true
        }

        // Delete offer
        if (selectedOfferIdx >= 0 && isHover(mx, my, x + 160, y + 80, 45, 12)) {
            sendDeleteOffer()
            return true
        }

        // Whitelist add
        if (isHover(mx, my, x + 190, y + 98, 10, 12)) {
            sendWhitelist(true)
            return true
        }

        // Whitelist remove
        if (isHover(mx, my, x + 202, y + 98, 10, 12)) {
            sendWhitelist(false)
            return true
        }

        // Scroller dragging
        if (isHover(mx, my, x + 92, y + 28, 6, 68)) {
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
        scroll = (((my - topPos - 28 - 4) / (68 - 8)).coerceIn(0.0, 1.0)).toFloat()
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
}
