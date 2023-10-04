package fr.lewon.dofus.bot.scripts.tasks.impl.inventory

import fr.lewon.dofus.bot.core.logs.LogItem
import fr.lewon.dofus.bot.core.ui.managers.DofusUIElement
import fr.lewon.dofus.bot.scripts.tasks.BooleanDofusBotTask
import fr.lewon.dofus.bot.sniffer.model.messages.game.inventory.items.InventoryWeightMessage
import fr.lewon.dofus.bot.util.game.MousePositionsUtil
import fr.lewon.dofus.bot.util.geometry.PointRelative
import fr.lewon.dofus.bot.util.io.KeyboardUtil
import fr.lewon.dofus.bot.util.io.MouseUtil
import fr.lewon.dofus.bot.util.io.WaitUtil
import fr.lewon.dofus.bot.util.network.info.GameInfo
import fr.lewon.dofus.bot.util.ui.UiUtil

class UseItemsInInventoryTask(private val searchName: String) : BooleanDofusBotTask() {

    override fun doExecute(logItem: LogItem, gameInfo: GameInfo): Boolean {
        MouseUtil.leftClick(gameInfo, MousePositionsUtil.getRestPosition(gameInfo))
        KeyboardUtil.sendKey(gameInfo, 'I')
        if (!WaitUtil.waitUntil { UiUtil.isUiElementWindowOpened(gameInfo, DofusUIElement.INVENTORY) }) {
            error("Failed to open inventory. Do you use the default 'i' keybinding ?")
        }
        MouseUtil.leftClick(gameInfo, UiUtil.getContainerBounds(DofusUIElement.INVENTORY, "btnConsumables").getCenter())
        MouseUtil.leftClick(gameInfo, UiUtil.getContainerBounds(DofusUIElement.INVENTORY, "searchInput2").getCenter())
        KeyboardUtil.writeKeyboard(gameInfo, searchName, 500)
        val itemsGridBounds = UiUtil.getContainerBounds(DofusUIElement.INVENTORY, "grid")
        val clickLocation = itemsGridBounds.getTopLeft().getSum(PointRelative(0.01f, 0.01f))
        do {
            gameInfo.eventStore.clear()
            MouseUtil.doubleLeftClick(gameInfo, clickLocation, sleepTime = 600)
        } while (hasItemBeenUsed(gameInfo))
        UiUtil.closeWindow(gameInfo, DofusUIElement.INVENTORY)
        return true
    }

    private fun hasItemBeenUsed(gameInfo: GameInfo): Boolean = WaitUtil.waitUntil(2000) {
        gameInfo.eventStore.getLastEvent(InventoryWeightMessage::class.java) != null
    }

    override fun onStarted(): String {
        return "Using items in inventory : $searchName ..."
    }
}