package com.dulpick.app.data.course.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder

// 값의 형식이 틀리면 던지지 않고 빈 값으로 읽는다.
// 구간 경로 한 칸 때문에 코스 전체 읽기가 깨지지 않게 한다 (iOS 의 try? decodeIfPresent 와 같다).
//
// 먼저 그 자리의 JSON 을 그대로 떠낸 뒤 원래 방식으로 읽어 본다. 실패하면 fallback 이다
internal open class LenientSerializer<T>(
    private val delegate: KSerializer<T>,
    private val fallback: T,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): T {
        // JSON 이 아니면 봐줄 방법이 없다. 원래 방식 그대로 읽는다
        val json = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = json.decodeJsonElement()
        return runCatching { json.json.decodeFromJsonElement(delegate, element) }.getOrDefault(fallback)
    }
}

// 경로 좌표 배열. 키가 없거나 형식이 틀리면 빈 배열
internal object RoutePathSerializer : LenientSerializer<List<RoutePointResponseDto>>(
    ListSerializer(RoutePointResponseDto.serializer()),
    emptyList(),
)

// 대중교통 구간 배열. 키가 없거나 형식이 틀리면 빈 배열
internal object TransitLegsSerializer : LenientSerializer<List<TransitLegResponseDto>>(
    ListSerializer(TransitLegResponseDto.serializer()),
    emptyList(),
)

// 대중교통. 키가 없거나 형식이 틀리면 null
internal object TransitSerializer : LenientSerializer<TransitToNextResponseDto?>(
    TransitToNextResponseDto.serializer().nullable,
    null,
)
