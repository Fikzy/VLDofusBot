package fr.lewon.dofus.bot.core.ui.geometry.xml

import fr.lewon.dofus.bot.core.ui.xml.containers.Container

object XmlBounds {

    const val MARGIN_WIDTH = 344f
    const val TOTAL_WIDTH = 1280f
    const val TOTAL_HEIGHT = TOTAL_WIDTH * 4f / 5f

    fun buildRootContainer(): Container {
        return Container("ROOT").also {
            it.defaultTopLeftPosition = XmlPoint()
            it.defaultSize = XmlPoint(TOTAL_WIDTH, TOTAL_HEIGHT)
        }
    }
}