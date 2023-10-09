package fr.lewon.dofus.bot.core.ui.geometry.xml

import fr.lewon.dofus.bot.core.ui.geometry.GamePoint

class XmlPoint(x: Float = 0f, y: Float = 0f) : GamePoint<XmlPoint>(x, y) {

    override fun buildPoint(x: Float, y: Float): XmlPoint = XmlPoint(x, y)
}