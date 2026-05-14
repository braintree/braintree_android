---
name: dependency-checker
description: Checks if DEPENDENCIES.md is out of date by comparing it against gradle/libs.versions.toml and each module's build.gradle. Reports any version mismatches, missing dependencies, or removed dependencies.
---

# Dependency Checker

Audits `DEPENDENCIES.md` against the actual Gradle build files to detect drift.

## When to Use This Skill

- After updating a dependency version in `libs.versions.toml`
- After adding or removing a dependency from a module's `build.gradle`
- Before a release to verify the dependency doc is accurate
- During PR review when build files have changed

## What This Skill Does

1. **Reads `DEPENDENCIES.md`** and extracts the per-module dependency tables (dependency name + version)
2. **Reads `gradle/libs.versions.toml`** to get the declared versions
3. **Reads each module's `build.gradle`** to get the actual `api` and `implementation` dependencies
4. **Resolves BOM-managed versions** by running `./gradlew :<module>:dependencies --configuration releaseRuntimeClasspath` when a dependency has no explicit version in the toml (e.g., Compose libraries managed by `androidx.compose:compose-bom`)
5. **Compares** and reports:
   - **Version mismatches**: dependency exists in both but the version in `DEPENDENCIES.md` differs from the source of truth
   - **Missing from doc**: dependency is in a `build.gradle` but not listed in `DEPENDENCIES.md`
   - **Removed / stale**: dependency is listed in `DEPENDENCIES.md` but no longer in the `build.gradle`
6. **Prints a summary** of all findings, grouped by module
7. **Offers to update `DEPENDENCIES.md`** with the corrected versions if any drift is found

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