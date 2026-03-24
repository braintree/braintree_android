# Braintree Android SDK — Claude Code Context

## Project Overview

The Braintree Android SDK is a multi-module Kotlin library that enables merchants to accept payments (credit card, PayPal, Venmo, Google Pay, 3D Secure, and more) in Android apps. It is distributed via Maven Central as individual AAR artifacts under the `com.braintreepayments.api` group.

- **Minimum Android SDK**: API 23
- **Target/Compile SDK**: API 36
- **Kotlin version**: 1.9.10
- **Java version**: 17
- **Gradle version**: 8.9.1 (via wrapper)
- **Current active major version**: v5.x

## Repository Structure

```
BraintreeCore/          # Core networking, config, auth, analytics
Card/                   # Credit/debit card tokenization
PayPal/                 # PayPal checkout and vault flows
Venmo/                  # Venmo app switch integration
GooglePay/              # Google Pay integration
ThreeDSecure/           # 3D Secure via Cardinal SDK
AmericanExpress/        # AmEx rewards balance
DataCollector/          # Fraud detection (Kount)
LocalPayment/           # Local/alternative payment methods
SEPADirectDebit/        # SEPA Direct Debit payments
ShopperInsights/        # Shopper enrichment data
PayPalMessaging/        # Pay Later messaging UI
UIComponents/           # Branded PayPal/Venmo buttons with full flows
VisaCheckout/           # Visa Checkout integration
SharedUtils/            # Internal HTTP, validation, URL utilities
TestUtils/              # Shared mock builders and test fixtures
Demo/                   # Reference implementation app
gradle/                 # Version catalog (libs.versions.toml) and wrapper
detekt/                 # Static analysis configuration
.github/workflows/      # GitHub Actions CI workflows
```

## Build & Development Setup

```bash
# Build the whole project
./gradlew build

# Build a specific module
./gradlew :Card:build

# Generate API docs (Dokka)
./gradlew dokkaHtmlMultiModule
```

Open the project root in Android Studio. No separate setup step is needed — Gradle handles all dependency resolution.

## Running Tests

```bash
# All unit tests
./gradlew --continue clean testRelease

# Unit tests for a specific module
./gradlew :Card:testRelease

# Instrumentation tests (requires connected device or emulator)
./gradlew --continue connectedAndroidTest

# Lint + Detekt (must pass before merging)
./ci lint
```

CI uses the `./ci` shell script as a wrapper. The three subcommands are `unit_tests`, `integration_tests`, and `lint`.

Instrumentation tests run on API 23, 31, and 35 emulators (x86_64, Pixel 7 Pro profile).

## Architecture & Key Patterns

### BraintreeClient as Central Hub
All payment method clients accept a `BraintreeClient` instance in their constructor. `BraintreeClient` owns:
- HTTP and GraphQL networking
- Authorization management (tokenization key vs. client token)
- Configuration loading and caching
- Analytics
- Browser switch coordination

Do not duplicate any of these concerns in individual payment clients.

### Client / Callback Split
Every public payment method module exposes a `[Payment]Client` class. Public methods accept a functional interface callback:

```kotlin
// Public API (callback-based)
fun tokenize(card: Card, callback: CardTokenizeCallback)

// Internal implementation (coroutine-based)
suspend fun tokenize(card: Card): CardResult
```

Implement the suspend function as the primary path; wrap it in a `coroutineScope.launch { }` block inside the callback method. This allows both styles to coexist without duplicating logic.

### Sealed Class Results
All async operations return a sealed result class — never throw exceptions into callback paths:

```kotlin
sealed class CardResult {
    class Success(val nonce: CardNonce) : CardResult()
    class Failure(val error: Exception) : CardResult()
}

// Some flows also have a Cancel state:
sealed class PayPalResult {
    class Success(val nonce: PayPalAccountNonce) : PayPalResult()
    class Failure(val error: Exception) : PayPalResult()
    data object Cancel : PayPalResult()
}
```

### Constructor Overloading for Testability
Public constructors take only `(Context, authorization: String)`. Internal constructors accept all dependencies for injection during testing:

```kotlin
// Public
constructor(context: Context, authorization: String) :
    this(BraintreeClient(context, authorization))

// Internal (full DI)
internal constructor(
    braintreeClient: BraintreeClient,
    apiClient: CardApiClient,
    analyticsParamRepository: AnalyticsParamRepository,
    dispatcher: CoroutineDispatcher,
    coroutineScope: CoroutineScope
)
```

Always provide both constructors when adding a new client.

### Parcelable Nonces
All `PaymentMethodNonce` subclasses (`CardNonce`, `PayPalAccountNonce`, etc.) implement `Parcelable` via the Kotlin Parcelize plugin. This is required for IPC across Activity boundaries.

### Analytics
Each client tracks events using constants on the module's analytics object (e.g., `CardAnalytics.CARD_TOKENIZE_STARTED`). Call `braintreeClient.sendAnalyticsEvent(...)` on every success/failure path.

### Configuration Management
`BraintreeClient` prefetches and caches configuration at construction time via `MerchantRepository` (singleton). Do not fetch configuration manually from within payment clients — call `braintreeClient.getConfiguration()` which returns the cached value.

## Coding Conventions

- **Packages**: `com.braintreepayments.api.<module>` (e.g., `com.braintreepayments.api.card`)
- **Class names**: PascalCase (`CardClient`, `PayPalAccountNonce`, `BraintreeException`)
- **Functions/properties**: camelCase (`tokenize`, `getConfiguration`)
- **Constants**: `UPPER_SNAKE_CASE` in `companion object`
- **Test files**: `*UnitTest.kt` for unit tests, `*Test.kt` for instrumentation tests
- Use `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` on internal APIs that must be visible across modules but not to consumers
- Use `@Suppress("SwallowedException")` only when catching and re-wrapping a known exception type
- Detekt is run in strict mode (`maxIssues: 0`) — all issues are treated as errors

## Naming Conventions

| Type | Pattern | Example |
|---|---|---|
| Clients | `[Name]Client` | `CardClient`, `PayPalClient` |
| Request/config models | `[Name]Request` or `[Name]Params` | `PayPalCheckoutRequest` |
| Nonce/result types | `[Name]Nonce` | `CardNonce`, `VenmoAccountNonce` |
| Result sealed classes | `[Name]Result` | `CardResult`, `PayPalResult` |
| Callback interfaces | `[Name]Callback` | `CardTokenizeCallback` |
| Exceptions | `[Name]Exception` | `BraintreeException`, `InvalidArgumentException` |
| Analytics constants | `[Name]Analytics` | `CardAnalytics`, `PayPalAnalytics` |

Note: Unlike the iOS SDK, Android classes do **not** use a `BT` prefix.

## Testing Conventions

- Use `MockK` for Kotlin classes and `Mockito` for Java interop cases.
- Use `TestUtils` module helpers (`MockkBraintreeClientBuilder`, `MockkApiClientBuilder`) to construct pre-configured mock clients.
- Use `Robolectric` for unit tests that require Android context — avoid spinning up an emulator for logic that doesn't need one.
- Use `kotlinx-coroutines-test` (`runTest`, `TestCoroutineDispatcher`) for testing suspend functions.
- Test both the `Success` and `Failure` branches of every sealed result.
- Verify analytics events are sent using captured call arguments on the mocked `BraintreeClient`.

## Error Handling

- Base exception: `BraintreeException(message, cause)` (extends `IOException`)
- Common exceptions: `InvalidArgumentException`, `ConfigurationException`, `AppSwitchNotAvailableException`, `UserCanceledException`
- Wrap caught exceptions into the `Failure` branch of the appropriate sealed result class — never propagate raw exceptions through callback paths.
- Do not add new `Exception` subclasses unless the existing types don't cover the scenario.

## Adding a New Module

1. Create the module directory and `build.gradle` following an existing module (e.g., `Card/`) as a template.
2. Register it in `settings.gradle`.
3. Add any new dependency versions to `gradle/libs.versions.toml`.
4. Create `src/main/java/com/braintreepayments/api/<module>/` for sources.
5. Create `src/test/java/...` for unit tests.
6. Follow the Client/Callback split, sealed result, and constructor overloading patterns above.
7. Add the module to the release matrix in `.github/workflows/release.yml`.

## Dependencies

All versions are managed in `gradle/libs.versions.toml`. Do not hardcode version strings in individual `build.gradle` files.

**Key runtime dependencies:**
- `browser-switch` (3.4.0) — handles OAuth/web authentication return URLs
- `play-services-wallet` (19.4.0) — Google Pay
- `cardinal` (2.2.7-7, via JFrog Artifactory) — 3D Secure certification (do not update without testing 3DS flows)
- `paypal-messages` (1.1.13) — Pay Later messaging UI

**Development/test only:**
- `robolectric`, `mockito-core`, `mockk`, `test-parameter-injector`, `jsonassert`, `kotlinx-coroutines-test`

## CI / GitHub Actions

| Workflow | Trigger | What it checks |
|---|---|---|
| `tests.yml` | Pull request | Unit tests (`./ci unit_tests`) |
| `instrumentation_tests.yml` | Pull request + manual | Instrumentation tests on API 23/31/35 emulators |
| `static_analysis.yml` | Pull request + manual | Android Lint + Detekt |
| `release.yml` | Manual (version input) | Full build + tests + Maven Central publish |
| `release_snapshot.yml` | Manual | Snapshot publish |

All PRs must pass unit tests, instrumentation tests, and static analysis before merging.

## Key Files

- `gradle/libs.versions.toml` — all dependency and plugin versions; update here first
- `settings.gradle` — module registration; add new modules here
- `detekt/detekt-config.yml` — static analysis rules
- `DEVELOPMENT.md` — environment setup, test commands, architecture overview
- `CONTRIBUTING.md` — PR guidelines and merchant server setup notes
- `CHANGELOG.md` — version history
- `v5_MIGRATION_GUIDE.md` — migration from v4.x to v5.x
- `APP_LINK_SETUP.md` — Android App Links configuration for return URLs
- `./ci` — shell script wrapping common Gradle commands used in CI
