#!/bin/bash

set -e
set -x

rm -rf /tmp/sctools
mkdir -p /tmp/sctools
pushd /tmp/sctools

git init
git remote add origin https://github.com/lucywang000/sctools.git 
# create a new clear branch
git checkout --orphan branch-$(date +%s)

popd

cp -rpvf resources/app/static /tmp/sctools
cp -pvf resources/app/static/index-gh-pages.html /tmp/sctools/index.html

cd /tmp/sctools
git add -f .

git config --global user.email "wxitb2017@gmail.com"
git config --global user.name "Lucy Wang"
git commit -a -m 'update'

git push -f origin HEAD:gh-pages
