package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameEntitiesDispositionMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameEntitiesDispositionEventHandler : IEventHandler<GameEntitiesDispositionMessage> {

    override fun onEventReceived(socketResult: GameEntitiesDispositionMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        for (disposition in socketResult.dispositions) {
            val fighter = gameInfo.fightBoard.getOrCreateFighterById(disposition.id)
            gameInfo.fightBoard.move(fighter, disposition.cellId)
        }
    }

}