#!/bin/bash

set -e
set -x

# ./copy_release_files.sh
#

owner=${1:-zytedata}

git remote set-url origin https://github.com/$owner/sctools.git
git fetch origin master
# create a new clear branch

git add -f docs

git reset --soft origin/master

git config --global user.email "wxitb2017@gmail.com"
git config --global user.name "Lucy Wang"
git commit -a -m 'gh pages update on gh actions'

git push -f origin HEAD:gh-pages
