package fr.lewon.dofus.bot.sniffer.model.messages.game.character.spellmodifier

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class RemoveSpellModifierMessage : NetworkMessage() {
	var actorId: Double = 0.0
	var actionType: Int = 0
	var modifierType: Int = 0
	var spellId: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		actorId = stream.readDouble().toDouble()
		actionType = stream.readUnsignedByte().toInt()
		modifierType = stream.readUnsignedByte().toInt()
		spellId = stream.readVarShort().toInt()
	}
	override fun getNetworkMessageId(): Int = 8534
}
