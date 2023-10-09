package fr.lewon.dofus.bot.scripts.tasks.impl.moves.util

data class ExplorationParameters(
    val killEverything: Boolean,
    val maxMonsterGroupLevel: Int,
    val maxMonsterGroupSize: Int,
    val searchedMonsterName: String,
    val stopWhenArchMonsterFound: Boolean,
    val stopWhenWantedMonsterFound: Boolean,
    val useZaaps: Boolean,
    val explorationThresholdMinutes: Int,
    val itemIdToResumeOn: String,
    val runForever: Boolean
)