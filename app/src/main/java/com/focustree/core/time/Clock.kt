package com.focustree.core.time

fun interface Clock {
    fun nowEpochMillis(): Long
}

object SystemClock : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
