package fr.lewon.dofus.bot.sniffer.model.messages.game.context

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class GameMapNoMovementMessage : NetworkMessage() {
	var cellX: Int = 0
	var cellY: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		cellX = stream.readUnsignedShort().toInt()
		cellY = stream.readUnsignedShort().toInt()
	}
	override fun getNetworkMessageId(): Int = 8603
}
