# fc4-tool

A tool for reorganizing, restructuring, and reformatting
[FC4](https://fundingcircle.github.io/fc4-framework/) diagrams.

[![CircleCI](https://circleci.com/gh/FundingCircle/fc4-framework.svg?style=shield)](https://circleci.com/gh/FundingCircle/fc4-framework)
[![codecov](https://codecov.io/gh/FundingCircle/fc4-framework/branch/master/graph/badge.svg)](https://codecov.io/gh/FundingCircle/fc4-framework)

## Purpose

As explained in
[The Toolset](https://fundingcircle.github.io/fc4-framework/methodology/toolset.html) section of
[the FC4 Methodology](https://fundingcircle.github.io/fc4-framework/methodology/):

> This tool was created because when one uses Structurizr Express (SE) to position the elements of a diagram, SE regenerates the diagram source YAML in such a way that the YAML becomes noisy and the sorting can change. This makes the source harder to work with in a text editor and impossible to usefully diff from revision to revision — and without useful diffing it’s very difficult to do effective peer review.
>
> So fc4-tool processes the YAML: cleans it up, applies a stable sort to all properties, removes empty properties, etc — so as to ensure that the changes applied in each revision are very small and specific and all extraneous changes are filtered out. This will hopefully enable effective peer review of revisions to the diagrams.
>
> fc4-tool also “snaps” the elements and vertices in a diagram to a virtual grid.

## Setup

1. Have a Java Runtime Environment (JRE) or Java Development Kit (JDK) installed
   1. On MacOS if you have [Homebrew](https://brew.sh/) you can run `brew cask install adoptopenjdk`
1. Download `fc4.jar` from the latest release on [the releases page](https://github.com/FundingCircle/fc4-framework/releases)


## Basic Usage Workflow

As explained in [The Authoring Workflow](https://fundingcircle.github.io/fc4-framework/methodology/authoring_workflow.html) section of
[the FC4 Methodology](https://fundingcircle.github.io/fc4-framework/methodology/):

> 1. In your text editor: either create a new diagram source file or open an existing diagram source file
> 1. In a terminal run `java -jar path/to/fc4.jar`
>    1. This starts the tool in a mode wherein it will watch your clipboard for diagram source YAML and process (clean up) that YAML when it sees that it’s been changed.
> 1. In your text editor, add/revise elements and relationships, then select-all and cut the diagram source from your editor into your system clipboard.
>    1. This will cause fc4-tool to process the contents of your clipboard.
> 1. Switch to [Structurizr Express](https://structurizr.com/help/express) (SE) » paste the source into the YAML textarea » press tab to blur the textarea
>    1. SE will either render the diagram, or display a red error indicator in its toolbar
>    2. If SE shows its red error indicator, click the indicator button to bring up a dialog listing the errors
> 1. Use SE to arrange the elements and edges as desired
> 1. Cut the diagram source from the SE YAML textarea into your system clipboard.
>    1. This will cause fc4-tool to process the contents of your clipboard.
> 1. Paste the diagram source back into the SE YAML textarea so as to re-render the diagram, now that the elements have been “snapped” to a virtual grid.
> 1. Continue to cut and past the diagram source between your text editor and SE, using SE to preview and adjust the rendered diagram, while fc4-tool cleans up the diagram as you work.

## Requirements and Prerequisites for Development and Testing

### Required

* Java 8/9/10
  * The tool cannot currently compile on Java 11 due to [an incompatibility](https://github.com/circleci/clj-yaml/issues/22) in a dependency of a dependency. Therefore you are recommended to use Java 8, 9, or 10 to develop and/or test the tool.

### Recommended

* [Docker](https://www.docker.com/)
  * For [running the tests](#running-the-tests)

## Running the Tests

1. Use CI
2. No, seriously, use CI!
3. Just kidding, I know sometimes you need to run the tests locally ;)

### With Docker

Run this in your shell:

```bash
docker run --rm `docker build -q .`
```

### Without Docker

If you’re old-school and prefer to run tests on bare metal:

1. Have `clojure` installed ([guide](https://clojure.org/guides/getting_started))
1. Run in your shell: `clojure -A:test:test/run`

## Starting a REPL for Dev/Test

You _could_ just run `clj` but you’re likely to want the test deps and dev utils to be accessible.
So you’ll probably want to run `clj -A:dev:test`

### Running the tests in a REPL

```
$ clj -A:dev:test
Clojure 1.9.0
user=> (require '[eftest.runner :refer [find-tests run-tests]])
user=> (run-tests (find-tests "test") {:fail-fast? true})
...
```

## Running the Linter

For linting, this project uses [cljfmt](https://github.com/weavejester/cljfmt),
via [cljfmt-runner](https://github.com/JamesLaverack/cljfmt-runner).

* To lint the entire project, run `clojure -A:lint`
* To lint the entire project **and automatically fix any problems found** run
  `clojure -A:lint:lint/fix`
  * This will change the files on disk but will not commit the changes nor stage
    them into the git index. This way you can review the changes that were
    applied and decide which to keep and which to discard.

We’ll soon be integrating a lint run into our CI builds so they’ll fail if any
source code is formatted incorrectly. Coming soon!

## Building an Uberjar

You’ll need to be using Java 9 or 10; 11 or higher cannot currently compile the project (see [issue #85](https://github.com/FundingCircle/fc4-framework/issues/85)).

```shell
# From <repo-root>/tool/ run:
bin/uberjar
```

Since you need to use Java 8/9/10 to compile the project but then Java 11 to validate the uberjar, you might find [jenv](http://www.jenv.be/) useful. (I discovered it via [Multiple JVM versions on macOS](https://pete-woods.com/2018/01/multiple-jvm-versions-on-macos/) by Pete Woods.)

## Contributors

* @99-not-out
* @arrdem
* @matthias-margush
* @sgerrand
* @timgilbert

Thank you all!

(If you notice that anyone is missing from this list, please open an issue or a PR — thank you!)
