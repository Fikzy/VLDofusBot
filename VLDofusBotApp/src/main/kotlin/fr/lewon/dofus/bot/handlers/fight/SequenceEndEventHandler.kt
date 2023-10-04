package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.sequence.SequenceEndMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object SequenceEndEventHandler : IEventHandler<SequenceEndMessage> {

    override fun onEventReceived(socketResult: SequenceEndMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val ongoingSequence = gameInfo.currentSequence.getOngoingSequence(socketResult.sequenceType)
        ongoingSequence?.isFinished = true
    }
}