package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.menu.GymShopMenu
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.CoinType
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class GymShopScreen(
    menu: GymShopMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<GymShopMenu>(menu, playerInventory, title) {

    private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/shop_background.png")
    private val ITEM_BG = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/shop_item_background.png")
    private val SCROLLER_BG = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/scroller_background.png")
    private val SCROLLER_THUMB = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/scroller.png")
    private val PREVIEW_BG = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/item_preview_background.png")
    private val QTY_BG = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/quantity_text_background.png")
    private val MINUS_BTN = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/quantity_text_minus_button.png")
    private val PLUS_BTN = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/quantity_text_plus_button.png")
    private val BUY_BTN = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/shop/buy_button.png")
    private val BACK_BUTTON = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/back_button.png")
    private val BACK_BUTTON_ICON = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "textures/gui/back_button_icon.png")

    private var scrollProgress: Float = 0f
    private var isDraggingScroller: Boolean = false
    private var selectedItemIndex: Int = 0
    private var quantity: Int = 1

    private val itemCache = mutableMapOf<String, ItemStack>()

    init {
        this.imageWidth = 184
        this.imageHeight = 180
        this.inventoryLabelY = this.imageHeight - 94
    }

    override fun init() {
        super.init()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Retiré pour l'instant
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos
        val y = topPos

        // 1. Fond principal
        graphics.blit(BACKGROUND, x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight)

        // 2. Liste d'items avec Scissor
        graphics.enableScissor(x + 8, y + 1, x + 8 + 77, y + 1 + 80)
        val totalListHeight = if (menu.items.isEmpty()) 0 else menu.items.size * 23 - 1
        val maxScrollY = (totalListHeight - 80).coerceAtLeast(0)
        val currentScrollY = (scrollProgress * maxScrollY).toInt()

        for (i in menu.items.indices) {
            val item = menu.items[i]
            // Espacement de 1px entre chaque item (hauteur de carte 22 + espacement 1 = 23px)
            // itemY commence à y+2 pour laisser 1px de marge en haut de la liste
            val itemY = y + 2 + i * 23 - currentScrollY

            // Fond
            val isSelected = i == selectedItemIndex
            val vOffset = if (isSelected) 22f else 0f
            graphics.blit(ITEM_BG, x + 8, itemY, 0f, vOffset, 77, 22, 77, 44)

            // Item à vendre (-1px vers la gauche)
            val resultStack = getItemStack(item.resultItem, item.resultCount)
            graphics.renderItem(resultStack, x + 11, itemY + 3)
            graphics.renderItemDecorations(font, resultStack, x + 11, itemY + 3)

            // Cost item — rapproché du prix (x: 56)
            val costStack = getItemStack(item.costItem, item.costCount)
            graphics.renderItem(costStack, x + 56, itemY + 3)

            // Prix — bord droit à x+54, juste à gauche de la pièce
            val canAffordUnit = canAfford(item.costItem, item.costCount, 1)
            val priceText = item.costCount.toString()
            val priceW = font.width(priceText)
            val priceColor = if (canAffordUnit) 0x3F3F3F else 0xFF5555
            graphics.drawString(font, priceText, x + 54 - priceW, itemY + 7, priceColor, false)
        }
        graphics.disableScissor()

        // 3. Scroller
        graphics.blit(SCROLLER_BG, x + 86, y + 1, 0f, 0f, 8, 81, 8, 81)
        val thumbMaxTravel = 81 - 11
        val thumbY = y + 1 + (scrollProgress * thumbMaxTravel).toInt()
        graphics.blit(SCROLLER_THUMB, x + 87, thumbY, 0f, 0f, 6, 11, 6, 11)

        // 4. Zone de preview (X restaurés pour recentrer à droite, Y reste à y+6)
        if (menu.items.isNotEmpty() && selectedItemIndex in menu.items.indices) {
            val item = menu.items[selectedItemIndex]

            // Item Preview Background (x: 120, y: 6) — +3px vers la droite
            graphics.blit(PREVIEW_BG, x + 120, y + 6, 0f, 0f, 32, 32, 32, 32)
            
            // Preview Item (x: 128, y: 14) — +3px vers la droite
            val previewStack = getItemStack(item.resultItem, item.resultCount)
            graphics.renderItem(previewStack, x + 128, y + 14)
            graphics.renderItemDecorations(font, previewStack, x + 128, y + 14)

            // Qty Background (x: 125, y: 43) — +3px vers la droite
            graphics.blit(QTY_BG, x + 125, y + 43, 0f, 0f, 22, 12, 22, 12)

            // Minus / Plus Buttons (x: 114, 147, y: 43) — +3px vers la droite
            val isMinusHovered = isMouseOver(mouseX, mouseY, x + 114, y + 43, 11, 12)
            val minusV = if (isMinusHovered) 12f else 0f
            graphics.blit(MINUS_BTN, x + 114, y + 43, 0f, minusV, 11, 12, 11, 24)

            val isPlusHovered = isMouseOver(mouseX, mouseY, x + 147, y + 43, 11, 12)
            val plusV = if (isPlusHovered) 12f else 0f
            graphics.blit(PLUS_BTN, x + 147, y + 43, 0f, plusV, 11, 12, 11, 24)

            // Quantity text
            val qtyStr = quantity.toString()
            val qtyW = font.width(qtyStr)
            graphics.drawString(font, qtyStr, x + 125 + (22 - qtyW) / 2, y + 45, 0xFFFFFF, false)

            // Buy button (x: 100, y: 59) — +3px vers la droite
            val canAffordTotal = canAfford(item.costItem, item.costCount, quantity)
            val isBuyHovered = canAffordTotal && isMouseOver(mouseX, mouseY, x + 100, y + 59, 73, 17)
            val buyV = if (isBuyHovered) 17f else 0f
            graphics.blit(BUY_BTN, x + 100, y + 59, 0f, buyV, 73, 17, 73, 34)

            // Buy Text
            val buyText = Component.translatable("cobblemongymodyssey.shop.buy").string
            val buyTextW = font.width(buyText)
            val buyTextColor = if (canAffordTotal) 0xFFFFFF else 0xAAAAAA
            graphics.drawString(font, buyText, x + 100 + (73 - buyTextW) / 2, y + 59 + (17 - 8) / 2, buyTextColor, canAffordTotal)
        }

        // 5. Balance (texte décalé -6px sur la gauche pour dégager le sprite copper coin de la texture)
        val balance = ClientWalletCache.balance
        val balanceText = WalletOverlay.formatCompact(balance)
        val balanceW = font.width(balanceText)
        graphics.drawString(font, balanceText, x + 92 - balanceW, y + 85, 0x3F3F3F, false)

        // 6. Draw Back Button (Bottom-left outside the interface)
        val backX = x - 29
        val backY = y + 167
        val isBackHovered = mouseX >= backX && mouseX < backX + 26 && mouseY >= backY && mouseY < backY + 13
        val backV = if (isBackHovered) 13f else 0f

        graphics.blit(BACK_BUTTON, backX, backY, 0f, backV, 26, 13, 26, 26)
        graphics.blit(BACK_BUTTON_ICON, backX + 2, backY + 1, 0f, 0f, 21, 11, 21, 11)
    }

    override fun renderTooltip(graphics: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(graphics, x, y)
        val rx = leftPos
        val ry = topPos

        // Preview item tooltip
        if (menu.items.isNotEmpty() && selectedItemIndex in menu.items.indices) {
            val item = menu.items[selectedItemIndex]
            if (isMouseOver(x, y, rx + 120, ry + 6, 32, 32)) {
                val previewStack = getItemStack(item.resultItem, item.resultCount)
                graphics.renderTooltip(font, previewStack, x, y)
                return
            }
        }

        // Viewport list items tooltip
        if (isMouseOver(x, y, rx + 8, ry + 1, 77, 80)) {
            val totalListHeight = if (menu.items.isEmpty()) 0 else menu.items.size * 23 - 1
            val maxScrollY = (totalListHeight - 80).coerceAtLeast(0)
            val currentScrollY = (scrollProgress * maxScrollY).toInt()

            val clickY = y - (ry + 1) + currentScrollY
            val clickedIndex = clickY / 23
            if (clickedIndex in menu.items.indices && clickY % 23 < 22) {
                val item = menu.items[clickedIndex]
                val slotLocalY = (clickedIndex * 23) - currentScrollY
                if (isMouseOver(x, y, rx + 8 + 4, ry + 2 + slotLocalY + 3, 16, 16)) {
                    val resultStack = getItemStack(item.resultItem, item.resultCount)
                    graphics.renderTooltip(font, resultStack, x, y)
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = leftPos
        val y = topPos

        // Back button click (Bottom-left outside the interface)
        val backX = x - 29
        val backY = y + 167
        if (mouseX >= backX && mouseX < backX + 26 && mouseY >= backY && mouseY < backY + 13) {
            minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
            ))
            val buf = net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(),
                minecraft?.level?.registryAccess() ?: throw java.lang.IllegalStateException("Registry access not available")
            )
            dev.architectury.networking.NetworkManager.sendToServer(
                ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "open_badge_case"),
                buf
            )
            return true
        }

        if (isMouseOver(mouseX.toInt(), mouseY.toInt(), x + 8, y + 1, 77, 80)) {
            val totalListHeight = if (menu.items.isEmpty()) 0 else menu.items.size * 23 - 1
            val maxScrollY = (totalListHeight - 80).coerceAtLeast(0)
            val currentScrollY = (scrollProgress * maxScrollY).toInt()

            val clickY = mouseY.toInt() - (y + 1) + currentScrollY
            val clickedIndex = clickY / 23
            if (clickedIndex in menu.items.indices && clickY % 23 < 22) {
                selectedItemIndex = clickedIndex
                quantity = 1
                minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
                ))
                return true
            }
        }

        if (isMouseOver(mouseX.toInt(), mouseY.toInt(), x + 86, y + 1, 8, 81)) {
            isDraggingScroller = true
            updateScroll(mouseY)
            return true
        }

        if (menu.items.isNotEmpty() && selectedItemIndex in menu.items.indices) {
            val item = menu.items[selectedItemIndex]

            // Minus (x: 114, y: 43)
            // - Click normal : -1
            // - Shift+Click  : -10
            if (isMouseOver(mouseX.toInt(), mouseY.toInt(), x + 114, y + 43, 11, 12)) {
                val isShift = net.minecraft.client.gui.screens.Screen.hasShiftDown()
                val delta = if (isShift) 10 else 1
                val newQty = (quantity - delta).coerceAtLeast(1)
                if (newQty != quantity) {
                    quantity = newQty
                    minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
                    ))
                }
                return true
            }

            // Plus (x: 147, y: 43)
            // - Click normal      : +1
            // - Shift+Click       : +10
            // - Ctrl+Shift+Click  : max achetable (balance / coût unitaire), plafonné à 99
            if (isMouseOver(mouseX.toInt(), mouseY.toInt(), x + 147, y + 43, 11, 12)) {
                val isShift = net.minecraft.client.gui.screens.Screen.hasShiftDown()
                val isCtrl  = net.minecraft.client.gui.screens.Screen.hasControlDown()
                val newQty = when {
                    isCtrl && isShift -> {
                        // Max affordable
                        val coinType = getCoinType(item.costItem)
                        val maxAffordable = if (coinType != null && item.costCount > 0) {
                            (ClientWalletCache.balance / (coinType.valueCCC * item.costCount)).coerceAtMost(99L).toInt()
                        } else {
                            99
                        }
                        maxAffordable.coerceAtLeast(1)
                    }
                    isShift -> (quantity + 10).coerceAtMost(99)
                    else    -> (quantity + 1).coerceAtMost(99)
                }
                if (newQty != quantity) {
                    quantity = newQty
                    minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, if (isCtrl && isShift) 1.4f else 1.0f
                    ))
                }
                return true
            }

            // Buy Button (x: 100, y: 59) — +3px vers la droite
            if (isMouseOver(mouseX.toInt(), mouseY.toInt(), x + 100, y + 59, 73, 17)) {
                if (canAfford(item.costItem, item.costCount, quantity)) {
                    val clickId = selectedItemIndex * 100 + (quantity - 1)
                    minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, clickId)
                    minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f
                    ))
                } else {
                    minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.VILLAGER_NO, 1.0f
                    ))
                }
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDraggingScroller = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (isDraggingScroller) {
            updateScroll(mouseY)
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (menu.items.size > 3) {
            val totalListHeight = if (menu.items.isEmpty()) 0 else menu.items.size * 23 - 1
            val maxScrollY = (totalListHeight - 80).coerceAtLeast(0)
            if (maxScrollY > 0) {
                val delta = -scrollY.toFloat() * 15f
                val currentScrollY = scrollProgress * maxScrollY
                val nextScrollY = (currentScrollY + delta).coerceIn(0f, maxScrollY.toFloat())
                scrollProgress = nextScrollY / maxScrollY
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    private fun updateScroll(mouseY: Double) {
        val y = topPos + 1
        val relativeY = (mouseY - y).coerceIn(0.0, 81.0)
        val travel = 81 - 11
        val thumbCenterOffset = 5.5
        val progress = ((relativeY - thumbCenterOffset) / travel).coerceIn(0.0, 1.0)
        scrollProgress = progress.toFloat()
    }

    private fun getItemStack(id: String, count: Int): ItemStack {
        val cacheKey = "$id:$count"
        return itemCache.getOrPut(cacheKey) {
            val loc = ResourceLocation.tryParse(id)
            val item = if (loc != null) net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc) else net.minecraft.world.item.Items.AIR
            ItemStack(item, count)
        }
    }

    private fun isMouseOver(mx: Int, my: Int, rx: Int, ry: Int, rw: Int, rh: Int): Boolean {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh
    }

    private fun getCoinType(itemId: String): CoinType? {
        val path = if (itemId.contains(":")) itemId.substringAfter(":") else itemId
        return when (path) {
            "cobble_copper_coin" -> CoinType.COPPER
            "cobble_silver_coin" -> CoinType.SILVER
            "cobble_gold_coin" -> CoinType.GOLD
            "cobble_platinum_coin" -> CoinType.PLATINUM
            else -> null
        }
    }

    private fun hasPhysicalItem(itemId: String, count: Int): Boolean {
        val loc = ResourceLocation.tryParse(itemId) ?: return false
        val item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc)
        var found = 0
        for (slot in menu.slots) {
            val stack = slot.item
            if (stack.item == item) {
                found += stack.count
            }
        }
        return found >= count
    }

    private fun canAfford(costItem: String, costCount: Int, qty: Int): Boolean {
        val coinType = getCoinType(costItem)
        val totalCost = costCount.toLong() * qty
        return if (coinType != null) {
            ClientWalletCache.balance >= coinType.valueCCC * totalCost
        } else {
            hasPhysicalItem(costItem, totalCost.toInt())
        }
    }
}
