package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.core.model.spell.DofusSpellEffectType
import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightDispellableEffectMessage
import fr.lewon.dofus.bot.sniffer.model.types.game.actions.fight.FightTemporaryBoostStateEffect
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameActionFightDispellableEffectEventHandler : IEventHandler<GameActionFightDispellableEffectMessage> {

    override fun onEventReceived(socketResult: GameActionFightDispellableEffectMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val fighter = gameInfo.fightBoard.getOrCreateFighterById(socketResult.effect.targetId)
        if (socketResult.actionId == DofusSpellEffectType.ADD_STATE.id) {
            val effect = socketResult.effect as FightTemporaryBoostStateEffect
            fighter.addStateBuff(
                uid = effect.uid.toString(),
                turnDuration = effect.turnDuration,
                stateId = effect.stateId
            )
        }
    }
}