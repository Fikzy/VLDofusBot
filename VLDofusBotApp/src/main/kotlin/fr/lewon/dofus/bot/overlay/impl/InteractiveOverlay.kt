package fr.lewon.dofus.bot.overlay.impl

import fr.lewon.dofus.bot.core.d2p.elem.D2PElementsAdapter
import fr.lewon.dofus.bot.core.d2p.elem.graphical.GraphicalElementData
import fr.lewon.dofus.bot.overlay.AbstractOverlay
import fr.lewon.dofus.bot.overlay.AbstractOverlayPanel
import fr.lewon.dofus.bot.sniffer.model.types.game.interactive.InteractiveElement
import fr.lewon.dofus.bot.util.filemanagers.impl.GlobalConfigManager
import fr.lewon.dofus.bot.util.game.InteractiveUtil
import fr.lewon.dofus.bot.util.geometry.PointAbsolute
import fr.lewon.dofus.bot.util.geometry.RectangleAbsolute
import fr.lewon.dofus.bot.util.io.toRectangleAbsolute
import fr.lewon.dofus.bot.util.network.info.GameInfo
import java.awt.Color
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.JButton
import javax.swing.JPanel

object InteractiveOverlay : AbstractOverlay() {

    override fun additionalInit() {
        // Nothing
    }

    override fun buildContentPane(): JPanel {
        return InteractiveOverlayPanel(this)
    }

    override fun buildOverlayBounds(): Rectangle {
        val gameBounds = gameInfo.gameBounds
        return Rectangle(
            0,
            gameBounds.y,
            gameBounds.x * 2 + gameBounds.width,
            gameBounds.height
        )
    }

    override fun updateOverlay(gameInfo: GameInfo) {
        super.updateOverlay(gameInfo)
        (contentPane as InteractiveOverlayPanel).updateInteractives(gameInfo)
    }

    private class InteractiveOverlayPanel(overlay: AbstractOverlay) : AbstractOverlayPanel(overlay) {

        private val refreshButton = JButton("Refresh")

        init {
            refreshButton.addActionListener {
                updateInteractives(gameInfo)
            }
            add(refreshButton)
        }

        private data class InteractiveData(
            val interactiveElement: InteractiveElement,
            val bounds: RectangleAbsolute,
            val gfx: BufferedImage?,
            val clickLocations: List<PointAbsolute>
        )

        var interactiveDataList = emptyList<InteractiveData>()

        fun updateInteractives(gameInfo: GameInfo) {
            interactiveDataList = gameInfo.interactiveElements.filter { it.onCurrentMap }.map { interactiveElement ->
                val destCellCompleteData = InteractiveUtil.getElementCellData(gameInfo, interactiveElement)
                val graphicalElement = destCellCompleteData.graphicalElements
                    .firstOrNull { it.identifier == interactiveElement.elementId }
                    ?: error("No graphical element found for element : ${interactiveElement.elementId}")
                val elementData = D2PElementsAdapter.getElement(graphicalElement.elementId)
                val bounds = InteractiveUtil
                    .getRawInteractiveBounds(gameInfo, destCellCompleteData, elementData, graphicalElement)
                    .toRectangleAbsolute(gameInfo)
                val gfx = if (GlobalConfigManager.readConfig().displayInteractiveGfx) getGfx(elementData) else null

                val potentialClickLocations =
                    InteractiveUtil.getInteractivePotentialClickLocations(gameInfo, interactiveElement.elementId)
                InteractiveData(interactiveElement, bounds, gfx, potentialClickLocations)
            }
        }

        private fun getGfx(elementData: GraphicalElementData): BufferedImage? {
            val gfx = InteractiveUtil.getInteractiveGfx(elementData)
                ?: return null
            val isSymmetrical = InteractiveUtil.isReversedHorizontally(elementData)
            if (!isSymmetrical) {
                return gfx
            }
            val at = AffineTransform()
            at.concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
            at.concatenate(AffineTransform.getTranslateInstance(-gfx.width.toDouble(), 0.0))
            val newImage = BufferedImage(gfx.width, gfx.height, BufferedImage.TYPE_INT_ARGB)
            val g = newImage.createGraphics()
            g.transform(at)
            g.drawImage(gfx, 0, 0, null)
            g.dispose()
            return newImage
        }

        override fun onHover(mouseLocation: Point) {
            //Nothing
        }

        override fun drawBackground(g: Graphics) {
            //Nothing
        }

        override fun drawOverlay(g: Graphics) {
            interactiveDataList.sortedByDescending { it.bounds.width * it.bounds.height }.forEach { interactive ->
                val rect = interactive.bounds
                g.color = if (interactive.interactiveElement.enabledSkills.isNotEmpty()) Color.LIGHT_GRAY else Color.RED
                g.fillRect(rect.x, rect.y, rect.width, rect.height)
                g.color = Color.BLACK
                g.drawString(interactive.interactiveElement.elementId.toString(), rect.x, rect.y)
                g.drawRect(rect.x, rect.y, rect.width, rect.height)
                g.drawImage(interactive.gfx, rect.x, rect.y, rect.width, rect.height, null)
                interactive.clickLocations.forEach { point ->
                    g.drawLine(point.x - 5, point.y, point.x + 5, point.y)
                    g.drawLine(point.x, point.y - 5, point.x, point.y + 5)
                }
            }
        }

    }
}