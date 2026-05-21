package com.datecalc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun WheelColumnPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 32.dp,
    visibleCount: Int = 3,
    fontSize: Float = 20f,
    accentColor: Color,
    isCircular: Boolean = false
) {
    require(items.isNotEmpty()) { "Items must not be empty" }

    val selectedColor = MaterialTheme.colorScheme.onSurface
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pickerBg = MaterialTheme.colorScheme.surfaceVariant

    if (isCircular) {
        InfiniteWheelPicker(
            items = items, selectedIndex = selectedIndex, onSelectedChange = onSelectedChange,
            modifier = modifier, itemHeight = itemHeight, visibleCount = visibleCount,
            fontSize = fontSize, accentColor = accentColor, selectedColor = selectedColor,
            unselectedColor = unselectedColor, pickerBg = pickerBg
        )
    } else {
        BoundedWheelPicker(
            items = items, selectedIndex = selectedIndex, onSelectedChange = onSelectedChange,
            modifier = modifier, itemHeight = itemHeight, visibleCount = visibleCount,
            fontSize = fontSize, accentColor = accentColor, selectedColor = selectedColor,
            unselectedColor = unselectedColor, pickerBg = pickerBg
        )
    }
}

private fun calcCenterIndex(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    itemPx: Float
): Int = firstVisibleItemIndex + (firstVisibleItemScrollOffset.toFloat() / itemPx).roundToInt()

private suspend fun snapToCenter(
    listState: LazyListState,
    itemPx: Float
) {
    val targetIndex = calcCenterIndex(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset,
        itemPx
    )
    if (listState.firstVisibleItemIndex != targetIndex ||
        listState.firstVisibleItemScrollOffset != 0
    ) {
        listState.animateScrollToItem(targetIndex, 0)
    }
}

@Composable
private fun InfiniteWheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier,
    itemHeight: Dp,
    visibleCount: Int,
    fontSize: Float,
    accentColor: Color,
    selectedColor: Color,
    unselectedColor: Color,
    pickerBg: Color
) {
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }
    val totalHeightDp = itemHeight * visibleCount
    val centerShift = (visibleCount - 1) / 2
    val itemCount = items.size

    val repeatCount = 200
    val totalItems = itemCount * repeatCount
    val middleStart = (repeatCount / 2) * itemCount

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = middleStart + selectedIndex
    )

    var syncedIndex by remember { mutableIntStateOf(selectedIndex) }
    val stableCallback = rememberUpdatedState(onSelectedChange)

    LaunchedEffect(listState) {
        snapshotFlow {
            calcCenterIndex(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                itemPx
            )
        }.collect { centerVirtual ->
            val realIdx = centerVirtual % itemCount
            if (realIdx != syncedIndex) {
                syncedIndex = realIdx
                stableCallback.value.invoke(realIdx)
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != syncedIndex && !listState.isScrollInProgress) {
            syncedIndex = selectedIndex
            val block = listState.firstVisibleItemIndex / itemCount
            val target = block * itemCount + selectedIndex
            listState.scrollToItem(target)
        }
    }

    LaunchedEffect(listState) {
        var snapping = false
        snapshotFlow { listState.isScrollInProgress }
        .collect { scrolling ->
            if (!scrolling && !snapping) {
                delay(100)
                if (!listState.isScrollInProgress) {
                    snapping = true
                    snapToCenter(listState, itemPx)
                    snapping = false
                }
            }
        }
    }

    val realIndex by remember {
        derivedStateOf {
            calcCenterIndex(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                itemPx
            ) % itemCount
        }
    }

    Box(
        modifier = modifier
            .height(totalHeightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(pickerBg)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cy = size.height / 2
            drawLine(accentColor.copy(alpha = 0.3f), Offset(0f, cy - itemPx / 2), Offset(size.width, cy - itemPx / 2), 1.5f)
            drawLine(accentColor.copy(alpha = 0.3f), Offset(0f, cy + itemPx / 2), Offset(size.width, cy + itemPx / 2), 1.5f)
            drawRect(accentColor.copy(alpha = 0.10f), Offset(0f, cy - itemPx / 2), Size(size.width, itemPx))
        }

        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * centerShift)
        ) {
            items(totalItems) { virtualIndex ->
                val realIdx = virtualIndex % itemCount
                val isSelected = realIdx == realIndex

                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[realIdx],
                        fontSize = fontSize.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) selectedColor else unselectedColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isSelected) 1f else 0.4f
                            scaleY = if (isSelected) 1f else 0.85f
                            scaleX = if (isSelected) 1f else 0.85f
                        }
                    )
                }
            }
        }

        Text("\u25B2", fontSize = 7.sp, color = accentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp))
        Text("\u25BC", fontSize = 7.sp, color = accentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp))
    }
}

@Composable
private fun BoundedWheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier,
    itemHeight: Dp,
    visibleCount: Int,
    fontSize: Float,
    accentColor: Color,
    selectedColor: Color,
    unselectedColor: Color,
    pickerBg: Color
) {
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }
    val totalHeightDp = itemHeight * visibleCount
    val centerShift = (visibleCount - 1) / 2
    val safeLastIndex = items.lastIndex.coerceAtLeast(0)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, safeLastIndex)
    )

    var syncedIndex by remember { mutableIntStateOf(selectedIndex) }
    val stableCallback = rememberUpdatedState(onSelectedChange)

    LaunchedEffect(listState) {
        snapshotFlow {
            val center = calcCenterIndex(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                itemPx
            )
            center.coerceIn(0, safeLastIndex)
        }.collect { index ->
            if (index != syncedIndex) {
                syncedIndex = index
                stableCallback.value.invoke(index)
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != syncedIndex && !listState.isScrollInProgress) {
            syncedIndex = selectedIndex
            val target = selectedIndex.coerceIn(0, safeLastIndex)
            listState.scrollToItem(target)
        }
    }

    LaunchedEffect(listState) {
        var snapping = false
        snapshotFlow { listState.isScrollInProgress }
        .collect { scrolling ->
            if (!scrolling && !snapping) {
                delay(100)
                if (!listState.isScrollInProgress) {
                    snapping = true
                    snapToCenter(listState, itemPx)
                    snapping = false
                }
            }
        }
    }

    val currentCenterIndex by remember {
        derivedStateOf {
            calcCenterIndex(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                itemPx
            ).coerceIn(0, safeLastIndex)
        }
    }

    Box(
        modifier = modifier
            .height(totalHeightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(pickerBg)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cy = size.height / 2
            drawLine(accentColor.copy(alpha = 0.3f), Offset(0f, cy - itemPx / 2), Offset(size.width, cy - itemPx / 2), 1.5f)
            drawLine(accentColor.copy(alpha = 0.3f), Offset(0f, cy + itemPx / 2), Offset(size.width, cy + itemPx / 2), 1.5f)
            drawRect(accentColor.copy(alpha = 0.10f), Offset(0f, cy - itemPx / 2), Size(size.width, itemPx))
        }

        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * centerShift)
        ) {
            items(items.size) { index ->
                val isSelected = index == currentCenterIndex
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = fontSize.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) selectedColor else unselectedColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isSelected) 1f else 0.4f
                            scaleY = if (isSelected) 1f else 0.85f
                            scaleX = if (isSelected) 1f else 0.85f
                        }
                    )
                }
            }
        }

        Text("\u25B2", fontSize = 7.sp, color = accentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp))
        Text("\u25BC", fontSize = 7.sp, color = accentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp))
    }
}
