# Conventions

코드/Git 작성 시 바로 보는 규칙.
구조/의존/흐름은 [ARCHITECTURE.md](ARCHITECTURE.md) 를 본다.

기준:
- 팀 FE 컨벤션 (Git commit / branch) — iOS 와 동일
- Kotlin 공식 코딩 컨벤션 + 현재 스택(Compose, MVI, Coroutines/Flow, Hilt)

---

## a. Coding Convention

### 1. 코드 레이아웃

포맷 자동정렬은 두지 않는다. 정렬은 Android Studio 기본 코드 스타일(`kotlin.code.style=official`) 에 맡긴다 (iOS 가 Xcode 기본 정렬에 맡긴 것과 같다).
정적 분석·안전성은 [../config/detekt/detekt.yml](../config/detekt/detekt.yml) 이 유일본이다 — 복잡도·긴 함수/클래스·줄 길이·널 안전성·네이밍만 본다. 임계값은 iOS `.swiftlint.yml` 을 옮겼다 (복잡도 12, 함수 90줄, 클래스 250줄, 줄 120자). `./gradlew detekt` 로 돌린다.

문서가 정하는 것은 detekt 가 안 잡는 둘뿐이다.

- 한 줄이 길면 파라미터/인자 기준으로 줄바꿈한다 (trailing comma 허용)
- `// region` 대신 최상위 선언 순서(공개 → 비공개) 로 섹션을 가른다

### 2. 명명

공통:

| 대상 | 규칙 |
|---|---|
| 타입 (class/interface/object) | `UpperCamelCase` |
| 함수/변수/프로퍼티 | `lowerCamelCase` |
| 상수 (`const`/top-level val) | `UPPER_SNAKE_CASE` |
| enum / sealed case | `UpperCamelCase` |
| Composable 함수 | `UpperCamelCase` (예외적으로 대문자) |
| Boolean | `is` / `has` / `should` / `can` |
| 약어 | 시작이면 소문자, 이어지면 대문자 (`userId`, `urlString`) |
| 함수명 앞 `get` | 금지 (프로퍼티가 아니라면) |

```kotlin
// ✅
fun name(user: User): String
suspend fun fetchUser(): User

// ❌
fun getName(user: User): String
```

계층 타입 (iOS ↔ Android 매핑은 [ARCHITECTURE.md](ARCHITECTURE.md) §1):

| 계층 | 패턴 | 예 |
|---|---|---|
| Domain entity | 명사 data class | `AuthSession` |
| Domain 계약 | `*Repository` (interface) | `AuthRepository` |
| Domain error | `*Error` (sealed) | `AuthError` |
| Data DTO | `*Dto` | `AuthSessionDto` |
| Data data source | `*DataSource` / `*Api` | `AuthLocalDataSource` |
| Data 구현 | `*RepositoryImpl` | `AuthRepositoryImpl` |
| DI 바인딩 | `*Module` | `AuthModule` |
| Feature 화면 | `*Screen` | `HomeScreen` |
| Feature VM | `*ViewModel` | `HomeViewModel` |
| Feature 계약 | `*Intent` / `*State` / `*SideEffect` | `HomeIntent` |

DataSource 프로퍼티:

```text
authLocal
authRemote
```

MVI Intent 는 이벤트 중심 (iOS TCA Action 과 동일한 철학):

```text
LoginButtonClicked
LogoutButtonClicked
Appeared
```

### 3. 타입 / 함수 / 가시성

- 상속 의도 없으면 class 는 열지 않는다 (Kotlin 기본 final 유지)
- 가시성은 좁게 (`private` 우선, 모듈 밖 노출만 `public`)
- 데이터는 `data class` / `value class`, 상태 분기는 `sealed interface`
- 파라미터/리턴 없는 콜백: `() -> Unit`
- 스레드는 Coroutine/Flow 우선, 콜백/`Thread` 남용 금지
- nullable 최소화, `!!` 금지 (불가피하면 이유 주석)

```kotlin
val onClick: () -> Unit = { ... }
```

### 4. 주석

| 종류 | 사용 |
|---|---|
| `/** */` (KDoc) | public API / 타입 의도 |
| `//` | 비자명한 이유만 |
| `// TODO:` | 최소화 |

쓸데없는 주석 금지. 주석은 명사형으로 끝내고 마침표를 붙이지 않는다 (iOS 코드 스타일과 동일).

### 5. Compose / MVI

- Composable 은 작고 단순하게, 상태를 소유하지 않는다 (stateless 우선)
- Screen 은 `state: State` 를 받고 `onIntent: (Intent) -> Unit` 만 상위로 낸다
- 화면 상태 본체는 `ViewModel` 의 `StateFlow<State>` 가 소유
- 부수효과(네비게이션·토스트·단발 이벤트) 는 `SideEffect` 로 내보내고 `CollectSideEffect` 로 받는다
- 상태 구독은 `collectAsStateWithLifecycle()`
- interface 정의는 `domain/`, live 바인딩은 `di/` (Hilt)

```kotlin
@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Button(onClick = { onIntent(HomeIntent.LoginButtonClicked) }) {
        Text("로그인")
    }
}
```

### Feature Logging

Feature 로그는 케이스별 수동 호출이 아니라 `MviViewModel` 공통 지점에서 자동으로 남긴다.

종류: 사용자 Intent / 상태 변경 / 화면 이동 / 오류.
토큰/Authorization/identityToken 금지, userId/provider/route 는 허용.

메시지 포맷:

```text
[Feature] [Auth] 사용자 액션: LoginButtonClicked(provider=kakao)
[Feature] [Auth] 상태 변경: isLoading(false → true)
[Feature] [Root] 화면 이동: bootstrapping → main
[Feature] [Auth] 오류: login(network, userVisible=true)
```

출력 규칙:

1. 전역 직접 로그는 호출 지점 태그를 남긴다
   예: `[App] [DulpickApp.kt:24] onCreate() - App bootstrap completed`
2. 모듈 래퍼 로그는 본문만 출력한다
   예: `[Network] [Response] ← 200 /api/v1/auth/reissue (617ms, 367B)`

### 6. 폴더

Feature 폴더 배치는 [ARCHITECTURE.md](ARCHITECTURE.md) §3 규칙을 본다.

공용 코드를 어디 둘지는 세 조건으로 판정한다. 셋 다 만족해야 DesignSystem(`ui/theme`·`designsystem`) 이다.

| 조건 | 질문 |
|---|---|
| 형태 | Composable·스타일·디자인 토큰인가 (모델·유틸·확장은 아니다) |
| 의존 | Domain·Hilt·서드파티 SDK 를 모르는가 |
| 결합 | 앱 고유 모델·문구·URL 을 모르는가 (호출자가 넘기는가) |

하나라도 어기면 `feature` 다. 단 외부 SDK 를 감싸면서 앱 고유 모델을 모르는 것은
화면용 인프라이므로 `core/` 에 둔다.
업무 뜻이 없는 순수한 값(위도·경도만 든 좌표 등)은 `domain/` 이 아니라 `core/` 유틸에 둔다.

규칙:

1. 지금은 `:app` 단일 모듈, 기능 분리는 패키지만
2. Screen / ViewModel / Contract 는 `feature/<name>/` 안에 flat
3. 파일명 = 메인 타입명 (`HomeScreen.kt`)
4. 커스텀 패키지는 단수 (`model`, `repository`, `feature`)
5. 패키지는 소문자, 밑줄 없이

### 7. Import / DI

계층별 허용·금지 의존은 [ARCHITECTURE.md](ARCHITECTURE.md) §1 을 본다.

DI (Hilt):

1. Feature 의 데이터 접근은 Domain `*Repository` interface 만 (`@Inject`)
2. interface ↔ Impl 바인딩은 `di/` Hilt `@Module` only. 외부 SDK 초기화는 SDK 를 소유한 계층이 갖고, `DulpickApp` 은 호출만
3. `DulpickApp` 에 `@HiltAndroidApp`, Activity 에 `@AndroidEntryPoint`, VM 에 `@HiltViewModel`
4. Composable 안에서 의존을 직접 생성하지 않는다 (`hiltViewModel()` 로 주입)
5. Feature 코드에 `*RepositoryImpl` / `*Dto` / DataSource 이름 쓰지 않음
6. `@Binds` 로 계약↔구현, 외부 객체는 `@Provides`. 스코프는 필요 최소(`@Singleton` 남용 금지)

### 8. 화면 통신

```text
Root NavHost
├─ 앱 인트로 (AppIntro)
├─ 온보딩 (Auth / Nickname / Couple / DateType)
├─ 메인 탭 (Home / Explore / Map / MyPage)
└─ 딥링크
```

허용:

```text
Screen → 상위      : SideEffect 로 이벤트 올림
상위 → Screen      : state / 목적지 인자
Screen → Domain    : *Repository (VM 경유)
URL → Route 파싱 → NavController
```

금지:

```text
ScreenA → ScreenB 직접 참조/상태 수정
Screen 에서 현재 목적지/탭 직접 변경 (SideEffect 로 올린다)
Screen 에서 URL 파싱 후 다른 Screen 진입
```

```kotlin
sealed interface HomeSideEffect {
    data class NavigateToDetail(val id: String) : HomeSideEffect
    data object SessionExpired : HomeSideEffect
}
```

로컬 에러는 화면 `state.errorMessage`.
전역 에러(`sessionExpired` 등)만 최상위로 승격.

### 9. 테스트

```text
ViewModel 테스트 (StateFlow 검증)
Domain *Repository 를 fake/mock 으로 대체
test_한글_한글
```

```text
test_세션없음_로그아웃상태복구
test_로그인성공_사이드이펙트_전달
```

규칙:

1. ViewModel 테스트에 Data/`*Impl` 구현을 끌어오지 않는다 (fake interface)
2. Coroutine 테스트는 `runTest` + `MainDispatcherRule`
3. Domain/Data 단위 테스트는 기본 강제 없음, 고위험 도메인만 보완
4. Compose UI 테스트(`connectedAndroidTest`) 는 핵심 플로우만

### 10. 새 기능 체크

1. Domain `{Entity, *Error, *Repository interface}` — `domain/`
2. Data `{Dto, DataSource, RepositoryImpl, Mapper}` — `data/`
3. `di/` Hilt `@Module` (`@Binds`/`@Provides`)
4. Feature `feature/<name>/` — `Screen` + `ViewModel` + `Contract`
5. 필요 시 navigation / 딥링크 Route
6. ViewModel 테스트

```text
화면? feature/
계약? domain/
구현? data/
인프라(데이터가 쓴다)? data/ 또는 core/
인프라(화면이 직접 쓴다)? core/
외부 SDK? Gradle 의존 + 소유 계층 래퍼
조립? di/ + DulpickApp
```

상세 구조는 [ARCHITECTURE.md](ARCHITECTURE.md).
환경·build type·applicationId 는 [ARCHITECTURE.md](ARCHITECTURE.md) §5 를 본다.

---

## b. Git Commit Convention

### 1. 스타일

형식:

```text
Type: 요약
```

| Type | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 의미 없는 스타일만 변경 |
| `refactor` | 리팩토링 |
| `test` | 테스트 추가/수정 |
| `chore` | 설정 변경 등 코드 의미 없는 작업 |

예:

```text
feat: 로그인 버튼 추가
fix: 딥링크 파싱 실패 수정
docs: 컨벤션에 git 규칙 반영
chore: 프로젝트 세팅 스켈레톤 정리
```

필요하면 본문과 이슈 참조를 추가한다.

```text
feat: 사용자 프로필 화면 추가

- 프로필 이미지, 이름 표시
- 팔로우 버튼 추가

Ref: DND-10
```

### 2. 권장

- 커밋은 기능 단위로 쪼갠다
- 첫 줄은 변경 내용이 보이게 쓴다
- 관련 없는 변경을 한 커밋에 섞지 않는다
- `WIP`, `update`, `fix bug` 같은 모호한 메시지 금지

---

## c. Git Branch Strategy

### 1. 브랜치 이름

형식:

```text
Type/Jira티켓
```

```text
feat/dnd-10
fix/dnd-25
refactor/dnd-42
docs/agent-docs
```

- Type 은 commit type 과 동일
- jira key 는 소문자
- Jira 키가 없으면 브랜치 이름에 `no-issue` 를 넣지 않는다. `docs/agent-docs` 처럼 내용을 쓴다.
  `[NO-ISSUE]` 는 PR 제목에만 붙인다

### 2. 브랜치 구조

```text
main
 └ dev
    └ feat/dnd-10
```

| 브랜치 | 역할 |
|---|---|
| `main` | 배포 가능 상태 |
| `dev` | 개발 통합 |
| `Type/Jira티켓` | 작업 브랜치 |

### 3. PR 흐름

1. `dev` 에서 작업 브랜치 생성
2. 기능 단위로 커밋
3. `dev` 로 PR
4. 리뷰 + CI 통과 후 merge
5. 릴리즈 시점에 `dev` → `main`

PR 제목:

```text
[DND-10] 로그인 화면 추가
[NO-ISSUE] 프로젝트 세팅
```

템플릿: [../.github/pull_request_template.md](../.github/pull_request_template.md)

머지 조건:

1. CI 통과
2. 아키텍처 의존 위반 없음
3. 가능하면 리뷰, 급하면 self-merge 허용

---

## d. 관련

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [../AGENTS.md](../AGENTS.md) — 하지 말 것은 `먼저 물을 것` 을 본다
