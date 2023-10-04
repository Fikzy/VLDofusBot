package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightDispellEffectMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameActionFightDispellEffectEventHandler : IEventHandler<GameActionFightDispellEffectMessage> {

    override fun onEventReceived(socketResult: GameActionFightDispellEffectMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val fighter = gameInfo.fightBoard.getOrCreateFighterById(socketResult.targetId)
        fighter.stateBuffs.remove(socketResult.boostUID.toString())
    }
}