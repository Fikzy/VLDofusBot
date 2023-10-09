package fr.lewon.dofus.bot.core.ui.geometry

abstract class GamePoint<T : GamePoint<T>>(var x: Float = 0f, var y: Float = 0f) {

    fun transpose(x: Float, y: Float): T {
        return buildPoint(this.x + x, this.y + y)
    }

    fun transpose(point: T): T {
        return buildPoint(x + point.x, y + point.y)
    }

    fun invert(): T {
        return buildPoint(-x, -y)
    }

    protected abstract fun buildPoint(x: Float, y: Float): T

}