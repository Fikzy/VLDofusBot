package fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight.challenge

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class ChallengeBonusChoiceSelectedMessage : NetworkMessage() {
	var challengeBonus: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		challengeBonus = stream.readUnsignedByte().toInt()
	}
	override fun getNetworkMessageId(): Int = 593
}
