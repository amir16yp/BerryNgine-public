#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

BRANCH="$(git branch --show-current 2>/dev/null || true)"
if [[ -z "${BRANCH}" ]]; then
  BRANCH="detached"
fi
BRANCH="${BRANCH//\//-}"

JAR="build/berryngine-${BRANCH}.jar"
CLASSES="build/classes-${BRANCH}"

mkdir -p "${CLASSES}" build
find src -name '*.java' -print > build/sources.txt
javac -d "${CLASSES}" @build/sources.txt

mkdir -p "${CLASSES}/berryngine/default_assets"
cp -a src/berryngine/default_assets/. "${CLASSES}/berryngine/default_assets/"

if [[ -e "${JAR}" ]]; then
  for _ in $(seq 1 10); do
    rm -f "${JAR}" 2>/dev/null || true
    if [[ ! -e "${JAR}" ]]; then
      break
    fi
    sleep 1
  done
  if [[ -e "${JAR}" ]]; then
    echo "Could not replace ${JAR} because it is in use." >&2
    exit 1
  fi
fi

# Use traditional jar flags for compatibility with older JDKs.
jar cfe "${JAR}" berryngine.TestScene -C "${CLASSES}" .

echo "Built ${JAR}"

case "${1:-}" in
  "")
    exit 0
    ;;
  test)
    java -jar "${JAR}"
    ;;
  benchmark)
    java -jar "${JAR}" --benchmark "${2:-}" "${3:-}"
    ;;
  *)
    echo "Usage: build.sh [test | benchmark [frames] [threads]]" >&2
    exit 2
    ;;
esac
