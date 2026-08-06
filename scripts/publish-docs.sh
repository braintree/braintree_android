#!/usr/bin/env bash
# Publishes Dokka reference docs for a single version to the gh-pages branch,
# mirroring how braintree_ios publishes Jazzy docs: one folder per version plus
# a `current` symlink to the latest, no version-picker UI.
#
# This script only commits locally to the gh-pages branch checked out in a
# worktree next to the repo. It intentionally does not push - the caller
# (e.g. the release workflow) is responsible for pushing when ready.
set -euo pipefail

VERSION="${1:?Usage: publish-docs.sh <version>}"
BRANCH="gh-pages"

REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
DOCS_SOURCE="${REPO_ROOT}/build/dokkaDocs"
WORKTREE_DIR="$(dirname "${REPO_ROOT}")/$(basename "${REPO_ROOT}")-${BRANCH}"

if [ ! -d "${DOCS_SOURCE}" ]; then
  echo "No generated docs found at ${DOCS_SOURCE}. Run ./gradlew dokkaHtmlMultiModule first." >&2
  exit 1
fi

if ! git -C "${REPO_ROOT}" worktree list --porcelain | grep -qx "worktree ${WORKTREE_DIR}"; then
  if git -C "${REPO_ROOT}" show-ref --verify --quiet "refs/heads/${BRANCH}"; then
    git -C "${REPO_ROOT}" worktree add "${WORKTREE_DIR}" "${BRANCH}"
  else
    git -C "${REPO_ROOT}" worktree add --orphan -b "${BRANCH}" "${WORKTREE_DIR}"
    git -C "${WORKTREE_DIR}" commit --allow-empty -m "Initialize gh-pages branch" --quiet
  fi
fi

rm -rf "${WORKTREE_DIR:?}/${VERSION}"
mkdir -p "${WORKTREE_DIR}/${VERSION}"
cp -R "${DOCS_SOURCE}/." "${WORKTREE_DIR}/${VERSION}/"

ln -sfn "${VERSION}" "${WORKTREE_DIR}/current"

# GitHub Pages runs Jekyll on branch deploys by default, which mangles Dokka's
# raw HTML/underscore-prefixed output (e.g. _images) - opt out.
touch "${WORKTREE_DIR}/.nojekyll"

# A bare directory-of-folders has nothing to serve at the branch root, so
# redirect / to the latest version.
cat > "${WORKTREE_DIR}/index.html" <<'EOF'
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="refresh" content="0; url=current/index.html">
    <link rel="canonical" href="current/index.html">
  </head>
  <body>
    Redirecting to <a href="current/index.html">latest reference docs</a>...
  </body>
</html>
EOF

git -C "${WORKTREE_DIR}" add -A
if git -C "${WORKTREE_DIR}" diff --cached --quiet; then
  echo "Nothing new to publish for ${VERSION}."
else
  git -C "${WORKTREE_DIR}" commit -m "Publish docs for ${VERSION}" --quiet
fi

echo "Docs for ${VERSION} committed locally to branch '${BRANCH}' in worktree: ${WORKTREE_DIR}"
echo "Review with: git -C \"${WORKTREE_DIR}\" log --stat -1"
echo "This script does not push. Push manually when ready: git -C \"${WORKTREE_DIR}\" push origin ${BRANCH}"
