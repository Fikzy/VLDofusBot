package fr.lewon.dofus.bot.sniffer.model.messages.game.interactive.zaap

import fr.lewon.dofus.bot.sniffer.model.types.game.interactive.zaap.TeleportDestination
import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class ZaapDestinationsMessage : TeleportDestinationsMessage() {
	var spawnMapId: Double = 0.0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		spawnMapId = stream.readDouble().toDouble()
	}
	override fun getNetworkMessageId(): Int = 5427
}
