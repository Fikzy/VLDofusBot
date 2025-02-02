package fr.lewon.dofus.bot.sniffer.model.messages.game.guild

import fr.lewon.dofus.bot.sniffer.model.types.game.social.SocialEmblem
import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class GuildModificationEmblemValidMessage : NetworkMessage() {
	lateinit var guildEmblem: SocialEmblem
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		guildEmblem = SocialEmblem()
		guildEmblem.deserialize(stream)
	}
	override fun getNetworkMessageId(): Int = 3489
}
