package fr.lewon.dofus.bot.util.game

import fr.lewon.dofus.bot.core.d2p.elem.D2PElementsAdapter
import fr.lewon.dofus.bot.core.d2p.elem.graphical.GraphicalElementData
import fr.lewon.dofus.bot.core.d2p.elem.graphical.impl.EntityGraphicalElementData
import fr.lewon.dofus.bot.core.d2p.elem.graphical.impl.NormalGraphicalElementData
import fr.lewon.dofus.bot.core.d2p.gfx.D2PWorldGfxAdapter
import fr.lewon.dofus.bot.core.d2p.maps.cell.CompleteCellData
import fr.lewon.dofus.bot.core.d2p.maps.element.GraphicalElement
import fr.lewon.dofus.bot.core.d2p.sprite.D2PBonesSpriteAdapter
import fr.lewon.dofus.bot.core.d2p.sprite.DefineSprite
import fr.lewon.dofus.bot.core.ui.geometry.ui.UIPoint
import fr.lewon.dofus.bot.core.ui.geometry.ui.UIRectangle
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameCautiousMapMovementRequestMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.context.GameMapMovementRequestMessage
import fr.lewon.dofus.bot.sniffer.model.messages.game.interactive.InteractiveUseRequestMessage
import fr.lewon.dofus.bot.sniffer.model.types.game.interactive.InteractiveElement
import fr.lewon.dofus.bot.sniffer.model.types.game.interactive.InteractiveElementSkill
import fr.lewon.dofus.bot.util.geometry.PointAbsolute
import fr.lewon.dofus.bot.util.geometry.PointRelative
import fr.lewon.dofus.bot.util.geometry.RectangleAbsolute
import fr.lewon.dofus.bot.util.geometry.RectangleRelative
import fr.lewon.dofus.bot.util.io.*
import fr.lewon.dofus.bot.util.network.info.GameInfo
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object InteractiveUtil {

    private val REF_INTERACTIVE_LOCATION = PointRelative(0.5502451f, 0.44257274f)
    private val REF_HEADER_RECT = RectangleRelative.build(
        REF_INTERACTIVE_LOCATION, PointRelative(0.560049f, 0.47473204f)
    )
    private val REF_FIRST_OPTION_LOCATION = PointRelative(0.560049f, 0.47473204f)
    private val REF_TENTH_OPTION_LOCATION = PointRelative(0.560049f, 0.6707504f)

    private val DELTA_OPTION = (REF_TENTH_OPTION_LOCATION.y - REF_FIRST_OPTION_LOCATION.y) / 9f

    private val OPTION_HEADER_MIN_COLOR = Color(65, 60, 48)
    private val OPTION_HEADER_MAX_COLOR = Color(73, 68, 56)

    private val INVALID_SKILL_IDS = listOf(339, 360, 361, 362)

    private val MinClickPoint = PointRelative(0.03f, 0.018f).toUIPoint()
    private val MaxClickPoint = PointRelative(0.97f, 0.875f).toUIPoint()

    fun getElementCellData(gameInfo: GameInfo, interactiveElement: InteractiveElement): CompleteCellData =
        gameInfo.mapData.completeCellDataByCellId.values
            .firstOrNull { it.graphicalElements.map { ge -> ge.identifier }.contains(interactiveElement.elementId) }
            ?: error("No cell data found for element : ${interactiveElement.elementId}")

    private fun cropBounds(bounds: UIRectangle, boundsCropDelta: UIRectangleDelta): UIRectangle {
        return UIRectangle(
            position = bounds.position.transpose(x = boundsCropDelta.leftDelta, y = boundsCropDelta.topDelta),
            size = bounds.size.transpose(
                x = -boundsCropDelta.rightDelta - boundsCropDelta.leftDelta,
                y = -boundsCropDelta.bottomDelta - boundsCropDelta.topDelta
            ).let {
                UIPoint(x = max(0f, it.x), y = max(0f, it.y))
            }
        )
    }

    private data class UIRectangleDelta(
        val leftDelta: Float,
        val rightDelta: Float,
        val topDelta: Float,
        val bottomDelta: Float,
    )

    private fun getBoundsCropDelta(bounds: UIRectangle): UIRectangleDelta {
        return UIRectangleDelta(
            leftDelta = max(0f, MinClickPoint.x - bounds.position.x),
            rightDelta = max(0f, bounds.position.x + bounds.size.x - MaxClickPoint.x),
            topDelta = max(0f, MinClickPoint.y - bounds.position.y),
            bottomDelta = max(0f, bounds.position.y + bounds.size.y - MaxClickPoint.y),
        )
    }

    fun getRawInteractiveBounds(
        gameInfo: GameInfo,
        destCellCompleteData: CompleteCellData,
        elementData: GraphicalElementData,
        graphicalElement: GraphicalElement,
    ): UIRectangle {
        val destCellId = destCellCompleteData.cellId
        val cell = gameInfo.dofusBoard.getCell(destCellId)
        val elementBounds = getElementBounds(elementData, cell)
        val offset = UIPoint(
            x = graphicalElement.pixelOffset.x,
            y = graphicalElement.pixelOffset.y - graphicalElement.altitude * 10
        )
        val cellCenter = cell.getCenter().toUIPoint().transpose(offset)
        return UIRectangle(cellCenter.transpose(elementBounds.position), elementBounds.size)
    }

    private fun getElementBounds(elementData: GraphicalElementData, cell: DofusCell): UIRectangle = when (elementData) {
        is NormalGraphicalElementData ->
            UIRectangle(position = elementData.origin.invert(), size = elementData.size)
        is EntityGraphicalElementData ->
            getBoneSprite(elementData)?.getBounds(elementData.horizontalSymmetry) ?: getDefaultBounds(cell)
        else -> getDefaultBounds(cell)
    }

    private fun getDefaultBounds(cell: DofusCell): UIRectangle {
        val topLeft = cell.bounds.getTopLeft().toUIPoint()
        val bottomRight = cell.bounds.getBottomRight().toUIPoint()
        val size = UIPoint(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
        val position = UIPoint(-size.x / 2f, -size.y / 2f)
        return UIRectangle(position, size)
    }

    private fun getBoneSprite(elementData: EntityGraphicalElementData): DefineSprite? {
        val boneId = elementData.entityLook.substring(1, elementData.entityLook.length - 1).toInt()
        return D2PBonesSpriteAdapter.getBoneSprite(boneId.toDouble())
    }

    fun getInteractivePotentialClickLocations(gameInfo: GameInfo, elementId: Int): List<PointAbsolute> {
        val interactiveElement = gameInfo.interactiveElements.firstOrNull { it.elementId == elementId }
            ?: error("Element not found on map : $elementId")
        val destCellCompleteData = getElementCellData(gameInfo, interactiveElement)
        val graphicalElement = destCellCompleteData.graphicalElements.firstOrNull {
            it.identifier == interactiveElement.elementId
        } ?: error("No graphical element found for element : ${interactiveElement.elementId}")

        val elementData = D2PElementsAdapter.getElement(graphicalElement.elementId)
        val rawBounds = getRawInteractiveBounds(gameInfo, destCellCompleteData, elementData, graphicalElement)
        val cropDelta = getBoundsCropDelta(rawBounds)
        val realBounds = cropBounds(rawBounds, cropDelta)
        getCustomClickLocations(elementId, realBounds.toRectangleAbsolute(gameInfo))?.let {
            return it
        }
        val clickLocations = getElementClickLocations(elementData, cropDelta).map {
            rawBounds.position.transpose(it).toPointAbsolute(gameInfo)
        }.filter {
            val pointRelative = it.toPointRelative(gameInfo)
            pointRelative.x in 0.03f..0.97f && pointRelative.y in 0.018f..0.88f
        }
        if (clickLocations.isEmpty()) {
            val defaultClickLocation = UIPoint(
                realBounds.position.x + realBounds.size.x / 2,
                realBounds.position.y + realBounds.size.y / 3
            ).toPointAbsolute(gameInfo)
            return listOf(defaultClickLocation)
        }
        return clickLocations
    }

    fun getInteractiveGfx(elementData: GraphicalElementData): BufferedImage? = when (elementData) {
        is NormalGraphicalElementData -> {
            val gfxByteArray = D2PWorldGfxAdapter.getWorldGfxImageData(elementData.gfxId.toDouble())
            ImageIO.read(ByteArrayInputStream(gfxByteArray))
        }
        is EntityGraphicalElementData ->
            getBoneSprite(elementData)?.getImage()
        else -> null
    }

    fun isReversedHorizontally(elementData: GraphicalElementData): Boolean = when (elementData) {
        is NormalGraphicalElementData -> elementData.horizontalSymmetry
        is EntityGraphicalElementData -> elementData.horizontalSymmetry
        else -> false
    }

    private fun getElementClickLocations(
        elementData: GraphicalElementData,
        cropDelta: UIRectangleDelta
    ): List<UIPoint> {
        val gfx = getInteractiveGfx(elementData)
            ?: return emptyList()
        val horizontalSymmetry = isReversedHorizontally(elementData)
        val sectionsCountX = 18
        val sectionsCountY = 12
        val minX = cropDelta.leftDelta
        val maxX = gfx.width - cropDelta.rightDelta
        val width = maxOf(0f, maxX - minX)
        val minY = cropDelta.topDelta
        val maxY = gfx.height - cropDelta.bottomDelta
        val height = maxOf(0f, maxY - minY)
        val preSelectedClickLocations = (0..sectionsCountX).flatMap { sectionX ->
            (0..sectionsCountY).map { sectionY ->
                UIPoint(
                    minX + width / 5f + sectionX.toFloat() / sectionsCountX * width * 3f / 5f,
                    minY + height / 10f + sectionY.toFloat() / sectionsCountY * height / 2f,
                )
            }
        }
        val raster = gfx.raster
        val numBands = raster.numBands
        val delta = 3
        val sideSize = (delta * 2 + 1)
        val intArray = IntArray(sideSize * sideSize * numBands)
        val validClickLocations = preSelectedClickLocations.filter { clickLocation ->
            val xPos = clickLocation.x.toInt()
            val yPos = clickLocation.y.toInt()
            val realXPos = if (horizontalSymmetry) gfx.width - xPos else xPos
            val rectXPos = realXPos - delta
            val rectYPos = yPos - delta
            if (rectXPos in 0..<gfx.width - sideSize && rectYPos in 0..<gfx.height - sideSize) {
                raster.getPixels(rectXPos, rectYPos, sideSize, sideSize, intArray).all { it != 0 }
            } else false
        }
        return validClickLocations
    }

    private fun getCustomClickLocations(elementId: Int, bounds: RectangleAbsolute) = when (elementId) {
        518476, // 20 ; -36
        -> listOf(bounds.getCenter().getSum(PointAbsolute(bounds.width / 3, 0)))
        485282, // -1 ; -42
        510414, // -21 ; 22
        -> listOf(bounds.getCenter().getSum(PointAbsolute(bounds.width / 3, bounds.height / 3)))
        523592, // -23 ; 39
        -> listOf(bounds.getCenter())
        483927, // 22 ; 22
        -> listOf(bounds.getCenter().getSum(PointAbsolute(bounds.width / 3, -bounds.height / 3)))
        521652, // -36 ; -60
        523654, // -33 ; -58
        -> listOf(bounds.getCenter().getSum(PointAbsolute(-bounds.width / 3, bounds.height / 3)))
        522812, // -23 ; 38
        510178, // -22 ; 23
        -> listOf(bounds.getTopRight().getSum(PointAbsolute(-2, 2)))
        else -> null
    }

    private fun getElementClickPosition(gameInfo: GameInfo, elementId: Int): PointAbsolute {
        val entitiesCellIds = gameInfo.entityPositionsOnMapByEntityId.values
        val interactiveElementsCellIds = gameInfo.interactiveElements
            .filter { it.elementId != elementId }
            .map { getElementCellData(gameInfo, it).cellId }
        val toAvoidAbsoluteLocations = entitiesCellIds.plus(interactiveElementsCellIds)
            .map { cellId -> gameInfo.dofusBoard.getCell(cellId) }
            .map { cell -> cell.getCenter().toPointAbsolute(gameInfo) }
        return getInteractivePotentialClickLocations(gameInfo, elementId).maxBy { clickLocation ->
            toAvoidAbsoluteLocations.minOfOrNull { entityLocation ->
                abs(clickLocation.x - entityLocation.x) + abs(clickLocation.y - entityLocation.y)
            } ?: Int.MAX_VALUE
        }
    }

    fun useInteractive(gameInfo: GameInfo, elementId: Int, skillId: Int) {
        gameInfo.eventStore.clear()
        RetryUtil.tryUntilSuccess(
            {
                val element = gameInfo.interactiveElements.firstOrNull { it.elementId == elementId }
                    ?: error("Element not found on current map : $elementId")
                val skills = element.enabledSkills.filter { !INVALID_SKILL_IDS.contains(it.skillId) }
                val elementClickLocation = getElementClickPosition(gameInfo, elementId)
                val skillIndex = skills.map { it.skillId }.indexOf(skillId)
                if (skillIndex < 0) {
                    error("No skill available on interactive : $skillId on element : $elementId")
                }
                doUseInteractive(gameInfo, elementClickLocation.toPointRelative(gameInfo), skills, skillIndex)
            },
            { waitUntilInteractiveUseRequestSent(gameInfo) },
            4
        ) ?: error("No interactive used")
    }

    private fun waitUntilInteractiveUseRequestSent(gameInfo: GameInfo): Boolean = WaitUtil.waitUntil(500) {
        gameInfo.eventStore.getLastEvent(InteractiveUseRequestMessage::class.java) != null
            || gameInfo.eventStore.getLastEvent(GameMapMovementRequestMessage::class.java) != null
            || gameInfo.eventStore.getLastEvent(GameCautiousMapMovementRequestMessage::class.java) != null
    }

    private fun doUseInteractive(
        gameInfo: GameInfo,
        elementClickLocation: PointRelative,
        skills: List<InteractiveElementSkill>,
        skillIndex: Int,
    ) {
        if (skills.filter { !INVALID_SKILL_IDS.contains(it.skillId) }.size > 1) {
            if (!clickButtonWithOptions(gameInfo, elementClickLocation, skillIndex)) {
                error("Couldn't use interactive")
            }
        } else {
            MouseUtil.leftClick(gameInfo, elementClickLocation)
        }
    }

    fun clickButtonWithOptions(
        gameInfo: GameInfo,
        buttonLocation: PointRelative,
        optionIndex: Int,
        optionListHasHeader: Boolean = true
    ): Boolean = RetryUtil.tryUntilSuccess(
        {
            doClickButtonWithOptions(
                gameInfo,
                buttonLocation,
                if (optionListHasHeader) optionIndex else optionIndex - 1
            )
        },
        10
    )

    private fun doClickButtonWithOptions(
        gameInfo: GameInfo,
        buttonLocation: PointRelative,
        optionIndex: Int,
    ): Boolean {
        val optionHeaderRect = REF_HEADER_RECT.getTranslation(REF_INTERACTIVE_LOCATION.opposite())
            .getTranslation(buttonLocation)
        MouseUtil.leftClick(gameInfo, buttonLocation)
        if (!WaitUtil.waitUntil(3000) { isOptionFound(gameInfo, buttonLocation, optionHeaderRect) }) {
            return false
        }
        val firstOptionLoc = REF_FIRST_OPTION_LOCATION.getDifference(REF_INTERACTIVE_LOCATION)
            .getSum(buttonLocation)
        MouseUtil.leftClick(gameInfo, firstOptionLoc.getSum(PointRelative(y = optionIndex.toFloat() * DELTA_OPTION)))
        return true
    }

    private fun isOptionFound(
        gameInfo: GameInfo,
        elementClickLocation: PointRelative,
        optionHeaderRect: RectangleRelative,
    ): Boolean {
        MouseUtil.move(gameInfo, elementClickLocation)
        return ScreenUtil.colorCount(
            gameInfo,
            optionHeaderRect,
            OPTION_HEADER_MIN_COLOR,
            OPTION_HEADER_MAX_COLOR,
        ) > 0
    }

    fun getNpcClickPosition(gameInfo: GameInfo, npcId: Int): PointRelative {
        val npcEntityId = gameInfo.entityIdByNpcId[npcId] ?: error("NPC $npcId not on current map")
        val npcCellId = gameInfo.entityPositionsOnMapByEntityId[npcEntityId] ?: error("entity $npcEntityId not found")
        val cell = gameInfo.dofusBoard.getCell(npcCellId)
        val delta = UIPoint(0f, -cell.cellData.floor.toFloat())
        val dRelativePoint = delta.toPointRelative()
        val destPointRelative = cell.getCenter().getSum(dRelativePoint)
        destPointRelative.x = max(0.001f, min(destPointRelative.x, 0.99f))
        destPointRelative.y = max(0.001f, min(destPointRelative.y, 0.99f))
        return destPointRelative
    }

    fun getCellClickPosition(gameInfo: GameInfo, cellId: Int, avoidCenter: Boolean = true): PointRelative {
        val cell = gameInfo.dofusBoard.getCell(cellId)
        val cellBounds = cell.bounds
        val cellCenter = cellBounds.getCenter()

        val floor = cell.cellData.floor
        val dxMultiplier = if (floor != 0 || !avoidCenter) 0 else if (cellCenter.x > 0.5) 1 else -1
        val dFloor = UIPoint(y = floor.toFloat()).toPointRelative()
        return PointRelative(
            cellCenter.x + dxMultiplier * cellBounds.width * 0.8f,
            cellCenter.y - dFloor.y
        )
    }

}