#!/usr/bin/env bash

set -euo pipefail

usage() {
    echo "usage: $0 [--release [VERSION]] [--mbv VERSION] [--build]

Build a docker image.

Options:

    --release            Pull from the current tagged release in github
    --release VERSION    Pull VERSION from the tagged release in github
    --mbv VERSION        Specify a metabase version, default is latest. Required for --release
    --build              Build the jar and create a docker image from using that
    --no-docker          Don't run docker build"

    exit "${1:-1}"
}

# constants
EXEC_DIR="$(dirname "$0")"
OUTPUT_FILE=target/dist/materialize-driver.jar
NOW="$(date +%Y%m%d_%H%M%S)"

# options
RELEASE=n
VERSION=_
METABASE_VERSION=latest
BUILD=n
DOCKER=y

main() {
    cd "$EXEC_DIR/.." || exit 1
    parse_args "$@"
    if [[ $VERSION == _ ]]; then
        VERSION=$(current_version)
    fi
    ensure_jar "$VERSION"
    if [[ $DOCKER == y ]]; then
        runv docker build \
             -t materialize/metabase:"$NOW" \
             -t materialize/metabase:"$VERSION" \
             -t materialize/metabase:latest \
             --build-arg METABASE_VERSION="$METABASE_VERSION" \
             .
    fi
}

parse_args() {
    local arg
    while [[ $# -gt 0 ]]; do
        arg="$1" && shift
        case "$arg" in
            --release)
                RELEASE=y
                if [[ ${1:-_} =~ ^.+\..+\..+ ]]; then
                    VERSION="$1" && shift
                fi
                ;;
            --mbv)
                if [[ $# -eq 0 ]]; then
                    echo "--mbv requires a VERSION"
                    usage 1
                fi
                METABASE_VERSION="$1" && shift
                ;;
            --build)
                BUILD=y
                ;;
            -h|--help)
                usage 0
                ;;
            --no-docker)
                DOCKER=n
                ;;
            *)
                echo "Unexpected argument: $arg"
                usage 1
                ;;
        esac
    done
    if [[ $RELEASE == y && $BUILD == y ]]; then
        echo "error: --release and --build don't make sense together"
        exit 1
    fi
    if [[ $RELEASE == y && $METABASE_VERSION == latest ]]; then
        echo "error: --mbv is required for --release"
        exit 1
    fi
}

ensure_jar() {
    local version
    version="$1"
    if [[ $RELEASE == y ]]; then
        # first arg is the version
        mkdir -p target/dist
        runv curl -s \
             --output "$OUTPUT_FILE" \
             "https://github.com/MaterializeInc/metabase-materialize-driver/releases/download/$version/materialize-driver.jar"
        echo "downloaded $OUTPUT_FILE"
    elif [[ $BUILD == y ]]; then
        build_jar
    elif [[ ! -f $OUTPUT_FILE ]]; then
        echo "neither --build nor --release, and $OUTPUT_FILE not present"
        exit 1
    fi
}

build_jar() {
    runv lein clean
    runv lein uberjar
    mkdir -p "$(dirname $OUTPUT_FILE)"
    runv cp target/materialize-driver-"$(plugin_version)"-standalone.jar "$OUTPUT_FILE"
    echo "created $OUTPUT_FILE"
}

plugin_version() {
    grep -o 'version: .*' resources/metabase-plugin.yaml | sed 's/version: //'
}

current_version() {
    local version
    local current_tag
    local tag_no_v
    version="$(plugin_version)"
    current_tag="$(git tag --list --points-at HEAD)"
    # strip the leading v from a tag, since it is never in the yaml file
    tag_no_v="${current_tag#v}"
    if [[ $version =~ $tag_no_v ]]; then
        echo "$current_tag"
    else
        echo "$version"
    fi
}

runv() {
    echo "🚀$ $*"
    "$@"
}

main "$@"
