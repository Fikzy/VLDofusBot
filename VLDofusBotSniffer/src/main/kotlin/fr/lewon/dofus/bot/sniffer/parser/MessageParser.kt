package fr.lewon.dofus.bot.sniffer.parser

import com.fasterxml.jackson.databind.ObjectMapper
import fr.lewon.dofus.bot.core.io.stream.ByteArrayReader
import fr.lewon.dofus.bot.sniffer.DofusMessagePremise
import fr.lewon.dofus.bot.sniffer.DofusMessageReceiverUtil
import fr.lewon.dofus.bot.sniffer.exceptions.AddToStoreFailedException
import fr.lewon.dofus.bot.sniffer.exceptions.IncompleteMessageException
import fr.lewon.dofus.bot.sniffer.exceptions.ParseFailedException
import fr.lewon.dofus.bot.sniffer.model.messages.NetworkMessage
import fr.lewon.dofus.bot.sniffer.store.EventStore
import org.pcap4j.packet.TcpPacket
import java.awt.Color

abstract class MessageParser(private val packetOrigin: PacketOrigin, private val state: MessageParserState) {

    companion object {

        const val BIT_RIGHT_SHIFT_LEN_PACKET_ID = 2
        const val BIT_MASK = 3
    }

    private val objectMapper = ObjectMapper()
    private val packets = ArrayList<TcpPacket>()

    @Synchronized
    fun receivePacket(tcpPacket: TcpPacket) {
        try {
            packets.add(tcpPacket)
            val printNevermind = packets.size > 20
            handlePackets()
            packets.clear()
            if (printNevermind) {
                println("${getLogPrefix()} : Nevermind, everything worked as planned.")
            }
        } catch (e: AddToStoreFailedException) {
            e.printStackTrace()
        } catch (e: Exception) {
            // Nothing
        }
        if (packets.size == 20) {
            println("${getLogPrefix()} : Large packet buffer, character might have crashed. If a character is stuck, please reload sniffer.")
        }
    }

    private fun getLogPrefix(): String =
        "${state.connection.characterName} - ${state.connection.client.ip}:${state.connection.client.port} [${packetOrigin.name}]"

    private fun handlePackets() {
        val rawData = getSortedPackets().flatMap { it.payload.rawData.toList() }.toByteArray()
        parseMessagePremises(ByteArrayReader(rawData)).forEach(this::processMessagePremise)
    }

    private fun getSortedPackets(): List<TcpPacket> {
        return packets.sortedBy { it.header.sequenceNumberAsLong }
    }

    private fun processMessagePremise(messagePremise: DofusMessagePremise) {
        val premiseStr = "[${packetOrigin.name}] ${messagePremise.eventClass.simpleName}:${messagePremise.eventId}"
        val message = deserializeMessage(messagePremise)
        addMessageToStore(message)
        val color = if (EventStore.getHandlers(messagePremise.eventClass).isEmpty()) Color.GRAY else Color.WHITE
        state.logger.log(premiseStr, description = objectMapper.writeValueAsString(message), color = color)
    }

    private fun deserializeMessage(messagePremise: DofusMessagePremise): NetworkMessage {
        return try {
            messagePremise.eventClass.getConstructor().newInstance().also { it.deserialize(messagePremise.stream) }
        } catch (t: Throwable) {
            throw ParseFailedException(messagePremise.eventClass.simpleName, messagePremise.eventId, t)
        }
    }

    private fun addMessageToStore(message: NetworkMessage) {
        try {
            state.eventStore.addSocketEvent(message, state.connection)
        } catch (t: Throwable) {
            throw AddToStoreFailedException(message::class.java.toString(), t)
        }
    }

    private fun parseMessagePremises(data: ByteArrayReader): List<DofusMessagePremise> {
        val premises = ArrayList<DofusMessagePremise>()
        while (data.available() > 0) {
            premises.add(parseMessagePremise(data))
        }
        return premises
    }

    private fun parseMessagePremise(src: ByteArrayReader): DofusMessagePremise {
        if (src.available() >= 2) {
            val header = src.readUnsignedShort()
            val lengthType = header and BIT_MASK
            val messageId = header shr BIT_RIGHT_SHIFT_LEN_PACKET_ID
            if (src.available() >= lengthType) {
                val messageLength = readMessageLength(header, lengthType, src)
                if (src.available() >= messageLength) {
                    val stream = ByteArrayReader(src.readNBytes(messageLength))
                    return DofusMessageReceiverUtil.parseMessagePremise(stream, messageId)
                }
            }
        }
        throw IncompleteMessageException()
    }

    protected abstract fun readMessageLength(header: Int, lengthType: Int, src: ByteArrayReader): Int

}