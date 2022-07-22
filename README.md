Analyze your jobs on Scrapy Cloud.

The tool is hosted on Github Pages: https://zytedata.github.io/sctools/

Table of Contents
=================

* [Screenshots](#screenshots)
   * [Display multiple jobs stats as a table](#display-multiple-jobs-stats-as-a-table)
   * [Sort jobs by some metric](#sort-jobs-by-some-metric)
   * [Filter jobs by spider args](#filter-jobs-by-spider-args)
   * [Customize which stats to show](#customize-which-stats-to-show)
   * [Select which scrapy stats to show](#select-which-scrapy-stats-to-show)
   * [Reorder the columns by drag and drop](#reorder-the-columns-by-drag-and-drop)
   * [Visualizing the Jobs stats](#visualizing-the-jobs-stats)
* [License](#license)

# Screenshots

## Display multiple jobs stats as a table

![Job Stats Table](screenshots/table.png)

## Sort jobs by some metric

![Sorting](screenshots/sort.png)

## Filter jobs by spider args

![Filtering](screenshots/filter.png)

## Customize which stats to show

![Customize](screenshots/customize.png)


## Select which scrapy stats to show

![Stats](screenshots/stats.png)

## Reorder the columns by drag and drop

Drag and drop to reorder the columns:

![Reorder](screenshots/reorder.gif)


## Visualizing the jobs stats

Number of items crawled over time:

![Crawled Items Chart](screenshots/chart.png)

# Development Guide

## Install pnpm

```
curl -fsSL https://get.pnpm.io/install.sh | sh -
```

## Install clojure

Follow this [document](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools).

## Launch the dev server

```
pnpm watch
```

It could take a while running for the first time.

In another terminal:

```
pnpm watch-css
```

Now visit http://127.0.0.1:3345 in a web browser.


# License

Copyright © 2021 Lucy Wang

Distributed under the the Apache License, Version 2.0.
