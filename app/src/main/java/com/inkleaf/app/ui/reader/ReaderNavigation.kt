package com.inkleaf.app.ui.reader

import com.inkleaf.app.domain.model.BlockModel
import com.inkleaf.app.domain.model.HeadingBlock

fun headingIndexById(blocks: List<BlockModel>): Map<String, Int> =
    blocks.mapIndexedNotNull { index, block ->
        (block as? HeadingBlock)?.id?.let { id -> id to index }
    }.toMap()
