package fr.lewon.dofus.bot.sniffer.model.messages.game.context

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class GameContextRemoveMultipleElementsWithEventsMessage : GameContextRemoveMultipleElementsMessage() {
	var elementEventIds: ArrayList<Int> = ArrayList()
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		elementEventIds = ArrayList()
		for (i in 0 until stream.readUnsignedShort().toInt()) {
			val item = stream.readUnsignedByte().toInt()
			elementEventIds.add(item)
		}
	}
	override fun getNetworkMessageId(): Int = 4936
}
