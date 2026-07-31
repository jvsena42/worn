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
   - `Config.xcconfig` is tracked in git and its `TEAM_ID` is intentionally empty — a locally
     filled `TEAM_ID` must never be committed. Verify by inspecting **added lines only**:
     `git diff | grep -E "^\+[^+]"` must show exactly the four version lines and nothing else.
     Do not grep the whole diff: `TEAM_ID=` sits a few lines above the version block, so it always
     appears as an unchanged context line and a naive grep aborts the release on every run.
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

8. **Generate changelog** (before tagging, so the user confirms the notes and the publish together):
   - Find the previous tag: `git describe --tags --abbrev=0 HEAD` — the new tag does not exist yet,
     so this returns the previous one. If no tag exists at all, use every commit.
   - List commits since it: `git log <previous_tag>..HEAD --oneline --no-merges`.
   - For a large range, group first: `git log <prev>..HEAD --no-merges --format="%s" | sed -E
     's/^([a-z]+)(\(.*\))?:.*/\1/' | sort | uniq -c | sort -rn`, then read the `feat:` and `fix:`
     subjects. Releases here can span ~90 commits, so summarize by theme rather than per commit.
   - Write the changelog as a bullet list of user-facing changes (group related commits, skip
     chore/CI/test/refactor commits — including this release's own version bump — and keep each
     bullet to one sentence in English). Do not trust commit subjects blindly: some past commits
     are mislabelled (two `fix: update changelog` commits only touched `.gitignore`).
   - End with a full-changelog link:
     `https://github.com/jvsena42/worn/compare/<previous_tag>...<version>`.

9. **Confirm, then create the git tag** on the merge commit now at the tip of `main`:
   - Show the user the changelog and state plainly that the next actions push a tag and publish a
     **public** release. Get explicit approval before continuing.
   - `git tag -a <version> -m "Release <version>"`.
   - Confirm it landed on the merge commit: `git rev-list -n1 <version>` should equal `main`'s tip.
   - `git push origin <version>` (tags push fine; only branch pushes to `main` are blocked).

10. **Create GitHub release**:
    - Write the approved changelog to a temp file and pass it with `--notes-file` — `--notes` is
      unwieldy for multi-line bodies.
    - `gh release create <version> Worn-<numeric_version>.apk --title "Worn <version>"
      --notes-file <file> --latest`.
    - Verify it published as intended:
      `gh release view <version> --json tagName,name,isDraft,isPrerelease,assets` and
      `gh api repos/jvsena42/worn/releases/latest -q '.tag_name'`. Note there is no `isLatest`
      JSON field — query the `releases/latest` endpoint instead.

11. **Clean up and summarize**: Delete the temporary `Worn-<numeric_version>.apk` and the merged
    release branch (`git branch -d chore/version-<numeric_version>`), then print the release URL,
    the new Android `versionCode`/`versionName`, and the iOS
    `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION`.

## Important

- Abort immediately if any step fails.
- Never commit or push directly to `main` — it is protected. The version bump always goes through
  the release branch and its PR.
- Ask the user for confirmation before pushing the tag and creating the release (step 9).
- Run `git commit`, `git push` and `gh pr create` as **separate** commands, never chained into one.
  Chained, a declined or interrupted call can leave the commit and push already done while the PR
  is missing, and the run then looks inconsistent. If a step seems to have half-run, re-check the
  real state (`git log`, `git ls-remote --heads origin <branch>`, `gh pr list`) before redoing
  anything — `git rev-parse @{u}` can fail on a branch that *was* pushed, when its remote-tracking
  ref is simply not fetched yet.
- Never skip detekt, the tests, or the APK signature verification.
- Never print, log, or commit any value from `local.properties`, and never commit a real `TEAM_ID`.
  Refer to the signing constants by name only.
