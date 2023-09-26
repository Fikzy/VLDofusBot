package fr.lewon.dofus.bot.sniffer.model.messages.connection.register

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class NicknameChoiceRequestMessage : NetworkMessage() {
	var nickname: String = ""
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		nickname = stream.readUTF()
	}
	override fun getNetworkMessageId(): Int = 4355
}
