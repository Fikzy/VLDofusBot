package fr.lewon.dofus.bot.scripts.tasks.impl.moves.subarea

import fr.lewon.dofus.bot.core.model.maps.DofusSubArea
import fr.lewon.dofus.bot.gui.main.exploration.lastexploration.LastExplorationUiUtil
import fr.lewon.dofus.bot.model.characters.DofusCharacter
import fr.lewon.dofus.bot.scripts.tasks.impl.moves.MultipleExplorationTask
import fr.lewon.dofus.bot.scripts.tasks.impl.moves.SingleExplorationTask
import fr.lewon.dofus.bot.scripts.tasks.impl.moves.util.ExplorationParameters

class ExploreSubAreasTask(
    subAreas: List<DofusSubArea>,
    explorationParameters: ExplorationParameters,
) : MultipleExplorationTask<DofusSubArea>(subAreas, explorationParameters) {

    override fun getItemById(itemsToExplore: List<DofusSubArea>, id: String): DofusSubArea? =
        itemsToExplore.firstOrNull { it.id.toString() == id }

    override fun onExplorationStart(character: DofusCharacter, itemsToExplore: List<DofusSubArea>) =
        LastExplorationUiUtil.onExplorationStart(character, itemsToExplore)

    override fun buildSingleExplorationTask(
        item: DofusSubArea,
        explorationParameters: ExplorationParameters
    ): SingleExplorationTask<DofusSubArea> =
        ExploreSubAreaTask(subArea = item, explorationParameters = explorationParameters)

    override fun buildOnStartedMessage(itemsToExplore: List<DofusSubArea>): String =
        "Exploring sub areas [${itemsToExplore.joinToString(", ") { it.label }})]"

}