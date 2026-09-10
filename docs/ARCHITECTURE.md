# Dulpick Android Architecture

iOS 원본을 Kotlin/Compose 로 옮긴 아키텍처. 계약(interface)·구현·화면의 경계는 iOS 와 같고, 표현 기술만 다르다.

```text
계층 = 패키지 (지금은 :app 단일 모듈)
화면 = feature/<이름> 폴더
외부 SDK = 소유 계층이 감싸고 di/ 에서 주입
Domain interface live 바인딩 = di/ Hilt 모듈 only
외부 SDK 초기화 = 소유 계층, DulpickApp 은 호출만
```

iOS 대비 스택 매핑:

| iOS | Android |
|---|---|
| SwiftUI | Jetpack Compose |
| TCA (Reducer/Store/Action/State) | MVI (`MviViewModel` / Intent / State / SideEffect) |
| DependencyClient (`@Dependency`) | Hilt (`@Inject`, `@Module`, `@Binds`) |
| Tuist 모듈 | 패키지 (필요 시 Gradle 모듈로 승격) |
| SwiftPM | Gradle Version Catalog (`libs.versions.toml`) |
| `.xcconfig` / scheme | `build type` (debug/release) + `manifestPlaceholders` |

---

## 1. 계층

지금은 `:app` 단일 Gradle 모듈이다. iOS 의 모듈은 `com.dulpick.app` 아래 패키지로 대응한다.

```text
com.dulpick.app/
  DulpickApp.kt            # @HiltAndroidApp, 부트스트랩·초기화 호출
  MainActivity.kt          # setContent { }, 루트 Compose 진입

  core/                    # 화면이 직접 쓰는 공통 토대 (iOS SharedLogger + CoreUI 성격)
    mvi/                   # MviViewModel, CollectSideEffect
    ...                    # 로거, 공통 인프라 래퍼

  ui/theme/                # 디자인 토큰 (Color, Theme, Type) — iOS SharedDesignSystem
  designsystem/            # 공용 Composable 컴포넌트 (필요 시)

  domain/                  # Entity, *Repository interface, *Error — iOS Domain
  data/                    # DTO, DataSource, *RepositoryImpl, Mapper — iOS Data

  di/                      # Hilt @Module — iOS App 의 live 주입 대응

  feature/<name>/          # 화면 하나. Screen + ViewModel + Contract
    navigation/            # NavHost, Route (전역 이동·딥링크)
```

### iOS 모듈 → Android 매핑

| iOS 모듈 | Android 위치 | 비고 |
|---|---|---|
| SharedUtils | `core/` (util) | 순수 공통 코드 |
| SharedDesignSystem | `ui/theme`, `designsystem/` | 토큰·컴포넌트 |
| SharedLogger | `core/` (log) | 공통 로거 |
| ThirdParty* | Gradle 의존 (`libs.versions.toml`) | 별도 계층 안 둠 |
| Domain | `domain/` | Entity, interface, Error |
| Core/* (Network/Storage/…) | `data/` 하위 또는 `core/` | 데이터가 쓰는 인프라 |
| CoreUI/* (KakaoMap/Analytics/…) | `core/` | 화면이 직접 쓰는 인프라 |
| Data | `data/` | DTO, DataSource, Impl |
| Feature | `feature/<name>/` | 화면 |
| App | `DulpickApp`, `MainActivity`, `di/` | 조립 |

### 계약/구현 네이밍 (iOS ↔ Android)

| iOS | Android | 위치 |
|---|---|---|
| `*Client` (포트/계약) | `*Repository` (interface) | `domain/` |
| `*Repository` (구현) | `*RepositoryImpl` | `data/` |
| `*ClientFactory` (생성) | Hilt `@Binds` / `@Provides` | `di/` |
| Entity | data class | `domain/` |
| DTO | `*Dto` (`@Serializable`) | `data/` |
| DataSource | `*DataSource` / `*Api` | `data/` |

### 의존 방향

```text
feature   → domain, core, ui/theme, designsystem
data      → domain, core
domain    → core (util 만)          # Android/Compose 프레임워크를 모른다
core      → (순수 Kotlin / 최소 안드로이드)
di        → 조립 (전 계층을 안다)
```

### 금지

```text
feature → data (*RepositoryImpl, *Dto, DataSource 직접 참조)
domain  → data / feature / Compose / Hilt android
data    → feature
```

의존 규칙은 지금은 문서·리뷰로 지킨다. 강제가 필요해지면 Gradle 모듈 분리보다 아키텍처 테스트(Konsist 등) 를 먼저 붙인다.

---

## 2. 런타임

```text
DulpickApp (@HiltAndroidApp)
  → 외부 SDK 초기화 (소유 계층 호출)
  → Hilt 그래프 구성 (di/*Module)
MainActivity
  → setContent { DulpickTheme { DulpickNavHost(...) } }
  → 최상위 ViewModel 이 세션 복구 → 시작 목적지 결정
```

앱 상태 (iOS phase 대응):

```text
bootstrapping
  → authRepository.restoreSession()
  → session 있음: 메인 (Home 등 탭)
  → session 없음 + !hasSeenAppIntro: 앱 인트로
  → session 없음 + hasSeenAppIntro: 온보딩(Auth)

앱 인트로 진입 → markAppIntroSeen()
앱 인트로 완료 → 온보딩(Auth)
```

Compose Navigation 의 `startDestination` 을 이 판정 결과로 정하거나, 로딩 목적지에서 관찰 후 이동한다.

---

## 3. Feature (MVI)

화면 하나 = `feature/<name>/` 패키지. 안은 flat.

구성:

```text
feature/home/
  HomeScreen.kt        # @Composable, state 렌더 + intent 전달
  HomeViewModel.kt     # MviViewModel<HomeIntent, HomeState, HomeSideEffect>
  HomeContract.kt      # HomeIntent(sealed), HomeState(data class), HomeSideEffect(sealed)
```

규칙:

1. Screen 은 `state` 를 받고 `onIntent` 로만 상위와 통신. 로직을 두지 않는다
2. Screen 간 직접 참조 금지
3. 전역 이동·상위 요청은 `SideEffect` 로 올려 navigation 이 처리한다
4. 데이터 접근은 Domain `*Repository` interface 만 (`@Inject`)
5. 전역 전환·딥링크는 `feature/*/navigation` 또는 최상위 NavHost 에서
6. 공용 코드는 `core/` 로 뺀다

네비게이션:

```text
Root NavHost → 탭 그래프 → 화면 목적지 → 다이얼로그/바텀시트(overlay)
```

딥링크:

```text
URL(intent-filter) → Route 파싱 → NavController 이동
bootstrapping / 인트로 / 온보딩 이면 pending, 로그인 후 flush
인트로 / 온보딩 은 home|explore|map|myPage 만 pending, signIn 은 무시
```

딥링크 스킴은 build type 으로 가른다: release `dulpick`, debug `dulpickdebug` (`manifestPlaceholders["deepLinkScheme"]`).

---

## 4. Domain / Data

새 기능의 폴더 구성은 [CONVENTIONS.md](CONVENTIONS.md) §10 을 본다.

규칙:

1. Domain `*Repository` = 계약(interface). Compose·Hilt·안드로이드 프레임워크를 모른다
2. Data `*RepositoryImpl` = 구현. `@Inject constructor`
3. `di/` Hilt `@Binds` 로 interface ↔ Impl 연결 (iOS `*ClientFactory` 대응)
4. Impl 은 기본적으로 DataSource 만 주입. SDK credential provider 등은 collaborator 로 허용
5. DataSource 프로퍼티는 `authLocal`, `authRemote`
6. Core/인프라 에러(`IOException`, HTTP 등) 는 Data 에서 Domain 에러로 매핑
7. 여러 Repository 조합은 Feature/최상위에서 처리

---

## 5. App / Config

설정:

```text
build.gradle.kts (android {})   → applicationId, minSdk/targetSdk, build type, flag
manifestPlaceholders            → deepLinkScheme 등 build type 별 값
local.properties / secrets      → API base url, 키 (커밋 금지)
BuildConfig                     → 코드에서 읽는 빌드 값
```

| Build type | applicationId | 딥링크 스킴 |
|---|---|---|
| `debug` | `com.dulpick.debug` | `dulpickdebug` |
| `release` | `com.dulpick.app` | `dulpick` |

base `applicationId = "com.dulpick"` 에 build type 별 suffix(`.debug`/`.app`) 를 붙여 iOS 와 동일한 id 를 만든다.
debug 는 배포 빌드와 나란히 설치된다 (iOS Dulpick-Debug/Dulpick 대응). `namespace` 는 `com.dulpick.app` 로 코드 패키지용이며 applicationId 와 무관하다.
저장소 namespace 는 applicationId 하나를 재사용한다.

---

## 6. 관련

- [CONVENTIONS.md](CONVENTIONS.md)
- [../AGENTS.md](../AGENTS.md)
- [../CLAUDE.md](../CLAUDE.md) (`AGENTS.md` symlink)

네이밍·새 기능 순서·테스트 규칙·DesignSystem 판정은 [CONVENTIONS.md](CONVENTIONS.md) 를 본다.

구조는 이 파일, 코딩 규칙은 `CONVENTIONS.md` 가 source of truth 다.
