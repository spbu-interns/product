#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

echo "Loading environment variables from server/.env..."
if [ -f "server/.env" ]; then
    export $(grep -v '^#' server/.env | xargs)
else
    echo "Warning: server/.env not found!"
fi

echo "Building project..."
./gradlew clean build -x test

echo "Running server..."
./gradlew :server:run
