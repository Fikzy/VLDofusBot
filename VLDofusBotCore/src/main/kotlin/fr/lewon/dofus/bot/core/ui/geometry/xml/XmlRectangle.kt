package fr.lewon.dofus.bot.core.ui.geometry.xml

import fr.lewon.dofus.bot.core.ui.geometry.GameRectangle

class XmlRectangle(position: XmlPoint, size: XmlPoint) : GameRectangle<XmlPoint>(position, size) {

    override fun buildPoint(x: Float, y: Float): XmlPoint = XmlPoint(x, y)
}