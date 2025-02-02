package fr.lewon.dofus.bot.sniffer.model.messages.game.context

import fr.lewon.dofus.bot.sniffer.model.types.game.context.IdentifiedEntityDispositionInformations
import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class GameEntitiesDispositionMessage : NetworkMessage() {
	var dispositions: ArrayList<IdentifiedEntityDispositionInformations> = ArrayList()
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		dispositions = ArrayList()
		for (i in 0 until stream.readUnsignedShort().toInt()) {
			val item = IdentifiedEntityDispositionInformations()
			item.deserialize(stream)
			dispositions.add(item)
		}
	}
	override fun getNetworkMessageId(): Int = 5409
}
