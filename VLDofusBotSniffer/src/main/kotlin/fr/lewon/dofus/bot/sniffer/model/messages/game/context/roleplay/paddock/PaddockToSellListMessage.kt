package fr.lewon.dofus.bot.sniffer.model.messages.game.context.roleplay.paddock

import fr.lewon.dofus.bot.sniffer.model.types.game.paddock.PaddockInformationsForSell
import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.model.types.NetworkType
import fr.lewon.dofus.bot.sniffer.model.ProtocolTypeManager
import fr.lewon.dofus.bot.core.io.stream.BooleanByteWrapper

open class PaddockToSellListMessage : NetworkMessage() {
	var pageIndex: Int = 0
	var totalPage: Int = 0
	var paddockList: ArrayList<PaddockInformationsForSell> = ArrayList()
	override fun deserialize(stream: ByteArrayReader) {
		super.deserialize(stream)
		pageIndex = stream.readVarShort().toInt()
		totalPage = stream.readVarShort().toInt()
		paddockList = ArrayList()
		for (i in 0 until stream.readUnsignedShort().toInt()) {
			val item = PaddockInformationsForSell()
			item.deserialize(stream)
			paddockList.add(item)
		}
	}
	override fun getNetworkMessageId(): Int = 150
}
