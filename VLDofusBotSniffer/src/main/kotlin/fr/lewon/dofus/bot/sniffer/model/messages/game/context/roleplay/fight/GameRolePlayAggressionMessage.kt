package fr.lewon.dofus.bot.sniffer.model.messages.game.context.roleplay.fight

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class GameRolePlayAggressionMessage : NetworkMessage() {
	var attackerId: Double = 0.0
	var defenderId: Double = 0.0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		attackerId = stream.readVarLong().toDouble()
		defenderId = stream.readVarLong().toDouble()
	}
	override fun getNetworkMessageId(): Int = 1812
}
