package fr.lewon.dofus.bot.sniffer.model.messages.game.character.choice

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class CharacterSelectedForceMessage : NetworkMessage() {
	var id: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		id = stream.readInt().toInt()
	}
	override fun getNetworkMessageId(): Int = 1675
}
