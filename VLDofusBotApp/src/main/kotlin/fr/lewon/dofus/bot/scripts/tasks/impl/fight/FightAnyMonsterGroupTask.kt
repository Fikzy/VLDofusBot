package fr.lewon.dofus.bot.scripts.tasks.impl.fight

import fr.lewon.dofus.bot.core.logs.LogItem
import fr.lewon.dofus.bot.scripts.tasks.BooleanDofusBotTask
import fr.lewon.dofus.bot.scripts.tasks.exceptions.DofusBotTaskFatalException
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameContextDestroyMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameMapMovementConfirmMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameMapMovementRequestMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.roleplay.fight.GameRolePlayAttackMonsterRequestMessage
import fr.lewon.dofus.bot.util.game.InteractiveUtil
import fr.lewon.dofus.bot.util.game.MousePositionsUtil
import fr.lewon.dofus.bot.util.game.RetryUtil
import fr.lewon.dofus.bot.util.io.MouseUtil
import fr.lewon.dofus.bot.util.io.WaitUtil
import fr.lewon.dofus.bot.util.network.info.GameInfo

class FightAnyMonsterGroupTask(
    private val fightArchmonsters: Boolean = false,
    private val fightQuestMonsters: Boolean = false,
    private val tryUntilFightStarted: Boolean = false,
    private val maxMonsterGroupLevel: Int = 0,
    private val maxMonsterGroupSize: Int = 0
) : BooleanDofusBotTask() {

    override fun doExecute(logItem: LogItem, gameInfo: GameInfo): Boolean {
        gameInfo.eventStore.clear()
        val validMonsterEntityIds = gameInfo.monsterInfoByEntityId.filter { entry ->
            val mainMonster = gameInfo.mainMonstersByGroupOnMap[entry.value]
            val groupLevel = entry.value.staticInfos.underlings.sumOf { monster -> monster.level } +
                entry.value.staticInfos.mainCreatureLightInfos.level
            val groupSize = entry.value.staticInfos.underlings.size + 1
            mainMonster != null
                && gameInfo.entityPositionsOnMapByEntityId[entry.key] != gameInfo.entityPositionsOnMapByEntityId[gameInfo.playerId]
                && (!mainMonster.isMiniBoss || fightArchmonsters)
                && (!mainMonster.isQuestMonster || fightQuestMonsters)
                && (maxMonsterGroupLevel <= 0 || groupLevel <= maxMonsterGroupLevel)
                && (maxMonsterGroupSize <= 0 || groupSize <= maxMonsterGroupSize)
        }.keys.toList()
        if (validMonsterEntityIds.isEmpty()) {
            return false
        }
        val couldStartFight = RetryUtil.tryUntilSuccess(
            function = { tryToStartFight(gameInfo, logItem, validMonsterEntityIds) },
            toCallAfterFail = {
                if (tryUntilFightStarted) {
                    WaitUtil.waitUntil(60000) { gameInfo.monsterInfoByEntityId.isNotEmpty() }
                }
            },
            totalTries = 4,
        )
        if (!couldStartFight) {
            return false
        }
        if (!FightTask().run(logItem, gameInfo)) {
            throw DofusBotTaskFatalException("Failed to fight")
        }
        return true
    }

    private fun tryToStartFight(gameInfo: GameInfo, logItem: LogItem, monsterEntityIds: List<Double>): Boolean {
        val fightAttemptLogItem = gameInfo.logger.addSubLog("Trying to fight a monster ...", logItem)
        val monsterEntityId = getMonstersToFight(gameInfo, monsterEntityIds, fightAttemptLogItem)
        if (monsterEntityId == null
            || !sendClickToMonsterGroup(gameInfo, fightAttemptLogItem, monsterEntityId)
            || !reachMonsterGroup(gameInfo, fightAttemptLogItem)
            || !requestFightAgainstMonstersGroup(gameInfo, fightAttemptLogItem)
            || !waitUntilFightStarts(gameInfo, fightAttemptLogItem)
        ) {
            gameInfo.logger.closeLog("KO", fightAttemptLogItem)
            return false
        }
        gameInfo.logger.closeLog("OK", fightAttemptLogItem)
        return true
    }

    private fun getMonstersToFight(
        gameInfo: GameInfo,
        monsterEntityIds: List<Double>,
        logItem: LogItem
    ): Double? {
        val findingMonsterLogItem = gameInfo.logger.addSubLog("Finding closest monsters group ...", logItem)
        val playerCellId = gameInfo.entityPositionsOnMapByEntityId[gameInfo.playerId]
            ?: throw DofusBotTaskFatalException("Unexpected error : couldn't find player position. You probably will have to reconnect your character.")
        val distanceByEntityId = HashMap<Double, Int>()
        for (entityId in monsterEntityIds) {
            val cellId = gameInfo.entityPositionsOnMapByEntityId[entityId] ?: continue
            val distance = gameInfo.dofusBoard.getPathLength(playerCellId, cellId) ?: continue
            distanceByEntityId[entityId] = distance
        }
        val monsterEntityId = distanceByEntityId.minByOrNull { it.value }?.key
        if (monsterEntityId == null) {
            gameInfo.logger.closeLog("KO", findingMonsterLogItem)
            return null
        }
        gameInfo.logger.closeLog(monsterEntityId.toString(), findingMonsterLogItem)
        return monsterEntityId
    }

    private fun sendClickToMonsterGroup(gameInfo: GameInfo, logItem: LogItem, monsterEntityId: Double): Boolean {
        val clickingMonstersLogItem = gameInfo.logger.addSubLog("Clicking on monsters ...", logItem)
        val monsterCellId = gameInfo.entityPositionsOnMapByEntityId[monsterEntityId] ?: return false
        val clickPosition = InteractiveUtil.getCellClickPosition(gameInfo, monsterCellId, false)
        MouseUtil.leftClick(gameInfo, MousePositionsUtil.getRestPosition(gameInfo))
        MouseUtil.leftClick(gameInfo, clickPosition)
        if (!WaitUtil.waitUntil(2000) {
                gameInfo.eventStore.getLastEvent(GameRolePlayAttackMonsterRequestMessage::class.java) != null
                    || gameInfo.eventStore.getLastEvent(GameMapMovementRequestMessage::class.java) != null
            }) {
            gameInfo.logger.closeLog("KO", clickingMonstersLogItem)
            return false
        }
        gameInfo.logger.closeLog("OK", clickingMonstersLogItem)
        return true
    }

    private fun reachMonsterGroup(gameInfo: GameInfo, logItem: LogItem): Boolean {
        val reachingMonstersLogItem = gameInfo.logger.addSubLog("Reaching monsters ...", logItem)
        if (!WaitUtil.waitUntil(8000) {
                gameInfo.eventStore.getLastEvent(GameRolePlayAttackMonsterRequestMessage::class.java) != null
                    || gameInfo.eventStore.getLastEvent(GameMapMovementConfirmMessage::class.java) != null
            }) {
            gameInfo.logger.closeLog("KO", reachingMonstersLogItem)
            return false
        }
        gameInfo.logger.closeLog("OK", reachingMonstersLogItem)
        return true
    }

    private fun requestFightAgainstMonstersGroup(gameInfo: GameInfo, logItem: LogItem): Boolean {
        val fightingMonstersLogItem = gameInfo.logger.addSubLog("Starting fight against monsters ...", logItem)
        if (!WaitUtil.waitUntil(1000) { gameInfo.eventStore.getLastEvent(GameRolePlayAttackMonsterRequestMessage::class.java) != null }) {
            gameInfo.logger.closeLog("KO", fightingMonstersLogItem)
            return false
        }
        gameInfo.logger.closeLog("OK", fightingMonstersLogItem)
        return true
    }

    private fun waitUntilFightStarts(gameInfo: GameInfo, logItem: LogItem): Boolean {
        val waitingForFightToStartLogItem = gameInfo.logger.addSubLog("Waiting for fight to start ...", logItem)
        if (!WaitUtil.waitUntil(4000) { gameInfo.eventStore.getLastEvent(GameContextDestroyMessage::class.java) != null }) {
            gameInfo.logger.closeLog("KO", waitingForFightToStartLogItem)
            return false
        }
        gameInfo.logger.closeLog("OK", waitingForFightToStartLogItem)
        return true
    }

    override fun onStarted(): String {
        return "Fighting any monster group ... "
    }
}