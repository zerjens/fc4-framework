# FC4 Framework » Getting Started » Prep (Starting From Scratch)

This page is for [documentarians](http://www.writethedocs.org/documentarians/)
who are completely new to FC4 and will be starting completely from scratch —
meaning, in particular, that you do _not_ need to work with an existing FC4
[corpus](../../concepts.md#corpus) that may already exist within your team or
organization.

(If your team/org _does_ already have an FC4 corpus that you want to work on,
please switch to [Prep (Existing Corpus)](1b-prep-existing-corpus.md).)

## Choose a Subject

Decide which system you want to document.

For the purposes of this guide, we’ll document a system named _Spline
Reticulator_.

<aside>
To get started, you mainly need to be clear on the name of the system. If other
aspects of the system are a little fuzzy — its boundaries, for example — don’t
worry about that; those tend to come into focus through the work of documenting
the subject.
</aside>

## Create the Git Repository

As described in [The Repository](../../methodology/repository.md) FC4 diagrams
should be part of a [Git](https://git-scm.com) repository. So let’s create one:

```shell
$ mkdir docs && cd docs && git init
```

### Create a New Git Branch

The [FC4 Authoring Workflow](../../methodology/authoring_workflow.md) uses Git
“feature branches” to prepare, review, and ship changes to the corpus.

```shell
$ git checkout -b spline-reticulator
```

## Create the Corpus Directory

We _could_ use the root directory of the repository as our FC4 corpus, but that
precludes other kinds of documentation being hosted in this repo. So let’s
instead create a root-level directory to serve as the corpus:

```shell
$ mkdir fc4 && cd fc4
```

<aside>
Why do we name this directory `fc4` rather than, say, `diagrams`? Because while
the primary value proposition of the FC4 framework is indeed to publish and
maintain software architecture diagrams, in order to do so we must create a
dataset that defines a static model of all of our software systems. That model
documents, in a form that’s readable by both humans and machines, all of our
systems, their containers, and their relationships. This data is crucial for
programmatically generating software architecture diagrams, but it also has many
other potential uses. [_violates YAGNI?_]
</aside>

### Initialize the Corpus

We need to create the two root-level directories and the styles file:

```shell
$ mkdir model views && touch styles.yaml
```

### Download and Install fc4-tool

1. Download the archive for your platform from the latest release on [the
   releases page](https://github.com/FundingCircle/fc4-framework/releases)
1. Expand the archive
1. Optional but recommended: move the extracted files to somewhere on your $PATH
   1. e.g. `mv ~/Downloads/fc4/fc4* ~/bin/`

----

OK, you’re prepped!

Next up: [Model the System](2-modeling.md)
