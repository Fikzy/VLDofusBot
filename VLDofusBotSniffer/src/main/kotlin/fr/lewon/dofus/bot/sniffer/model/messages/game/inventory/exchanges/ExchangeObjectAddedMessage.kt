package fr.lewon.dofus.bot.sniffer.model.messages.game.inventory.exchanges

import fr.lewon.dofus.bot.sniffer.model.types.game.data.items.ObjectItem
import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class ExchangeObjectAddedMessage : ExchangeObjectMessage() {
	lateinit var obj: ObjectItem
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		obj = ObjectItem()
		obj.deserialize(stream)
	}
	override fun getNetworkMessageId(): Int = 3343
}
