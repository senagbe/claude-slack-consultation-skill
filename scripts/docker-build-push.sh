#!/bin/bash
set -e

cd "$(dirname "$0")/.."

#### Configuration ####
IMAGE_NAME="${IMAGE_NAME:-slack-skill}"
IMAGE_TAG="${IMAGE_TAG:-$(git describe --tags --always --dirty 2>/dev/null || echo "latest")}"
REGISTRY="${REGISTRY:-}"          # e.g. gcr.io/my-project  or  docker.io/myuser
PLATFORM="linux/amd64"

#### Helpers ####
print_header() { echo; echo "----------------------------------------"; echo "  $1"; echo "----------------------------------------"; }
die() { echo "ERROR: $1" >&2; exit 1; }

#### Argument parsing ####
PUSH=false

usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Build (and optionally push) the Docker image for the Slack Consultation Skill server.

Options:
  --push              Push the image after building
  --tag TAG           Override the image tag     (default: git describe or "latest")
  --registry REG      Registry prefix, e.g. gcr.io/my-project
  -h, --help          Show this help

Environment variables (override defaults):
  IMAGE_NAME   Name of the image               (default: slack-skill)
  IMAGE_TAG    Tag to apply                    (default: git describe)
  REGISTRY     Registry prefix                 (default: empty)

Examples:
  # Local build only
  $0

  # Build and push to Docker Hub
  $0 --registry docker.io/myuser --push

  # Build and push to GCR with explicit tag
  $0 --registry gcr.io/my-project --tag v1.2.3 --push
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --push)           PUSH=true ;;
        --tag)            IMAGE_TAG="$2"; shift ;;
        --registry)       REGISTRY="$2"; shift ;;
        -h|--help)        usage ;;
        *) die "Unknown option: $1. Run with --help for usage." ;;
    esac
    shift
done

# Build the full image reference
if [[ -n "$REGISTRY" ]]; then
    FULL_IMAGE="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
else
    FULL_IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"
fi

#### Pre-flight checks ####
print_header "Pre-flight checks"

command -v docker &>/dev/null || die "Docker is not installed or not in PATH"
echo "Docker found: $(docker --version)"

[[ -f "Dockerfile" ]] || die "Dockerfile not found in $(pwd)"
echo "Dockerfile found"

echo
echo "Image : $FULL_IMAGE"
echo "Push  : $PUSH"

#### Build ####
print_header "Building image"

docker build --platform "$PLATFORM" --tag "$FULL_IMAGE" .

echo "Image built: $FULL_IMAGE"

#### Push ####
if [[ "$PUSH" == true ]]; then
    print_header "Pushing image"

    [[ -n "$REGISTRY" ]] || die "Cannot push without a --registry. Set REGISTRY or pass --registry."

    docker push "$FULL_IMAGE"
    echo "Image pushed: $FULL_IMAGE"

    # Also tag and push as :latest if this is a versioned tag
    if [[ "$IMAGE_TAG" != "latest" ]]; then
        LATEST_IMAGE="${REGISTRY}/${IMAGE_NAME}:latest"
        docker tag "$FULL_IMAGE" "$LATEST_IMAGE"
        docker push "$LATEST_IMAGE"
        echo "Latest tag pushed: $LATEST_IMAGE"
    fi
fi

#### Summary ####
print_header "Done"
echo "Image : $FULL_IMAGE"
echo
