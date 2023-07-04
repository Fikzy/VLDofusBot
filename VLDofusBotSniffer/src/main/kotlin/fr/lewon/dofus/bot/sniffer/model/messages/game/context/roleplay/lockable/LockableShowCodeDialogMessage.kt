package fr.lewon.dofus.bot.sniffer.model.messages.game.context.roleplay.lockable

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class LockableShowCodeDialogMessage : NetworkMessage() {
	var changeOrUse: Boolean = false
	var codeSize: Int = 0
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		changeOrUse = stream.readBoolean()
		codeSize = stream.readUnsignedByte().toInt()
	}
	override fun getNetworkMessageId(): Int = 8686
}
