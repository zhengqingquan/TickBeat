package com.tickbeat.app.domain

/** Shift raw tap timestamps so judging matches perceived audible clicks. */
fun applyInputOffset(tapMs: List<Long>, offsetMs: Int): List<Long> =
    if (offsetMs == 0) tapMs else tapMs.map { it - offsetMs }

fun applyInputOffset(tapMs: Long, offsetMs: Int): Long = tapMs - offsetMs
