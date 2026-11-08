package com.dulpick.app.feature.placedetail.component

// 사진 뷰어 아래 점 한 칸
data class PlacePhotoDot(
    val index: Int,
    val isCurrent: Boolean,
    // 창 밖에 사진이 더 남은 쪽 끝 점. 작게 그린다
    val isSmall: Boolean,
)

// 점 줄이 한 번에 그리는 칸 수
private const val WINDOW_SIZE = 5

// 5장 이하면 다 그린다. 넘으면 지금 사진을 가운데 둔 5칸만 그리고,
// 창 밖에 사진이 남은 쪽 끝 점을 작게 한다. 1장 이하면 점이 없다
fun placePhotoDots(count: Int, current: Int): List<PlacePhotoDot> {
    if (count <= 1) return emptyList()
    val index = current.coerceIn(0, count - 1)
    val size = minOf(count, WINDOW_SIZE)
    val start = (index - WINDOW_SIZE / 2).coerceIn(0, count - size)
    val end = start + size
    return (start until end).map { dot ->
        PlacePhotoDot(
            index = dot,
            isCurrent = dot == index,
            isSmall = (dot == start && start > 0) || (dot == end - 1 && end < count),
        )
    }
}
