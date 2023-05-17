package fr.lewon.dofus.bot.sniffer.model.types.game.presets

import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class StatsPreset : Preset() {
	var stats: ArrayList<SimpleCharacterCharacteristicForPreset> = ArrayList()
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		stats = ArrayList()
		for (i in 0 until stream.readUnsignedShort().toInt()) {
			val item = SimpleCharacterCharacteristicForPreset()
			item.deserialize(stream)
			stats.add(item)
		}
	}
}
