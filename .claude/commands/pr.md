---
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git branch:*), Bash(git push:*), Bash(gh pr create:*)
description: Push and open a pull request
---

## Context

- Current branch: !`git branch --show-current`
- Git status: !`git status`
- Commits since main: !`git log main..HEAD --oneline`
- Diff from main: !`git diff main...HEAD --stat`

## Your task

Based on the above context, create a pull request for the current branch:

1. Push the current branch to origin with `git push -u origin <branch>`
2. Create a PR using `gh pr create` targeting `main`. Fill in the PR body following the project's PR template (`.github/pull_request_template.md`):
   - **Summary**: concise description of what changed and why, based on the commits and diff
   - **Changes**: bullet list of key changes
   - **Test plan**: relevant verification steps for these changes
   - **Checklist**: include all items, check off those that apply
3. If the argument `$ARGUMENTS` contains `--draft`, add the `--draft` flag to `gh pr create`

You MUST do all of the above in a single message. Do not use any other tools or do anything else. Do not send any other text or messages besides these tool calls.
