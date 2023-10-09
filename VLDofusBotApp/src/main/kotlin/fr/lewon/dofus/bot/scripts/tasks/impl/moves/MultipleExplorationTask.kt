package fr.lewon.dofus.bot.scripts.tasks.impl.moves

import fr.lewon.dofus.bot.core.logs.LogItem
import fr.lewon.dofus.bot.gui.main.exploration.lastexploration.LastExplorationUiUtil
import fr.lewon.dofus.bot.model.characters.DofusCharacter
import fr.lewon.dofus.bot.scripts.tasks.BooleanDofusBotTask
import fr.lewon.dofus.bot.scripts.tasks.impl.moves.util.ExplorationParameters
import fr.lewon.dofus.bot.util.filemanagers.impl.ExplorationRecordManager
import fr.lewon.dofus.bot.util.network.info.GameInfo

abstract class MultipleExplorationTask<T>(
    private val itemsToExplore: List<T>,
    private val explorationParameters: ExplorationParameters
) : BooleanDofusBotTask() {

    override fun doExecute(logItem: LogItem, gameInfo: GameInfo): Boolean {
        val runForever = explorationParameters.runForever
        val itemToResumeWith = getItemToResumeWith()
        var skipExploration = itemToResumeWith != null
        do {
            val explorationTasks = itemsToExplore.map { buildSingleExplorationTask(it, explorationParameters) }
            onExplorationStart(gameInfo.character, itemsToExplore)
            for (explorationTask in explorationTasks) {
                if (skipExploration) {
                    skipExploration = itemToResumeWith != explorationTask.itemToExplore
                }
                if (!skipExploration) {
                    when (explorationTask.run(logItem, gameInfo)) {
                        ExplorationStatus.FoundSomething -> return true
                        ExplorationStatus.NotFinished -> return false
                        else -> Unit
                    }
                }
                LastExplorationUiUtil.updateExplorationProgress(gameInfo.character, explorationTask.itemToExplore, 1, 1)
            }
            if (runForever) {
                clearExploredMaps(explorationTasks)
            }
        } while (runForever)
        return true
    }

    private fun getItemToResumeWith(): T? {
        if (explorationParameters.itemIdToResumeOn.isBlank()) {
            return null
        }
        return getItemById(itemsToExplore, explorationParameters.itemIdToResumeOn)
    }

    protected abstract fun getItemById(itemsToExplore: List<T>, id: String): T?

    private fun clearExploredMaps(explorationTasks: List<SingleExplorationTask<T>>) {
        val mapIds = explorationTasks.flatMap { it.getMapsToExplore() }.map { it.id }
        ExplorationRecordManager.clearExploreMaps(mapIds)
    }

    protected abstract fun onExplorationStart(character: DofusCharacter, itemsToExplore: List<T>)

    protected abstract fun buildSingleExplorationTask(
        item: T,
        explorationParameters: ExplorationParameters
    ): SingleExplorationTask<T>

    protected abstract fun buildOnStartedMessage(itemsToExplore: List<T>): String

    override fun onStarted(): String = buildOnStartedMessage(itemsToExplore)
}