package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.sequence.SequenceStartMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil
import fr.lewon.dofus.bot.util.network.info.FightSequence

object SequenceStartEventHandler : IEventHandler<SequenceStartMessage> {

    override fun onEventReceived(socketResult: SequenceStartMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val newSequence = FightSequence(socketResult.sequenceType, socketResult.authorId)
        val currentSequence = gameInfo.currentSequence
        if (currentSequence.isFinished) {
            gameInfo.currentSequence = newSequence
        } else {
            currentSequence.subSequences.add(newSequence)
        }
    }
}