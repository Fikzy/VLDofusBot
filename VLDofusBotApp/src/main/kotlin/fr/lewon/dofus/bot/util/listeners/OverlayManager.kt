package fr.lewon.dofus.bot.util.listeners

import fr.lewon.dofus.bot.core.utils.LockUtils.executeSyncOperation
import fr.lewon.dofus.bot.gui.main.characters.CharactersUIUtil
import fr.lewon.dofus.bot.util.filemanagers.impl.CharacterManager
import fr.lewon.dofus.bot.util.filemanagers.impl.GlobalConfigManager
import fr.lewon.dofus.bot.util.network.GameSnifferUtil
import fr.lewon.dofus.bot.util.network.info.GameInfo
import java.util.concurrent.locks.ReentrantLock

object OverlayManager {

    private var displayedOverlay: OverlayInfo? = null
    private val lock = ReentrantLock()

    fun toggleOverlay(toToggleOverlay: OverlayInfo) {
        Thread {
            lock.executeSyncOperation {
                doToggleOverlay(toToggleOverlay)
            }
        }.start()
    }

    private fun doToggleOverlay(toToggleOverlay: OverlayInfo) {
        if (toToggleOverlay == displayedOverlay) {
            toToggleOverlay.overlay.isVisible = false
            displayedOverlay = null
        } else if (GlobalConfigManager.readConfig().run { displayOverlays && shouldDisplayOverlay(toToggleOverlay) }) {
            val gameInfo = getSelectedCharacterGameInfo()
            if (gameInfo == null) {
                println("Select exactly one character to display an overlay")
            } else {
                toToggleOverlay.overlay.updateOverlay(gameInfo)
                displayedOverlay?.overlay?.isVisible = false
                toToggleOverlay.overlay.isVisible = true
                displayedOverlay = toToggleOverlay
            }
        }
    }

    private fun getSelectedCharacterGameInfo(): GameInfo? {
        val characterUIStates = CharactersUIUtil.getSelectedCharactersUIStates()
        val characterName = characterUIStates.firstOrNull()?.name
            ?: return null
        val character = CharacterManager.getCharacter(characterName)
            ?: return null
        val connection = GameSnifferUtil.getFirstConnection(character)
            ?: return null
        return GameSnifferUtil.getGameInfoByConnection(connection)
    }

    @Synchronized
    fun updateDisplayedOverlay(gameInfo: GameInfo) {
        try {
            if (gameInfo == getSelectedCharacterGameInfo()) {
                displayedOverlay?.overlay?.updateOverlay(gameInfo)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}