package fr.lewon.dofus.bot.sniffer.model.messages.game.context.roleplay.houses.guild

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class HouseGuildNoneMessage : NetworkMessage() {
	var houseId: Int = 0
	var instanceId: Int = 0
	var secondHand: Boolean = false
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		houseId = stream.readVarInt().toInt()
		instanceId = stream.readInt().toInt()
		secondHand = stream.readBoolean()
	}
	override fun getNetworkMessageId(): Int = 1896
}
