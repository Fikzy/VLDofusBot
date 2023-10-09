package fr.lewon.dofus.bot.core.ui.managers

import fr.lewon.dofus.bot.core.ui.dat.DatUtil
import fr.lewon.dofus.bot.core.ui.geometry.xml.XmlBounds
import fr.lewon.dofus.bot.core.ui.geometry.xml.XmlPoint
import fr.lewon.dofus.bot.core.ui.xml.containers.Container

enum class DofusUIElement(
    private val xmlFileName: String,
    private val positionOverrideType: OverrideType = OverrideType.NO_OVERRIDE,
    private val key: String = "NO_OVERRIDE",
    private val ctr: String = "NO_OVERRIDE",
) {

    INVENTORY("equipmentUi.xml", OverrideType.REPLACE, "storage", "equipmentUi"),
    ZAAP_SELECTION("zaapiSelection.xml", OverrideType.ADD_OVERRIDE, "zaapSelection", "window281"),
    BANNER("banner.xml", OverrideType.REPLACE, "banner", "mainCtr"),
    TREASURE_HUNT("treasureHunt.xml", OverrideType.REPLACE, "treasureHunt", "ctr_hunt"),
    ARENA("pvpArena.xml", OverrideType.REPLACE, "pvpArena", "window921"),
    MOUNT_PADDOCK("mountPaddock.xml"),
    STORAGE("storage.xml"),
    LVL_UP("LevelUp.xml"),
    LVL_UP_WITH_SPELL("LevelUpWithSpell.xml"),
    LVL_UP_OMEGA("LevelUpOmega.xml"),
    QUEST_BASE("questBase.xml", OverrideType.REPLACE, key = "questBase", ctr = "mainCtr"),
    CHALLENGE_DISPLAY("challengeDisplay.xml", OverrideType.REPLACE, key = "challengeDisplay", ctr = "mainCtr"),
    TIPS("tipsUi.xml", OverrideType.REPLACE, key = "tips", ctr = "ctr_main")
    ;

    companion object {

        private const val CONTEXT_DEFAULT = "default"
        private const val CONTEXT_FIGHT = "fight"

        private fun getXmlPoint(keyRegex: String): XmlPoint? {
            val uiPointByKey = DatUtil.getDatFileContent("Berilia_ui_positions", DofusXmlPointByKey::class.java)
                ?: error("Couldn't get UI position : $keyRegex")
            return uiPointByKey.entries.firstOrNull { it.key.matches(Regex(keyRegex)) }?.value
        }

        fun shouldInitializeXml(xmlFileName: String): Boolean {
            return entries.any { it.xmlFileName == xmlFileName }
        }

        private class DofusXmlPointByKey : HashMap<String, XmlPoint?>()
    }

    fun getPosition(fightContext: Boolean = false): XmlPoint {
        return getContainer(fightContext).bounds.position
    }

    fun getSize(fightContext: Boolean = false): XmlPoint {
        return getContainer(fightContext).bounds.size
    }

    fun getContainer(fightContext: Boolean = false): Container {
        val uiDefinition = XmlUiUtil.getUIDefinition(xmlFileName)
        val container = uiDefinition.children.firstOrNull { it.name.matches(Regex(ctr)) }
            ?: uiDefinition.children[0]
        container.defaultSize = getXmlPoint(buildSizeKey(fightContext))
        XmlContainerInitializer.initAll(container)
        val overriddenPosition = getXmlPoint(buildPosKey(fightContext))
        val positionDelta = if (overriddenPosition != null) {
            positionOverrideType.getResultPosition(container.bounds.position, overriddenPosition)
                .transpose(container.bounds.position.invert())
        } else if (uiDefinition.fullscreen) {
            XmlPoint(XmlBounds.MARGIN_WIDTH).invert()
        } else {
            XmlPoint(0f, 0f)
        }
        updatePosition(container, positionDelta)
        return container
    }

    private fun updatePosition(container: Container, positionDelta: XmlPoint) {
        container.bounds.position = container.bounds.position.transpose(positionDelta)
        container.children.forEach {
            updatePosition(it, positionDelta)
        }
    }

    private fun buildPosKey(fightContext: Boolean): String {
        return buildKey(fightContext, "pos")
    }

    private fun buildSizeKey(fightContext: Boolean): String {
        return buildKey(fightContext, "size")
    }

    private fun buildKey(fightContext: Boolean, infoType: String): String {
        val context = if (fightContext) CONTEXT_FIGHT else CONTEXT_DEFAULT
        return "$key##$infoType##$ctr##$context"
    }

    private enum class OverrideType(private val resultPositionCalculator: (XmlPoint, XmlPoint) -> XmlPoint) {

        ADD_OVERRIDE({ basePosition, overriddenPosition -> basePosition.transpose(overriddenPosition) }),
        REPLACE({ _, overriddenPosition -> overriddenPosition }),
        NO_OVERRIDE({ basePosition, _ -> basePosition })
        ;

        fun getResultPosition(basePosition: XmlPoint, overriddenPosition: XmlPoint): XmlPoint {
            return resultPositionCalculator(basePosition, overriddenPosition)
        }
    }

}