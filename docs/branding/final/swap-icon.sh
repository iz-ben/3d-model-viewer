#!/usr/bin/env bash
# Usage: swap-icon.sh <ico|monkey>
set -e
choice="$1"
DST="/Users/ben/IdeaProjects/3d-model-viewer/src/main/resources/META-INF"
SRC="/tmp/logo/final"
case "$choice" in
  ico)    cp "$SRC/ico_light.svg"    "$DST/pluginIcon.svg";    cp "$SRC/ico_dark.svg"    "$DST/pluginIcon_dark.svg";;
  monkey) cp "$SRC/monkey_light.svg" "$DST/pluginIcon.svg";    cp "$SRC/monkey_dark.svg" "$DST/pluginIcon_dark.svg";;
  *) echo "usage: swap-icon.sh <ico|monkey>"; exit 1;;
esac
echo "Swapped plugin icon to: $choice"
