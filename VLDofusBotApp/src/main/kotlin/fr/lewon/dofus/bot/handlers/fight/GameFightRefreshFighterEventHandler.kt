package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.character.GameFightRefreshFighterMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameFightRefreshFighterEventHandler : IEventHandler<GameFightRefreshFighterMessage> {

    override fun onEventReceived(socketResult: GameFightRefreshFighterMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val fighter = gameInfo.fightBoard.getOrCreateFighterById(socketResult.informations.contextualId)
        gameInfo.fightBoard.move(fighter, socketResult.informations.disposition.cellId)
    }
}