---
name: dependency-checker
description: Checks if DEPENDENCIES.md is out of date by comparing it against gradle/libs.versions.toml, the root build.gradle, and each module's build.gradle. Reports any version mismatches, missing dependencies, removed dependencies, or drift in the merchant-facing build requirements table.
---

# Dependency Checker

Audits `DEPENDENCIES.md` against the actual Gradle build files to detect drift.

## When to Use This Skill

- After updating a dependency version in `libs.versions.toml`
- After adding or removing a dependency from a module's `build.gradle`
- Before a release to verify the dependency doc is accurate
- During PR review when build files have changed

## What This Skill Does

1. **Reads `DEPENDENCIES.md`** and extracts the per-module dependency tables (dependency name + version) and the "Build Requirements (Merchant-Facing)" table
2. **Reads `gradle/libs.versions.toml`** to get the declared versions
3. **Reads the root `build.gradle`** to get the merchant-facing build settings (`compileSdk`, `minSdk`, `targetCompatibility`)
4. **Reads each module's `build.gradle`** to get the actual `api` and `implementation` dependencies
5. **Resolves BOM-managed versions** by running `./gradlew :<module>:dependencies --configuration releaseRuntimeClasspath` when a dependency has no explicit version in the toml (e.g., Compose libraries managed by `androidx.compose:compose-bom`)
6. **Compares** and reports:
   - **Version mismatches**: dependency exists in both but the version in `DEPENDENCIES.md` differs from the source of truth
   - **Missing from doc**: dependency is in a `build.gradle` but not listed in `DEPENDENCIES.md`
   - **Removed / stale**: dependency is listed in `DEPENDENCIES.md` but no longer in the `build.gradle`
   - **Build requirement drift**: a value in the "Build Requirements (Merchant-Facing)" table differs from its source of truth (see the dedicated section below)
7. **Prints a summary** of all findings, grouped by module
8. **Offers to update `DEPENDENCIES.md`** with the corrected values if any drift is found

## How to Use

```
/dependency-checker
```

```
Check if DEPENDENCIES.md is up to date
```

```
/dependency-checker — only check the Card and PayPal modules
```

## Excluded Modules

Skip deprecated or legacy modules — they should NOT appear in `DEPENDENCIES.md`. Currently excluded:

- `VisaCheckout`

If a deprecated module is found in `DEPENDENCIES.md`, flag it for removal. If a deprecated module is missing from the doc, do not report it as a missing entry.

## Build Requirements (Merchant-Facing)

`DEPENDENCIES.md` has a "Build Requirements (Merchant-Facing)" table near the top listing the build settings that get baked into the published AARs and therefore constrain the consuming merchant app. Each row has a different source of truth — none of them come from a module's dependency block:

| Row in `DEPENDENCIES.md` | Source of truth |
|---|---|
| `compileSdk` | Root `build.gradle` → `ext { compileSdkVersion = ... }` |
| `minSdk` | Root `build.gradle` → `ext { minSdkVersion = ... }` |
| Java bytecode level (`targetCompatibility`) | Root `build.gradle` → `sdkTargetJavaVersion = JavaVersion.VERSION_XX` (feeds `javaTargetCompatibility`) |
| Kotlin version | `gradle/libs.versions.toml` → `kotlin` |
| Android Gradle Plugin | `gradle/libs.versions.toml` → `androidGradlePlugin` |

To verify this section:
1. Read the root `build.gradle` and `gradle/libs.versions.toml` for the five values above
2. Compare each against the table in `DEPENDENCIES.md`
3. Report any mismatch as **build requirement drift** and offer to correct it
4. Only these merchant-facing settings belong in the table — do NOT add internal-only build tooling (Gradle wrapper, Dokka, Detekt, KSP, publishing plugins), since those never reach the consuming app

Note: the Java bytecode level is stored as `JavaVersion.VERSION_11` in the root `build.gradle` but written as the bare number (`11`) in `DEPENDENCIES.md` — compare the numeric value, not the literal string.

## browser-switch Dependencies

`DEPENDENCIES.md` has a dedicated section for `browser-switch` transitive dependencies (between "Key Third-Party Libraries" and "Per-Module Dependencies"). This section lists the direct runtime dependencies of `com.braintreepayments.api:browser-switch`.

To verify this section:
1. Run `./gradlew :BraintreeCore:dependencies --configuration releaseRuntimeClasspath` and find the `browser-switch` subtree
2. Extract the direct dependencies (first-level children under `browser-switch`)
3. Compare the dependency names and versions against what `DEPENDENCIES.md` lists
4. Report mismatches, missing, or stale entries the same way as per-module checks

## Tips

- The source of truth for versions is `gradle/libs.versions.toml` and the Gradle dependency resolution, not `DEPENDENCIES.md`
- Internal module dependencies (`:BraintreeCore`, `:SharedUtils`, etc.) don't have versions — they ship with the SDK
- Some Compose dependencies are BOM-managed; their versions come from `androidx.compose:compose-bom` declared in `UIComponents/build.gradle`
- The bundled Magnes SDK version is embedded in the JAR filename at `DataCollector/libs/`