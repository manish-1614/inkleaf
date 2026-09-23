package com.inkleaf.app.ui.reader

import com.inkleaf.app.domain.model.BlockModel
import com.inkleaf.app.domain.model.HeadingBlock

fun headingIndexById(blocks: List<BlockModel>): Map<String, Int> =
    blocks.mapIndexedNotNull { index, block ->
        (block as? HeadingBlock)?.id?.let { id -> id to index }
    }.toMap()

fun clampScrollTarget(targetIndex: Int, totalBlocks: Int): Int {
    if (totalBlocks <= 0) return 0
    return targetIndex.coerceIn(0, totalBlocks - 1)
}

fun shouldUseInstantScroll(targetIndex: Int, currentIndex: Int, threshold: Int = 30): Boolean {
    return kotlin.math.abs(targetIndex - currentIndex) > threshold
}

