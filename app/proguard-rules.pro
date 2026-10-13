# R8 규칙. 릴리스에서 난독화·미사용 코드 제거를 켜고, 반사로 찾는 것만 남긴다.
# 규칙을 넓게 쓰면 난독화 비율이 떨어지니 필요한 만큼만 남긴다.

# ── 크래시 추적
# 줄 번호를 남겨 Play 콘솔에서 스택트레이스를 읽을 수 있게 한다.
# 원본 파일명은 가린다 (mapping.txt 로 복원)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── 제네릭·어노테이션 정보
# Retrofit 이 응답 타입을, 직렬화가 @SerialName 을 이 정보로 읽는다
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# ── kotlinx.serialization
# @Serializable 이 만든 serializer 를 이름으로 찾는다. 지워지거나 이름이 바뀌면 JSON 파싱이 깨진다
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── 서버 문자열을 상수 이름으로 맞추는 enum 들(상태·다음 동작·카테고리 등)
-keepclassmembers enum * { *; }

# ── Retrofit
# 어노테이션이 붙은 인터페이스 메서드는 실행 중에 반사로 읽힌다
-keepclasseswithmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ── 카카오 지도·로그인 SDK
# 지도 엔진이 JNI 로 찾는 클래스가 있어 통째로 남긴다. 줄이면 지도가 뜨지 않는다
-keep class com.kakao.vectormap.** { *; }
-keep class com.kakao.sdk.** { *; }
-dontwarn com.kakao.**
