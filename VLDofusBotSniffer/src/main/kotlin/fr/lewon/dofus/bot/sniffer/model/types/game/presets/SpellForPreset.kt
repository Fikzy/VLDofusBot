package fr.lewon.dofus.bot.sniffer.model.types.game.presets

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class SpellForPreset : NetworkType() {
	var spellId: Int = 0
	var shortcuts: ArrayList<Int> = ArrayList()
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		spellId = stream.readVarShort().toInt()
		shortcuts = ArrayList()
		for (i in 0 until stream.readUnsignedShort().toInt()) {
			val item = stream.readUnsignedShort().toInt()
			shortcuts.add(item)
		}
	}
}
