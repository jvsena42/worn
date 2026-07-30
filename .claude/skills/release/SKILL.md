---
name: release
description: Bump versions on a release branch, build a signed APK, tag, and publish a GitHub release
disable-model-invocation: true
argument-hint: "<version> (e.g. v0.2.0)"
---

Release process for Worn. Version: $ARGUMENTS

Worn is a Kotlin Multiplatform app, so a release bumps **both** platforms in the same commit even
though only the Android APK is published as a release asset.

`main` is protected — nothing is committed or pushed to it directly. The version bump goes on a
release branch and reaches `main` through a pull request; the tag is then created on the resulting
merge commit (this is how `v0.1.0` was released: branch `chore/version-0.1.0`, PR #21, tag on the
merge commit).

## Steps

1. **Validate version argument**: Ensure a version was provided (e.g. `v0.2.0`). It must start with
   `v` followed by semver. Abort if missing or malformed. Derive the numeric version by stripping
   the `v` prefix (e.g. `v0.2.0` -> `0.2.0`). Abort if the tag already exists (`git tag -l`).

2. **Pre-flight checks**:
   - Ensure the working tree is clean (`git status`). Abort if there are uncommitted changes.
   - Ensure you are on `main` and in sync with `origin/main` (`git fetch origin && git status`).
   - Run `./gradlew detekt`. Abort if it reports issues.
   - Run `./gradlew :shared:allTests`. Abort if tests fail.

3. **Create the release branch**: `git switch -c chore/version-<numeric_version>` off `main`.

4. **Bump versions (one commit, both platforms)**:
   - `composeApp/build.gradle.kts` — set `versionName` to the numeric version and increment
     `versionCode` by 1.
   - `iosApp/Configuration/Config.xcconfig` — set `MARKETING_VERSION` to the numeric version and
     `CURRENT_PROJECT_VERSION` to the **same number as the new Android `versionCode`**, keeping the
     two build numbers in lockstep. This xcconfig is the base configuration for both project-level
     build configs, so it covers the `iosApp` and `WornShareExtension` targets; the Info.plists
     carry no version keys (`GENERATE_INFOPLIST_FILE = YES`).
   - `Config.xcconfig` is tracked in git and its `TEAM_ID` is intentionally empty. Change only the
     two version lines, then check `git diff` shows nothing else — a locally filled `TEAM_ID` must
     never be committed.
   - Commit: `chore: bump version to <numeric_version>`.

5. **Open the pull request**:
   - `git push -u origin chore/version-<numeric_version>`.
   - `gh pr create` targeting `main`, title `chore: bump version to <numeric_version>`, body filled
     in from `.github/pull_request_template.md` (Summary, Changes, Test plan, Checklist).
   - Print the PR URL.

6. **Wait for the PR to merge**: `main` requires the `check` CI job to pass (`enforce_admins` is on,
   so there is no bypass), but requires no approving review — the PR can be merged as soon as CI is
   green. Watch it with `gh pr checks --watch`, then merge it, or use `gh pr merge --auto` if the
   user asks. Do not proceed until `gh pr view --json state` reports `MERGED`. Then
   `git switch main && git pull origin main`.

7. **Build signed APK** (from merged `main`, so the artifact matches the commit being tagged):
   - Confirm `local.properties` defines all four signing constants: `KEYSTORE_FILE`,
     `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Check that the **keys are present** only —
     never read, print, or echo their values. Abort with a clear message if any is missing, because
     the build would otherwise silently produce an unsigned APK.
   - Run `./gradlew clean :composeApp:assembleRelease`.
   - Verify the APK exists at `composeApp/build/outputs/apk/release/composeApp-release.apk`.
   - Verify it is actually signed: `apksigner verify <apk>` (or `jarsigner -verify`). Abort if the
     APK is unsigned.
   - Copy it to `Worn-<numeric_version>.apk` in the repo root for upload (`*.apk` is gitignored).

8. **Create git tag** on the merge commit now at the tip of `main`:
   - `git tag -a <version> -m "Release <version>"`.
   - `git push origin <version>` (tags push fine; only branch pushes to `main` are blocked).

9. **Generate changelog**:
   - Find the previous tag: `git describe --tags --abbrev=0 HEAD~1` (if no previous tag exists, use
     all commits).
   - List commits since the previous tag: `git log <previous_tag>..HEAD --oneline --no-merges`.
   - Write a short changelog as a bullet-point list summarizing the user-facing changes (group
     related commits, skip chore/CI-only commits — including this release's own version bump — and
     keep each bullet to one sentence in English).
   - Show the changelog to the user for approval before proceeding.

10. **Create GitHub release**:
    - Use `gh release create <version>` with the signed APK (`Worn-<numeric_version>.apk`) attached.
    - Title: `Worn <version>`.
    - Use the approved changelog as the release body (pass via `--notes`).
    - Mark as latest release.

11. **Clean up and summarize**: Delete the temporary `Worn-<numeric_version>.apk` and the merged
    release branch (`git branch -d chore/version-<numeric_version>`), then print the release URL,
    the new Android `versionCode`/`versionName`, and the iOS
    `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION`.

## Important

- Abort immediately if any step fails.
- Never commit or push directly to `main` — it is protected. The version bump always goes through
  the release branch and its PR.
- Ask the user for confirmation before pushing the tag and creating the release.
- Never skip detekt, the tests, or the APK signature verification.
- Never print, log, or commit any value from `local.properties`, and never commit a real `TEAM_ID`.
  Refer to the signing constants by name only.
