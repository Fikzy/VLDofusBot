package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.core.model.spell.DofusSpellEffectType
import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightExchangePositionsMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameActionFightExchangePositionsEventHandler : IEventHandler<GameActionFightExchangePositionsMessage> {

    override fun onEventReceived(socketResult: GameActionFightExchangePositionsMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val isRollback = gameInfo.currentSequence.spellLevelsStarted.flatMap { it.effects }.any {
            it.effectType == DofusSpellEffectType.ROLLBACK_PREVIOUS_POSITION
        }
        val caster = gameInfo.fightBoard.getFighter(socketResult.casterCellId)
        val target = gameInfo.fightBoard.getFighter(socketResult.targetCellId)
        val isCasterRolledBack = isRollback && socketResult.targetId != caster?.id
        val isTargetRolledBack = isRollback && socketResult.targetId != target?.id
        caster?.let { gameInfo.fightBoard.move(it, socketResult.targetCellId, isCasterRolledBack) }
        target?.let { gameInfo.fightBoard.move(it, socketResult.casterCellId, isTargetRolledBack) }
    }
}