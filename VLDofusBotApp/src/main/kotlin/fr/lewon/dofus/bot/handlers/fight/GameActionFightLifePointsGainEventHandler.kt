package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightLifePointsGainMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameActionFightLifePointsGainEventHandler : IEventHandler<GameActionFightLifePointsGainMessage> {

    override fun onEventReceived(socketResult: GameActionFightLifePointsGainMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val fighter = gameInfo.fightBoard.getOrCreateFighterById(socketResult.targetId)
        fighter.hpHealed += socketResult.delta
    }

}