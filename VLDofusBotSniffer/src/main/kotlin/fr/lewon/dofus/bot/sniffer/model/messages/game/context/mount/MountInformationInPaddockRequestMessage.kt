package fr.lewon.dofus.bot.sniffer.model.messages.game.context.mount

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class MountInformationInPaddockRequestMessage : NetworkMessage() {
	var mapRideId: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		mapRideId = stream.readVarInt().toInt()
	}
	override fun getNetworkMessageId(): Int = 306
}
