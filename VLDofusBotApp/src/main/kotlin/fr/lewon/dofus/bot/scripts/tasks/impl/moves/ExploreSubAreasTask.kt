package fr.lewon.dofus.bot.scripts.tasks.impl.moves

import fr.lewon.dofus.bot.core.logs.LogItem
import fr.lewon.dofus.bot.core.model.maps.DofusSubArea
import fr.lewon.dofus.bot.gui.main.exploration.lastexploration.LastExplorationUiUtil
import fr.lewon.dofus.bot.scripts.tasks.BooleanDofusBotTask
import fr.lewon.dofus.bot.util.network.info.GameInfo

class ExploreSubAreasTask(
    private val subAreas: List<DofusSubArea>,
    private val killEverything: Boolean,
    private val searchedMonsterName: String,
    private val stopWhenArchMonsterFound: Boolean,
    private val stopWhenWantedMonsterFound: Boolean,
    private val itemIdsToHarvest: List<Double>,
    private val runForever: Boolean,
    private val explorationThresholdMinutes: Int
) : BooleanDofusBotTask() {

    override fun doExecute(logItem: LogItem, gameInfo: GameInfo): Boolean {
        do {
            LastExplorationUiUtil.onExplorationStart(gameInfo.character, subAreas)
            for (subArea in subAreas) {
                when (exploreSubArea(logItem, gameInfo, subArea)) {
                    ExplorationStatus.FoundSomething -> return true
                    ExplorationStatus.NoMapExplored -> return false
                    else -> {}
                }
            }
        } while (runForever)
        LastExplorationUiUtil.finishExploration(gameInfo.character)
        return true
    }

    private fun exploreSubArea(logItem: LogItem, gameInfo: GameInfo, subArea: DofusSubArea) = ExploreSubAreaTask(
        subArea = subArea,
        killEverything = killEverything,
        searchedMonsterName = searchedMonsterName,
        stopWhenArchMonsterFound = stopWhenArchMonsterFound,
        stopWhenWantedMonsterFound = stopWhenWantedMonsterFound,
        itemIdsToHarvest = itemIdsToHarvest,
        explorationThresholdMinutes = explorationThresholdMinutes,
    ).run(logItem, gameInfo)

    override

    fun onStarted(): String {
        return "Exploring sub areas [${subAreas.joinToString(", ") { it.label }})]"
    }

}