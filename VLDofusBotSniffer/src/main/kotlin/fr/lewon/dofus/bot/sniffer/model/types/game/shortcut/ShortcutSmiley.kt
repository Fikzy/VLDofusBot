package fr.lewon.dofus.bot.sniffer.model.types.game.shortcut

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class ShortcutSmiley : Shortcut() {
	var smileyId: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		smileyId = stream.readVarShort().toInt()
	}
}
