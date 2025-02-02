package fr.lewon.dofus.bot.sniffer.model.messages.game.context.fight

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class GameFightReadyMessage : NetworkMessage() {
	var isReady: Boolean = false
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		isReady = stream.readBoolean()
	}
	override fun getNetworkMessageId(): Int = 4245
}
