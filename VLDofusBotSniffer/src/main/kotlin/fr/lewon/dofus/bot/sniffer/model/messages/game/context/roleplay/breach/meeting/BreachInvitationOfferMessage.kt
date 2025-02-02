package fr.lewon.dofus.bot.sniffer.model.messages.game.context.roleplay.breach.meeting

import fr.lewon.dofus.bot.sniffer.model.types.game.character.CharacterMinimalInformations
import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class BreachInvitationOfferMessage : NetworkMessage() {
	lateinit var host: CharacterMinimalInformations
	var timeLeftBeforeCancel: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		host = CharacterMinimalInformations()
		host.deserialize(stream)
		timeLeftBeforeCancel = stream.readVarInt().toInt()
	}
	override fun getNetworkMessageId(): Int = 5823
}
