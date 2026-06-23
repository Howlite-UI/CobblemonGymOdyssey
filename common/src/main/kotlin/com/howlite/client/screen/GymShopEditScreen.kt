package com.howlite.client.screen

import com.google.gson.GsonBuilder
import com.howlite.CobblemonGymOdyssey
import com.howlite.data.GymBadge
import com.howlite.data.GymRegion
import com.howlite.menu.GymShopEditMenu
import com.howlite.shop.GymShop
import dev.architectury.networking.NetworkManager
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class GymShopEditScreen(
    menu: GymShopEditMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<GymShopEditMenu>(menu, playerInventory, title) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Local mutable copy of the config for staging
    private var configCopy: GymShop.ShopsConfig = cloneConfig(menu.config)
    private var selectedRegion: GymRegion = GymRegion.KANTO
    private var selectedItemIndex: Int = -1

    // Input fields
    private lateinit var resultItemBox: EditBox
    private lateinit var resultCountBox: EditBox
    private lateinit var costItemBox: EditBox
    private lateinit var costCountBox: EditBox
    private lateinit var requiredBadgeBox: EditBox

    // Scroll progress for the item list
    private var scrollProgress: Float = 0f
    private var isDraggingScroller: Boolean = false

    private val itemCache = mutableMapOf<String, ItemStack>()

    init {
        this.imageWidth = 256
        this.imageHeight = 224
        this.inventoryLabelY = 128
    }

    override fun init() {
        super.init()
        
        val gx = leftPos
        val gy = topPos

        // Initialize Edit Boxes
        resultItemBox = EditBox(font, gx + 130, gy + 18, 114, 12, Component.literal("Result Item"))
        resultItemBox.setMaxLength(128)
        
        resultCountBox = EditBox(font, gx + 130, gy + 42, 30, 12, Component.literal("Result Count"))
        resultCountBox.setFilter { it.all { c -> c.isDigit() } }
        
        costItemBox = EditBox(font, gx + 130, gy + 66, 114, 12, Component.literal("Cost Item"))
        costItemBox.setMaxLength(128)
        
        costCountBox = EditBox(font, gx + 130, gy + 90, 30, 12, Component.literal("Cost Count"))
        costCountBox.setFilter { it.all { c -> c.isDigit() } }
        
        requiredBadgeBox = EditBox(font, gx + 130, gy + 114, 114, 12, Component.literal("Required Badge"))
        requiredBadgeBox.setMaxLength(64)

        // Add inputs as renderable widgets
        addRenderableWidget(resultItemBox)
        addRenderableWidget(resultCountBox)
        addRenderableWidget(costItemBox)
        addRenderableWidget(costCountBox)
        addRenderableWidget(requiredBadgeBox)

        // Set responders to update the staged model on keypress
        val updateStaged = {
            val list = getSelectedRegionItemsMutable()
            if (selectedItemIndex in list.indices) {
                val current = list[selectedItemIndex]
                val updated = GymShop.ShopItemConfig(
                    requiredBadge = requiredBadgeBox.value.trim(),
                    costItem = costItemBox.value.trim(),
                    costCount = costCountBox.value.toIntOrNull() ?: 1,
                    resultItem = resultItemBox.value.trim(),
                    resultCount = resultCountBox.value.toIntOrNull() ?: 1
                )
                list[selectedItemIndex] = updated
            }
        }

        resultItemBox.setResponder { updateStaged() }
        resultCountBox.setResponder { updateStaged() }
        costItemBox.setResponder { updateStaged() }
        costCountBox.setResponder { updateStaged() }
        requiredBadgeBox.setResponder { updateStaged() }

        // Populate fields initially
        selectItem(-1)
    }

    private fun getSelectedRegionItemsMutable(): MutableList<GymShop.ShopItemConfig> {
        val regionName = selectedRegion.name
        val shopConfig = configCopy.shops[regionName] ?: GymShop.RegionShopConfig()
        val mutableList = shopConfig.items.toMutableList()
        
        // Make sure it is backed in the map
        val updatedShops = configCopy.shops.toMutableMap()
        updatedShops[regionName] = GymShop.RegionShopConfig(mutableList)
        configCopy = GymShop.ShopsConfig(updatedShops)
        
        return mutableList
    }

    private fun selectItem(index: Int) {
        val list = getSelectedRegionItemsMutable()
        if (index in list.indices) {
            selectedItemIndex = index
            val item = list[index]
            resultItemBox.value = item.resultItem
            resultCountBox.value = item.resultCount.toString()
            costItemBox.value = item.costItem
            costCountBox.value = item.costCount.toString()
            requiredBadgeBox.value = item.requiredBadge
            
            resultItemBox.setEditable(true)
            resultCountBox.setEditable(true)
            costItemBox.setEditable(true)
            costCountBox.setEditable(true)
            requiredBadgeBox.setEditable(true)
        } else {
            selectedItemIndex = -1
            resultItemBox.value = ""
            resultCountBox.value = ""
            costItemBox.value = ""
            costCountBox.value = ""
            requiredBadgeBox.value = ""
            
            resultItemBox.setEditable(false)
            resultCountBox.setEditable(false)
            costItemBox.setEditable(false)
            costCountBox.setEditable(false)
            requiredBadgeBox.setEditable(false)
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Render inventory label at the bottom
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos
        val y = topPos

        // 1. Draw Main Dark Translucent Panel
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xEE1A1A1A.toInt())
        // Draw Borders
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF444444.toInt())
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF444444.toInt())
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF444444.toInt())
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF444444.toInt())

        // Draw splits
        graphics.fill(x + 50, y + 15, x + 51, y + 130, 0xFF333333.toInt()) // split between regions and items
        graphics.fill(x + 124, y + 15, x + 125, y + 130, 0xFF333333.toInt()) // split between items and editor
        graphics.fill(x + 1, y + 131, x + imageWidth - 1, y + 132, 0xFF333333.toInt()) // split between top editor and player inv

        // Title bar
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 14, 0xFF2D2D2D.toInt())
        graphics.drawString(font, "GYM SHOP EDITOR (ADMIN)", x + 6, y + 4, 0x00FF99.toInt(), false)

        // Render Save Button in Title Bar
        val saveHover = mouseX >= x + 160 && mouseX < x + 250 && mouseY >= y + 2 && mouseY < y + 13
        val saveColor = if (saveHover) 0xFF44FF44.toInt() else 0xFF22AA22.toInt()
        graphics.fill(x + 160, y + 2, x + 250, y + 13, saveColor)
        val saveTxt = "SAVE & APPLY"
        val saveTxtW = font.width(saveTxt)
        graphics.drawString(font, saveTxt, x + 160 + (90 - saveTxtW) / 2, y + 3, 0xFFFFFF, false)

        // 2. Draw Region List (Far Left)
        val regions = GymRegion.entries
        for (i in regions.indices) {
            val reg = regions[i]
            val regY = y + 16 + i * 12
            val isRegSelected = reg == selectedRegion
            
            // Draw hover/selected background
            val regHover = mouseX >= x + 3 && mouseX < x + 48 && mouseY >= regY && mouseY < regY + 11
            val regBgColor = when {
                isRegSelected -> 0x4400FF99.toInt()
                regHover -> 0x22FFFFFF.toInt()
                else -> 0x00000000
            }
            if (regBgColor != 0) {
                graphics.fill(x + 3, regY, x + 48, regY + 11, regBgColor)
            }
            
            val regTextColor = if (isRegSelected) 0x00FF99 else 0xAAAAAA
            graphics.drawString(font, reg.name, x + 5, regY + 2, regTextColor, false)
        }

        // 3. Draw Items List (Middle)
        val itemsList = getSelectedRegionItemsMutable()
        graphics.enableScissor(x + 52, y + 16, x + 116, y + 130)
        
        val totalListHeight = if (itemsList.isEmpty()) 0 else itemsList.size * 22
        val maxScrollY = (totalListHeight - 110).coerceAtLeast(0)
        val currentScrollY = (scrollProgress * maxScrollY).toInt()

        for (i in itemsList.indices) {
            val item = itemsList[i]
            val itemY = y + 16 + i * 22 - currentScrollY
            
            val isItemSelected = i == selectedItemIndex
            val itemHover = mouseX >= x + 52 && mouseX < x + 116 && mouseY >= itemY && mouseY < itemY + 21
            val itemBgColor = when {
                isItemSelected -> 0x3300FF99.toInt()
                itemHover -> 0x11FFFFFF.toInt()
                else -> 0x11000000.toInt()
            }
            graphics.fill(x + 52, itemY, x + 116, itemY + 21, itemBgColor)

            // Render result item
            val resultStack = getItemStack(item.resultItem, item.resultCount)
            graphics.renderItem(resultStack, x + 54, itemY + 2)
            graphics.renderItemDecorations(font, resultStack, x + 54, itemY + 2)

            // Render cost item
            val costStack = getItemStack(item.costItem, item.costCount)
            graphics.renderItem(costStack, x + 98, itemY + 2)

            // Price/badge simple check text
            val costTxt = item.costCount.toString()
            graphics.drawString(font, costTxt, x + 96 - font.width(costTxt), itemY + 6, 0xCCCCCC, false)
        }
        graphics.disableScissor()

        // Draw Scroller for middle list
        graphics.fill(x + 118, y + 16, x + 122, y + 126, 0xFF222222.toInt())
        val thumbMaxTravel = 110 - 10
        val thumbY = y + 16 + (scrollProgress * thumbMaxTravel).toInt()
        graphics.fill(x + 118, thumbY, x + 122, thumbY + 10, 0xFF555555.toInt())

        // Draw '+' Button at the bottom of the items list
        val addHover = mouseX >= x + 52 && mouseX < x + 122 && mouseY >= y + 121 && mouseY < y + 130
        val addBgColor = if (addHover) 0xFF33AA33.toInt() else 0xFF228822.toInt()
        graphics.fill(x + 52, y + 121, x + 122, y + 130, addBgColor)
        val addText = "+ ADD OFFER"
        graphics.drawString(font, addText, x + 52 + (70 - font.width(addText)) / 2, y + 122, 0xFFFFFF, false)

        // 4. Draw Selected Item Editor Panel (Right)
        if (selectedItemIndex in itemsList.indices) {
            val item = itemsList[selectedItemIndex]

            graphics.drawString(font, "VENDU (RESULTAT) :", x + 130, y + 10, 0xAAAAAA, false)
            graphics.drawString(font, "QTÉ :", x + 130 + 114 - 30 - 30, y + 34, 0xAAAAAA, false)
            
            graphics.drawString(font, "COÛT (MONNAIE) :", x + 130, y + 58, 0xAAAAAA, false)
            graphics.drawString(font, "PRIX :", x + 130 + 114 - 30 - 30, y + 82, 0xAAAAAA, false)

            graphics.drawString(font, "BADGE REQUIS :", x + 130, y + 106, 0xAAAAAA, false)

            // Render Preview Items next to input boxes
            val resultStack = getItemStack(item.resultItem, 1)
            graphics.renderItem(resultStack, x + 232, y + 2)
            
            val costStack = getItemStack(item.costItem, 1)
            graphics.renderItem(costStack, x + 232, y + 50)

            // Render focused field pointer indicators
            if (resultItemBox.isFocused) {
                graphics.drawString(font, "<- Pick Inv Item", x + 130, y + 31, 0x00FF99.toInt(), false)
            } else if (costItemBox.isFocused) {
                graphics.drawString(font, "<- Pick Inv Item", x + 130, y + 79, 0x00FF99.toInt(), false)
            }

            // Render Delete Button
            val delHover = mouseX >= x + 180 && mouseX < x + 250 && mouseY >= y + 90 && mouseY < y + 101
            val delBgColor = if (delHover) 0xFFFF4444.toInt() else 0xFFCC2222.toInt()
            graphics.fill(x + 180, y + 90, x + 250, y + 101, delBgColor)
            val delTxt = "DELETE"
            graphics.drawString(font, delTxt, x + 180 + (70 - font.width(delTxt)) / 2, y + 92, 0xFFFFFF, false)
        } else {
            graphics.drawString(font, "Select an offer", x + 140, y + 50, 0x777777, false)
            graphics.drawString(font, "to edit details.", x + 140, y + 62, 0x777777, false)
        }

        // 5. Draw Player Inventory slot backgrounds manually
        for (slot in menu.slots) {
            val sx = x + slot.x
            val sy = y + slot.y
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF333333.toInt())
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B.toInt())
        }
    }

    override fun renderTooltip(graphics: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(graphics, x, y)
        val rx = leftPos
        val ry = topPos

        // Check if mouse is hovering over an item slot to show tooltips
        val slot = getSlotUnderMouse(x.toDouble(), y.toDouble())
        if (slot != null && slot.hasItem()) {
            graphics.renderTooltip(font, slot.item, x, y)
            return
        }

        // Tooltip for middle item list
        if (isMouseOver(x, y, rx + 52, ry + 16, 64, 110)) {
            val itemsList = getSelectedRegionItemsMutable()
            val totalListHeight = if (itemsList.isEmpty()) 0 else itemsList.size * 22
            val maxScrollY = (totalListHeight - 110).coerceAtLeast(0)
            val currentScrollY = (scrollProgress * maxScrollY).toInt()

            val clickY = y - (ry + 16) + currentScrollY
            val index = clickY / 22
            if (index in itemsList.indices) {
                val item = itemsList[index]
                val itemLocalY = (index * 22) - currentScrollY
                if (isMouseOver(x, y, rx + 52 + 2, ry + 16 + itemLocalY + 2, 16, 16)) {
                    val resultStack = getItemStack(item.resultItem, item.resultCount)
                    graphics.renderTooltip(font, resultStack, x, y)
                } else if (isMouseOver(x, y, rx + 52 + 46, ry + 16 + itemLocalY + 2, 16, 16)) {
                    val costStack = getItemStack(item.costItem, item.costCount)
                    graphics.renderTooltip(font, costStack, x, y)
                }
            }
        }
    }

    private fun getSlotUnderMouse(mx: Double, my: Double): Slot? {
        val rx = leftPos
        val ry = topPos
        for (slot in menu.slots) {
            val sx = rx + slot.x
            val sy = ry + slot.y
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                return slot
            }
        }
        return null
    }

    override fun slotClicked(slot: Slot?, slotId: Int, mouseButton: Int, type: ClickType) {
        if (slot != null) {
            val itemStack = slot.item
            if (!itemStack.isEmpty) {
                val itemId = BuiltInRegistries.ITEM.getKey(itemStack.item).toString()
                if (resultItemBox.isFocused) {
                    resultItemBox.value = itemId
                    minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f
                    ))
                } else if (costItemBox.isFocused) {
                    costItemBox.value = itemId
                    minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f
                    ))
                }
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = leftPos
        val y = topPos

        // 1. Save and Apply Button click
        if (mouseX >= x + 160 && mouseX < x + 250 && mouseY >= y + 2 && mouseY < y + 13) {
            saveAndSync()
            minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f
            ))
            return true
        }

        // 2. Region List click
        val regions = GymRegion.entries
        for (i in regions.indices) {
            val regY = y + 16 + i * 12
            if (mouseX >= x + 3 && mouseX < x + 48 && mouseY >= regY && mouseY < regY + 11) {
                selectedRegion = regions[i]
                scrollProgress = 0f
                selectItem(-1)
                minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
                ))
                return true
            }
        }

        // 3. Shop Items List selection click
        if (isMouseOver(mouseX.toInt(), mouseY.toInt(), x + 52, y + 16, 64, 110)) {
            val itemsList = getSelectedRegionItemsMutable()
            val totalListHeight = if (itemsList.isEmpty()) 0 else itemsList.size * 22
            val maxScrollY = (totalListHeight - 110).coerceAtLeast(0)
            val currentScrollY = (scrollProgress * maxScrollY).toInt()

            val clickY = mouseY.toInt() - (y + 16) + currentScrollY
            val clickedIndex = clickY / 22
            if (clickedIndex in itemsList.indices) {
                selectItem(clickedIndex)
                minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
                ))
                return true
            }
        }

        // 4. Drag scrollbar click
        if (isMouseOver(mouseX.toInt(), mouseY.toInt(), x + 118, y + 16, 4, 110)) {
            isDraggingScroller = true
            updateScroll(mouseY)
            return true
        }

        // 5. Add Offer button click
        if (mouseX >= x + 52 && mouseX < x + 122 && mouseY >= y + 121 && mouseY < y + 130) {
            val list = getSelectedRegionItemsMutable()
            val newItem = GymShop.ShopItemConfig(
                requiredBadge = "",
                costItem = "cobblemongymodyssey:cobble_copper_coin",
                costCount = 1,
                resultItem = "cobblemon:poke_ball",
                resultCount = 1
            )
            list.add(newItem)
            
            // Re-back the list to map
            val updatedShops = configCopy.shops.toMutableMap()
            updatedShops[selectedRegion.name] = GymShop.RegionShopConfig(list)
            configCopy = GymShop.ShopsConfig(updatedShops)

            selectItem(list.size - 1)
            
            // Scroll to the end
            val totalListHeight = list.size * 22
            val maxScrollY = (totalListHeight - 110).coerceAtLeast(0)
            scrollProgress = if (maxScrollY > 0) 1.0f else 0f
            
            minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.1f
            ))
            return true
        }

        // 6. Delete Button click
        if (selectedItemIndex != -1) {
            if (mouseX >= x + 180 && mouseX < x + 250 && mouseY >= y + 90 && mouseY < y + 101) {
                val list = getSelectedRegionItemsMutable()
                if (selectedItemIndex in list.indices) {
                    list.removeAt(selectedItemIndex)
                    
                    // Re-back the list to map
                    val updatedShops = configCopy.shops.toMutableMap()
                    updatedShops[selectedRegion.name] = GymShop.RegionShopConfig(list)
                    configCopy = GymShop.ShopsConfig(updatedShops)

                    selectItem(-1)
                    minecraft?.soundManager?.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.ITEM_BREAK, 1.0f
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
        val list = getSelectedRegionItemsMutable()
        if (list.size > 5) {
            val totalListHeight = list.size * 22
            val maxScrollY = (totalListHeight - 110).coerceAtLeast(0)
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
        val y = topPos + 16
        val relativeY = (mouseY - y).coerceIn(0.0, 110.0)
        val travel = 110 - 10
        val thumbCenterOffset = 5.0
        val progress = ((relativeY - thumbCenterOffset) / travel).coerceIn(0.0, 1.0)
        scrollProgress = progress.toFloat()
    }

    private fun getItemStack(id: String, count: Int): ItemStack {
        val cacheKey = "$id:$count"
        return itemCache.getOrPut(cacheKey) {
            val loc = ResourceLocation.tryParse(id)
            val item = if (loc != null) BuiltInRegistries.ITEM.get(loc) else Items.AIR
            ItemStack(item, count)
        }
    }

    private fun isMouseOver(mx: Int, my: Int, rx: Int, ry: Int, rw: Int, rh: Int): Boolean {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh
    }

    private fun cloneConfig(src: GymShop.ShopsConfig): GymShop.ShopsConfig {
        val json = gson.toJson(src)
        return gson.fromJson(json, GymShop.ShopsConfig::class.java)
    }

    private fun saveAndSync() {
        val json = gson.toJson(configCopy)
        val player = minecraft?.player
        if (player != null) {
            val buf = net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(),
                minecraft?.level?.registryAccess() ?: throw java.lang.IllegalStateException("Registry access not available")
            )
            buf.writeUtf(json)
            NetworkManager.sendToServer(
                ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "save_shop_config"),
                buf
            )
        }
    }
}
