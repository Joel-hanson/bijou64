#!/usr/bin/env sh
set -e
cd "$(dirname "$0")"

if [ ! -d native/bijou ]; then
  git submodule update --init --depth 1 native/bijou
else
  if [ -f .gitmodules ]; then
    git submodule update --init --depth 1 native/bijou
  fi

  if [ -d native/bijou/.git ]; then
    git -C native/bijou fetch --depth 1 origin main
    git -C native/bijou checkout main
    git -C native/bijou reset --hard origin/main
  fi
fi

cd native
cargo build --release
