package com.dulpick.app.feature.map

import com.dulpick.app.domain.place.Place

// 지도 위 두 상세(장소·게시글)를 여닫는 상태 전이. 뷰모델이 커지지 않게 여기에 모은다

// 게시글 상세를 얹는다. 장소 상세가 열려 있어도 닫지 않는다 — 게시글을 닫으면 그 자리로 돌아간다.
// 핀은 상세를 받아온 뒤에 바뀐다. 여기서 미리 비우면 로딩 동안 핀이 사라진다
fun MapState.openingPostDetail(contentId: String, returnsOnClose: Boolean): MapState =
    copy(
        postDetail = PostDetail(contentId, returnsOnClose = returnsOnClose),
        topDetail = TopDetail.POST,
    )

// 게시글 상세가 받아온 장소들. 이때부터 지도 핀·카메라가 이걸 본다.
// 지금 열린 게시글의 것일 때만 올린다 — 앞 게시글의 응답이 늦게 닿으면 남의 핀을 그리게 된다
fun MapState.withPostPlaces(contentId: String, places: List<Place>): MapState {
    if (postDetail?.contentId != contentId) return this
    return copy(postDetail = postDetail.copy(places = places))
}

// 게시글 속 장소 행 탭. 핀 탭과 똑같이 그 장소 상세를 위에 얹는다
fun MapState.openingPostPlaceDetail(placeId: String): MapState {
    val place = postDetail?.places?.firstOrNull { it.id == placeId } ?: return this
    return copy(
        detail = DetailTarget(place, query = "", serverPlaceId = place.id.toLongOrNull()),
        topDetail = TopDetail.PLACE,
    )
}

// 게시글을 닫는다. 아래에 장소 상세가 남아 있으면 그 자리로 돌아간다
fun MapState.closingPostDetail(): MapState =
    copy(postDetail = null, topDetail = if (detail != null) TopDetail.PLACE else null)

// 장소 상세를 닫는다. 아래에 게시글 상세가 남아 있으면 그 자리로 돌아간다
fun MapState.closingPlaceDetail(): MapState =
    copy(detail = null, topDetail = if (postDetail != null) TopDetail.POST else null)
