package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.GameFightTurnStartPlayingMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameFightTurnStartPlayingEventHandler : IEventHandler<GameFightTurnStartPlayingMessage> {

    override fun onEventReceived(socketResult: GameFightTurnStartPlayingMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        gameInfo.fightBoard.triggerNewTurn()
    }
}