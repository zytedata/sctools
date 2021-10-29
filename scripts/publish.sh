#!/bin/bash

set -e
set -x

# ./copy_release_files.sh
#

default_repo=zytedata/sctools
git remote add upstream https://github.com/${default_repo}.git
git fetch upstream master

# create a new clear branch
git add -f docs

git reset --soft upstream/master

git config --global user.email "wxitb2017@gmail.com"
git config --global user.name "Lucy Wang"
git commit -a -m 'gh pages update on gh actions'

repo=${GITHUB_REPOSITORY:-$default_repo}
git remote set-url origin https://github.com/${repo}.git
git push -f origin HEAD:gh-pages
