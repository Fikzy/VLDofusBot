package fr.lewon.dofus.bot.scripts.tasks.impl.fight

import fr.lewon.dofus.bot.core.d2o.managers.spell.SpellManager
import fr.lewon.dofus.bot.core.logs.LogItem
import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel
import fr.lewon.dofus.bot.core.ui.managers.DofusUIElement
import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.DofusCharacteristics
import fr.lewon.dofus.bot.game.fight.FightBoard
import fr.lewon.dofus.bot.game.fight.ai.FightAI
import fr.lewon.dofus.bot.game.fight.ai.SpellSimulator
import fr.lewon.dofus.bot.game.fight.ai.complements.AIComplement
import fr.lewon.dofus.bot.game.fight.ai.complements.DefaultAIComplement
import fr.lewon.dofus.bot.game.fight.ai.impl.DefaultFightAI
import fr.lewon.dofus.bot.game.fight.operations.CastSpellOperation
import fr.lewon.dofus.bot.game.fight.operations.MoveOperation
import fr.lewon.dofus.bot.game.fight.operations.PassTurnOperation
import fr.lewon.dofus.bot.model.characters.sets.CharacterSetElement
import fr.lewon.dofus.bot.scripts.tasks.BooleanDofusBotTask
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.GameActionAcknowledgementMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightCastOnTargetRequestMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightCastRequestMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.sequence.SequenceStartMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameEntitiesDispositionMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameMapMovementRequestMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.GameFightEndMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.GameFightOptionStateUpdateMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.GameFightTurnEndMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.GameFightTurnStartPlayingMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.challenge.ChallengeModSelectMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.roleplay.MapComplementaryInformationsDataMessage
import fr.lewon.dofus.bot.util.filemanagers.impl.CharacterSetsManager
import fr.lewon.dofus.bot.util.game.DofusColors
import fr.lewon.dofus.bot.util.game.MousePositionsUtil
import fr.lewon.dofus.bot.util.game.MoveUtil
import fr.lewon.dofus.bot.util.game.RetryUtil
import fr.lewon.dofus.bot.util.geometry.PointRelative
import fr.lewon.dofus.bot.util.geometry.RectangleRelative
import fr.lewon.dofus.bot.util.io.*
import fr.lewon.dofus.bot.util.network.info.GameInfo
import fr.lewon.dofus.bot.util.ui.UiUtil
import java.awt.event.KeyEvent

open class FightTask(
    private val aiComplement: AIComplement = DefaultAIComplement(),
    private val teamFight: Boolean = false,
) : BooleanDofusBotTask() {

    companion object {

        private val CHALLENGE_RANDOM = 1

        private val REF_TOP_LEFT_POINT = PointRelative(0.4016129f, 0.88508064f)

        private val REF_CREATURE_MODE_BUTTON_BOUNDS = RectangleRelative.build(
            PointRelative(0.8946648f, 0.96410257f),
            PointRelative(0.90834475f, 0.982906f)
        )

        private val REF_BLOCK_HELP_BUTTON_BOUNDS = RectangleRelative.build(
            PointRelative(0.90697676f, 0.8666667f),
            PointRelative(0.9138167f, 0.88376063f)
        )

        private val REF_RESTRICT_TO_TEAM_BUTTON_BOUNDS = RectangleRelative.build(
            PointRelative(0.94516134f, 0.86693543f),
            PointRelative(0.96129036f, 0.88306457f)
        )

        private val CLOSE_FIGHT_BUTTON_1 = RectangleRelative.build(
            PointRelative(0.9419354f, 0.27620968f),
            PointRelative(0.95645154f, 0.30040324f)
        )

        private val CLOSE_FIGHT_BUTTON_2 = RectangleRelative.build(
            PointRelative(0.716129f, 0.69153225f),
            PointRelative(0.7306452f, 0.7076613f)
        )

        private val MIN_COLOR = DofusColors.HIGHLIGHT_COLOR_MIN
        private val MAX_COLOR = DofusColors.HIGHLIGHT_COLOR_MAX
        private val MIN_COLOR_CROSS = DofusColors.UI_BANNER_BLACK_COLOR_MIN
        private val MAX_COLOR_CROSS = DofusColors.UI_BANNER_BLACK_COLOR_MAX
        private val MIN_COLOR_BG = DofusColors.UI_BANNER_GREY_COLOR_MIN
        private val MAX_COLOR_BG = DofusColors.UI_BANNER_GREY_COLOR_MAX
    }

    private fun getCloseButtonLocation(gameInfo: GameInfo): RectangleRelative? {
        if (ScreenUtil.colorCount(gameInfo, CLOSE_FIGHT_BUTTON_1, MIN_COLOR_CROSS, MAX_COLOR_CROSS) > 0
            && ScreenUtil.colorCount(gameInfo, CLOSE_FIGHT_BUTTON_1, MIN_COLOR_BG, MAX_COLOR_BG) > 0
        ) {
            return CLOSE_FIGHT_BUTTON_1
        }
        if (ScreenUtil.colorCount(gameInfo, CLOSE_FIGHT_BUTTON_2, MIN_COLOR_CROSS, MAX_COLOR_CROSS) > 0
            && ScreenUtil.colorCount(gameInfo, CLOSE_FIGHT_BUTTON_2, MIN_COLOR_BG, MAX_COLOR_BG) > 0
        ) {
            return CLOSE_FIGHT_BUTTON_2
        }
        return null
    }

    private fun getLvlUpCloseButtonBounds(gameInfo: GameInfo): RectangleRelative? {
        val lvlUpUiElements = listOf(
            DofusUIElement.LVL_UP,
            DofusUIElement.LVL_UP_OMEGA,
            DofusUIElement.LVL_UP_WITH_SPELL
        )
        for (uiElement in lvlUpUiElements) {
            val closeButtonBounds = UiUtil.getContainerBounds(uiElement, "btn_close_main")
            if (ScreenUtil.colorCount(gameInfo, closeButtonBounds, MIN_COLOR, MAX_COLOR) != 0) {
                return closeButtonBounds
            }
        }
        return null
    }

    private fun isFightEnded(gameInfo: GameInfo): Boolean {
        return gameInfo.eventStore.getLastEvent(GameFightEndMessage::class.java) != null
            || gameInfo.eventStore.getLastEvent(MapComplementaryInformationsDataMessage::class.java) != null
    }

    override fun doExecute(logItem: LogItem, gameInfo: GameInfo): Boolean {
        val fightBoard = gameInfo.fightBoard
        val dofusBoard = gameInfo.dofusBoard
        initFight(gameInfo)

        val characterSpells = CharacterSetsManager.getSelectedSet(gameInfo.character.name).spells
        val characterSpellBySpellLevelId = HashMap<Int, CharacterSetElement>()
        for (characterSpell in characterSpells) {
            val spell = characterSpell.elementId?.let(SpellManager::getSpell)
            if (spell != null) {
                for (spellLevel in spell.levels) {
                    characterSpellBySpellLevelId[spellLevel.id] = characterSpell
                }
            }
        }

        if (fightBoard.getPlayerFighter()?.spells?.isNotEmpty() != true) {
            error("No spell found for player fighter")
        }

        val ai = getFightAI(dofusBoard, aiComplement)
        selectInitialPosition(gameInfo, fightBoard, ai)
        MouseUtil.leftClick(gameInfo, MousePositionsUtil.getRestPosition(gameInfo))

        gameInfo.eventStore.clear()
        ai.onFightStart(fightBoard)
        KeyboardUtil.sendKey(gameInfo, KeyEvent.VK_F1, 0)
        waitForMessage(gameInfo, GameFightTurnStartPlayingMessage::class.java, 60 * 1000)

        while (!isFightEnded(gameInfo)) {
            ai.onNewTurn()
            val turnLogItem = gameInfo.logger.addSubLog("New turn starting (${ai.currentTurn}) ...", logItem)
            MouseUtil.leftClick(gameInfo, MousePositionsUtil.getRestPosition(gameInfo), 400)
            var nextOperation = ai.getNextOperation(fightBoard, null)
            while (!isFightEnded(gameInfo) && nextOperation != PassTurnOperation) {
                clearOperationsMessages(gameInfo)
                val nextOperationLogItem = gameInfo.logger.addSubLog("Next Operation", turnLogItem)
                logCharacteristics(gameInfo, nextOperationLogItem, fightBoard)
                if (nextOperation is MoveOperation) {
                    val moveCellId = nextOperation.cellIds.last()
                    gameInfo.logger.addSubLog("Moving to cell : $moveCellId", nextOperationLogItem)
                    val moveCell = gameInfo.dofusBoard.getCell(moveCellId)
                    processMove(gameInfo, moveCell)
                } else if (nextOperation is CastSpellOperation) {
                    val spellLevel = nextOperation.spell
                    val characterSpell = characterSpellBySpellLevelId[spellLevel.id]
                        ?: error("No character spell found for spell level : ${spellLevel.id}")
                    val spellLogItem = gameInfo.logger.addSubLog(
                        message = "Casting spell ${spellLevel.spellId} on cell : ${nextOperation.targetCellId}",
                        parent = nextOperationLogItem,
                        subItemCapacity = 30
                    )
                    val expectedDamagesByFighter =
                        getExpectedDamagesByFighter(gameInfo, fightBoard, spellLevel, nextOperation.targetCellId)
                    if (expectedDamagesByFighter.isNotEmpty()) {
                        val damagesLogItem = gameInfo.logger.addSubLog("Expected damages :", spellLogItem)
                        expectedDamagesByFighter.filter { it.value != 0 }.forEach {
                            gameInfo.logger.addSubLog("${it.value} on ${it.key}", damagesLogItem)
                        }
                    }
                    castSpell(gameInfo, characterSpell, gameInfo.dofusBoard.getCell(nextOperation.targetCellId))
                }
                val waitingAcknowledgementLogItem = gameInfo.logger.addSubLog(
                    "Waiting acknowledgment ...",
                    nextOperationLogItem
                )
                if (!waitForMessage(gameInfo, GameActionAcknowledgementMessage::class.java)) {
                    gameInfo.logger.closeLog("KO", waitingAcknowledgementLogItem)
                    error("Action didn't get acknowledged by client")
                }
                gameInfo.logger.closeLog("OK", waitingAcknowledgementLogItem)
                gameInfo.logger.closeLog("OK", nextOperationLogItem)
                if (isFightEnded(gameInfo)) {
                    break
                }
                val calculatingLogItem = gameInfo.logger.addSubLog("Calculating next operation ...", turnLogItem)
                nextOperation = ai.getNextOperation(fightBoard, nextOperation)
                gameInfo.logger.closeLog("OK", calculatingLogItem)
            }
            KeyboardUtil.sendKey(gameInfo, KeyEvent.VK_F1, 0)
            gameInfo.logger.addSubLog("Waiting for turn to end of fight to finish ...", turnLogItem)
            if (!waitForMessage(gameInfo, GameFightTurnEndMessage::class.java)) {
                error("Turn did not end")
            }
            gameInfo.logger.closeLog("OK - Turn finished", turnLogItem)
            gameInfo.logger.addSubLog("Waiting for next turn to start or fight to finish ...", logItem)
            if (!waitForMessage(gameInfo, GameFightTurnStartPlayingMessage::class.java, 3 * 60 * 1000)) {
                error("Next turn did not start")
            }
        }
        gameInfo.logger.addSubLog("Fight ended, waiting until map is loaded ...", logItem)
        MoveUtil.waitForMapChangeFinished(gameInfo)
        gameInfo.fightBoard.resetFighters()

        gameInfo.logger.addSubLog("Closing fight end popups ...", logItem)
        WaitUtil.sleep(800)
        if (!WaitUtil.waitUntil { getCloseButtonLocation(gameInfo) != null || getLvlUpCloseButtonBounds(gameInfo) != null }) {
            error("Close button not found")
        }
        val lvlUpCloseButtonBounds = getLvlUpCloseButtonBounds(gameInfo)
        if (lvlUpCloseButtonBounds != null) {
            RetryUtil.tryUntilSuccess(
                toTry = { MouseUtil.leftClick(gameInfo, lvlUpCloseButtonBounds.getCenter()) },
                successChecker = { WaitUtil.waitUntil(3000) { getLvlUpCloseButtonBounds(gameInfo) == null } },
                tryCount = 3
            )
        }
        MouseUtil.leftClick(gameInfo, MousePositionsUtil.getRestPosition(gameInfo), 500)
        KeyboardUtil.sendKey(gameInfo, KeyEvent.VK_ESCAPE)
        return true
    }

    private fun logCharacteristics(gameInfo: GameInfo, logItem: LogItem, fightBoard: FightBoard) {
        val playerFighter = fightBoard.getPlayerFighter()
            ?: return
        gameInfo.logger.addSubLog("AP : ${DofusCharacteristics.ACTION_POINTS.getValue(playerFighter)}", logItem)
        gameInfo.logger.addSubLog("MP : ${DofusCharacteristics.MOVEMENT_POINTS.getValue(playerFighter)}", logItem)
    }

    private fun getExpectedDamagesByFighter(
        gameInfo: GameInfo,
        fightBoard: FightBoard,
        spellLevel: DofusSpellLevel,
        targetCellId: Int,
    ): Map<Double, Int> {
        val spellSimulator = SpellSimulator(gameInfo.dofusBoard)
        val newFightBoard = fightBoard.deepCopy()
        val playerFighter = newFightBoard.getPlayerFighter()
            ?: return emptyMap()
        spellSimulator.simulateSpell(newFightBoard, playerFighter, spellLevel, targetCellId)
        val initialLostHpByFighterId = fightBoard.getAllFighters(true).associate {
            it.id to it.hpLost
        }
        val newLostHpByFighterId = newFightBoard.getAllFighters(true).associate {
            it.id to it.hpLost
        }
        return initialLostHpByFighterId.keys.associateWith {
            val initialLostHp = initialLostHpByFighterId[it] ?: 0
            val newLostHp = newLostHpByFighterId[it] ?: 0
            newLostHp - initialLostHp
        }
    }

    private fun clearOperationsMessages(gameInfo: GameInfo) {
        gameInfo.eventStore.clear(GameFightTurnStartPlayingMessage::class.java)
        gameInfo.eventStore.clear(SequenceStartMessage::class.java)
        gameInfo.eventStore.clear(GameMapMovementRequestMessage::class.java)
        gameInfo.eventStore.clear(GameActionFightCastOnTargetRequestMessage::class.java)
        gameInfo.eventStore.clear(GameActionFightCastRequestMessage::class.java)
        gameInfo.eventStore.clear(GameActionAcknowledgementMessage::class.java)
    }

    protected open fun getFightAI(dofusBoard: DofusBoard, aiComplement: AIComplement): FightAI {
        return DefaultFightAI(dofusBoard, aiComplement)
    }

    protected open fun selectInitialPosition(gameInfo: GameInfo, fightBoard: FightBoard, ai: FightAI) {
        val playerFighter = fightBoard.getPlayerFighter() ?: error("Player not found")
        ai.selectStartCell(fightBoard)?.takeIf { it != playerFighter.cell }?.let {
            WaitUtil.sleep(500)
            MouseUtil.leftClick(gameInfo, it.getCenter())
        }
    }

    private fun processMove(gameInfo: GameInfo, target: DofusCell) {
        RetryUtil.tryUntilSuccess(
            { MouseUtil.doubleLeftClick(gameInfo, target.getCenter()) },
            {
                WaitUtil.waitUntil(2000) { isMoveRequested(gameInfo) }
            },
            4
        ) ?: error("Couldn't request move to cell : ${target.cellId}")
        waitForSequenceCompleteEnd(gameInfo)
    }

    private fun isMoveRequested(gameInfo: GameInfo): Boolean =
        isFightEnded(gameInfo)
            || gameInfo.eventStore.getLastEvent(SequenceStartMessage::class.java) != null
            || gameInfo.eventStore.getLastEvent(GameMapMovementRequestMessage::class.java) != null

    private fun castSpell(gameInfo: GameInfo, characterSpell: CharacterSetElement, target: DofusCell) {
        RetryUtil.tryUntilSuccess(
            {
                val keyEvent = KeyEvent.getExtendedKeyCodeForChar(characterSpell.key.code)
                KeyboardUtil.sendKey(gameInfo, keyEvent, 300, characterSpell.ctrlModifier)
                MouseUtil.leftClick(gameInfo, target.getCenter())
            },
            {
                WaitUtil.waitUntil(2000) { isSpellCastRequested(gameInfo) }
            },
            4
        ) ?: error(buildSpellErrorMessage(characterSpell, target))
        waitForSequenceCompleteEnd(gameInfo)
    }

    private fun isSpellCastRequested(gameInfo: GameInfo): Boolean =
        isFightEnded(gameInfo)
            || gameInfo.eventStore.getLastEvent(SequenceStartMessage::class.java) != null
            || gameInfo.eventStore.getLastEvent(GameActionFightCastOnTargetRequestMessage::class.java) != null
            || gameInfo.eventStore.getLastEvent(GameActionFightCastRequestMessage::class.java) != null

    private fun buildSpellErrorMessage(characterSpell: CharacterSetElement, target: DofusCell): String {
        val spellName = characterSpell.elementId?.let(SpellManager::getSpell)?.name
        return "Couldn't cast spell [$spellName] on cell [${target.cellId}]"
    }

    private fun waitForSequenceCompleteEnd(gameInfo: GameInfo): Boolean = WaitUtil.waitUntil(10000) {
        isFightEnded(gameInfo) || isSequenceComplete(gameInfo)
    }

    private fun isSequenceComplete(gameInfo: GameInfo): Boolean {
        val currentSequence = gameInfo.currentSequence
        return gameInfo.eventStore.isAllEventsPresent(
            SequenceStartMessage::class.java,
            GameActionAcknowledgementMessage::class.java
        ) && (currentSequence.isFinished && currentSequence.fighterId == gameInfo.playerId)
    }

    private fun waitForMessage(
        gameInfo: GameInfo,
        eventClass: Class<out NetworkMessage>,
        timeOutMillis: Int = WaitUtil.DEFAULT_TIMEOUT_MILLIS,
    ): Boolean = WaitUtil.waitUntil(timeOutMillis) {
        isFightEnded(gameInfo) || gameInfo.eventStore.getLastEvent(eventClass) != null
    }

    private fun initFight(gameInfo: GameInfo) {
        val uiPoint = DofusUIElement.BANNER.getPosition(true)
        val uiPointRelative = uiPoint.toPointRelative()
        val deltaTopLeftPoint = REF_TOP_LEFT_POINT.opposite().getSum(uiPointRelative)
        val creatureModeBounds = REF_CREATURE_MODE_BUTTON_BOUNDS.getTranslation(deltaTopLeftPoint)
        val blockHelpBounds = REF_BLOCK_HELP_BUTTON_BOUNDS.getTranslation(deltaTopLeftPoint)
        val restrictToTeamBounds = REF_RESTRICT_TO_TEAM_BUTTON_BOUNDS.getTranslation(deltaTopLeftPoint)

        val fightOptionsReceived = WaitUtil.waitUntil {
            getBlockHelpOptionValue(gameInfo) != null && getRestrictToTeamOptionValue(gameInfo) != null
        }
        if (!fightOptionsReceived) {
            error("Fight option values not received")
        }

        WaitUtil.waitUntil(15000) {
            gameInfo.fightBoard.getPlayerFighter() != null && gameInfo.fightBoard.getEnemyFighters().isNotEmpty()
        }
        gameInfo.updatePlayerFighter()
        WaitUtil.sleep(800)

        if (getBlockHelpOptionValue(gameInfo) == teamFight) {
            MouseUtil.leftClick(gameInfo, blockHelpBounds.getCenter())
        }
        if (teamFight && getRestrictToTeamOptionValue(gameInfo) == true) {
            MouseUtil.leftClick(gameInfo, restrictToTeamBounds.getCenter())
        }
        if (!gameInfo.isCreatureModeToggled) {
            if (!WaitUtil.waitUntil(1000) { isCreatureModeActive(gameInfo, creatureModeBounds) }) {
                MouseUtil.leftClick(gameInfo, creatureModeBounds.getCenter())
            }
            gameInfo.isCreatureModeToggled = true
        }

        if (WaitUtil.waitUntil(2000) { gameInfo.eventStore.getLastEvent(ChallengeModSelectMessage::class.java) != null }) {
            val challengeMessage = gameInfo.eventStore.getLastEvent(ChallengeModSelectMessage::class.java)
            if (challengeMessage != null && challengeMessage.challengeMod != CHALLENGE_RANDOM) {
                MouseUtil.leftClick(gameInfo, MousePositionsUtil.getRestPosition(gameInfo))
                KeyboardUtil.sendKey(gameInfo, KeyEvent.VK_F1, 500)
            }
        }

        gameInfo.eventStore.clearUntilLast(GameEntitiesDispositionMessage::class.java)
    }

    private fun isCreatureModeActive(gameInfo: GameInfo, creatureModeBounds: RectangleRelative): Boolean {
        return ScreenUtil.colorCount(gameInfo, creatureModeBounds, MIN_COLOR, MAX_COLOR) > 0
    }

    private fun getBlockHelpOptionValue(gameInfo: GameInfo): Boolean? {
        return getFightOptionValue(gameInfo, 2)
    }

    private fun getRestrictToTeamOptionValue(gameInfo: GameInfo): Boolean? {
        return getFightOptionValue(gameInfo, 1)
    }

    private fun getFightOptionValue(gameInfo: GameInfo, option: Int): Boolean? {
        return gameInfo.eventStore.getLastEvent(
            GameFightOptionStateUpdateMessage::class.java
        ) { it.option == option }?.state
    }

    override fun onStarted(): String {
        return "Fight started"
    }
}