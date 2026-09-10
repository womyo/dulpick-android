# AGENTS

Dulpick Android 작업 시 에이전트 진입점.
iOS 원본(`~/project/dnd-15th-1-ios`)을 Kotlin/Compose + MVI + Hilt 로 포팅한다.

---

## 어디를 보나

| 질문 | 문서 |
|---|---|
| 모듈·의존·앱 흐름 | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 코딩·Git 규칙 | [docs/CONVENTIONS.md](docs/CONVENTIONS.md) |
| 정적 분석·안전성 | [config/detekt/detekt.yml](config/detekt/detekt.yml) (포맷은 Android Studio 기본 스타일) |
| 리뷰 기준 | [.coderabbit.yaml](.coderabbit.yaml) |

---

## 절대 규칙

1. Feature 는 화면 단위로 패키지를 나눈다. 지금은 `:app` 단일 모듈, 계층은 패키지로 가른다
2. 화면(Screen) 간 직접 참조 금지. 상위 이동 요청은 `SideEffect` 로 올린다
3. Feature 의 데이터 접근은 Domain interface(`*Repository`) 만 쓴다. `*Impl` 이름을 Feature 에서 쓰지 않는다
4. Domain interface 의 live 바인딩은 `di/` Hilt 모듈에서만 한다. 외부 SDK 초기화는 SDK 를 소유한 계층이 갖고, `DulpickApp` 은 호출만 한다
5. Core/인프라 에러는 Data 에서 Domain 에러로 매핑한다
6. 전역 에러(`sessionExpired` 등)만 최상위(Root) 로 승격한다
7. minSdk 26 / compileSdk·targetSdk 36 / JVM 17 을 지킨다. Gradle 실행 JDK 는 17 (25 는 Gradle 8.11 이 지원 안 함)

---

## 먼저 물을 것

- 패키지 계층 → Gradle 멀티모듈 승격
- Feature → Data 직접 참조
- applicationId / 딥링크 스킴 / build type 이름 변경
- 새 서드파티 SDK 추가
- 문서 추가·분리

---

## 자주 쓰는 명령

```bash
# 동기화·빌드 (IDE 없이)
./gradlew assembleDebug

# 정적 분석 (포맷 자동정렬은 없음, 분석·안전성만)
./gradlew detekt

# 단위 테스트
./gradlew testDebugUnitTest

# 특정 테스트만
./gradlew testDebugUnitTest --tests "com.dulpick.app.*"

# 계기 테스트 (에뮬레이터/기기 필요)
./gradlew connectedDebugAndroidTest

# 딥링크 확인 (에뮬레이터 부팅 후)
adb shell am start -a android.intent.action.VIEW -d "dulpickdebug://home"
# 배포 빌드(release) 는 dulpick://home
```

Gradle 실행 JDK 는 `gradle.properties` 의 `org.gradle.java.home` 로 JBR 17 에 고정돼 있다.
IDE 는 Settings → Build Tools → Gradle → Gradle JDK 를 `jbr-17` 로 맞춘다.
