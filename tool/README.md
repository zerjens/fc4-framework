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

1. Install Clojure as per [this guide](https://clojure.org/guides/getting_started)
   1. This project uses the new Clojure CLI (`clj`) and
      [tools.deps](https://clojure.org/guides/deps_and_cli), both of which are installed by
      [the new official Clojure installers](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
      released alongside Clojure 1.9. If you’ve been working with Clojure for awhile, you might
      not have these tools installed. Try `which clj` to check, and if that prints a blank line,
      try running the appropriate
      [installer](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools).
2. Clone [this repo](https://github.com/FundingCircle/fc4-framework)
3. `cd` into the repo and then `cd tool`
4. To install the dependencies, run: `clojure -Sdescribe'`

## Basic Usage

1. Have `clj` installed ([guide](https://clojure.org/guides/getting_started))
1. Run in your shell, from the root of the repo: `cd tool && ./wcb`

## Full Usage Workflow

As explained in [The Authoring Workflow](https://fundingcircle.github.io/fc4-framework/methodology/authoring_workflow.html) section of
[the FC4 Methodology](https://fundingcircle.github.io/fc4-framework/methodology/):

> 1. In your text editor: either create a new diagram source file or open an existing diagram source file
> 1. In a terminal, in your `fc4` working dir, run `cd tool && ./wcb`
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
rm -Rf src/test_utils
clojure -A:uberjar
git reset --hard HEAD # undo the deletion above BE CAREFUL NOT TO LOSE CHANGES
mv target/tool-1.0.0-SNAPSHOT-standalone.jar target/fc4.jar
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
