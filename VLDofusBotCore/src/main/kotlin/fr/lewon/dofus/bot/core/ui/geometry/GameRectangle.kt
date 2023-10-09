package fr.lewon.dofus.bot.core.ui.geometry

abstract class GameRectangle<T : GamePoint<T>>(var position: T, var size: T) {

    fun getCenter(): T = buildPoint(position.x + size.x / 2, position.y + size.y / 2)

    protected abstract fun buildPoint(x: Float, y: Float): T
}