#!/bin/bash

set -e
set -x

sctools_dir=$(echo "${BASH_SOURCE[0]}" \
                  | xargs readlink -f \
                  | xargs dirname \
                  | xargs dirname)

mkdir -p ${sctools_dir}/docs
cd ${sctools_dir}/docs

static_src=${sctools_dir}/resources/app/sctools/static/
static_dst=./sctools/static

mkdir -p $static_dst

rsync -av --delete \
      '--include=favicon.png' \
      '--include=styles' \
      '--include=styles/*' \
      '--include=js' \
      '--include=js/main.js' \
      '--include=js/main.js.map' \
      '--include=fontawesome' \
      '--include=fontawesome/webfonts' \
      '--include=fontawesome/webfonts/*' \
      '--include=fontawesome/css' \
      '--include=fontawesome/css/fa.purged.css' \
      '--exclude=*' \
      $static_src $static_dst

rsync -av ${sctools_dir}/resources/app/index-gh-pages.html index.html

build_ts=$(python -c "import sys, time; print(int(time.time()*1000))")
sed -i -e "s/SCTOOLS_BUILD_TS/$build_ts/g" index.html

# So that we have both docs/sctools/static (for local dev) and
# docs/static (for gh pages)
ln -sf sctools/static .
