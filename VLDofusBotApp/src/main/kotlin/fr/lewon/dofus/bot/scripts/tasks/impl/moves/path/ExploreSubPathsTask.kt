package fr.lewon.dofus.bot.scripts.tasks.impl.moves.path

import fr.lewon.dofus.bot.gui.main.exploration.lastexploration.LastExplorationUiUtil
import fr.lewon.dofus.bot.model.characters.DofusCharacter
import fr.lewon.dofus.bot.model.characters.paths.SubPath
import fr.lewon.dofus.bot.scripts.tasks.impl.moves.MultipleExplorationTask
import fr.lewon.dofus.bot.scripts.tasks.impl.moves.SingleExplorationTask
import fr.lewon.dofus.bot.scripts.tasks.impl.moves.util.ExplorationParameters

class ExploreSubPathsTask(
    subPaths: List<SubPath>,
    explorationParameters: ExplorationParameters,
) : MultipleExplorationTask<SubPath>(subPaths, explorationParameters) {

    override fun getItemById(itemsToExplore: List<SubPath>, id: String): SubPath? =
        itemsToExplore.firstOrNull { it.id == id }

    override fun onExplorationStart(character: DofusCharacter, itemsToExplore: List<SubPath>) =
        LastExplorationUiUtil.onExplorationStart(character, itemsToExplore)

    override fun buildSingleExplorationTask(
        item: SubPath,
        explorationParameters: ExplorationParameters
    ): SingleExplorationTask<SubPath> =
        ExploreSubPathTask(subPath = item, explorationParameters = explorationParameters)

    override fun buildOnStartedMessage(itemsToExplore: List<SubPath>): String =
        "Exploring sub paths [${itemsToExplore.joinToString(", ") { it.displayName }})]"

}