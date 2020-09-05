#!/bin/bash

set -e
set -x

rm -rf /tmp/sctools
mkdir -p /tmp/sctools
pushd /tmp/sctools

git init
git remote add origin https://github.com/lucywang000/sctools.git 
git fetch origin master
# create a new clear branch
git checkout --orphan branch-$(date +%s)

popd

cp -rpvf resources/app/static /tmp/sctools
cp -pvf resources/app/static/index-gh-pages.html /tmp/sctools/index.html

cd /tmp/sctools

build_ts=$(python -c "import sys, time; print(int(time.time()*1000))")
sed -i -e "s/SCTOOLS_BUILD_TS/$build_ts/g" index.html

git add -f .
git reset --soft origin/master

git config --global user.email "wxitb2017@gmail.com"
git config --global user.name "Lucy Wang"
git commit -a -m 'gh pages update on gh actions'

git push -f origin HEAD:gh-pages
