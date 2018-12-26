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

## Identify the Repository

As described in [The Repository](../../methodology/repository.md) FC4 diagrams
should be part of a Git repository.

If your team or organization already has an FC4 repository, then you’ll need to
make sure you have a local clone of that repo, and that its `master` branch is
up to date:

```shell
# If you need to clone an existing repo:
$ git clone https://our-git-host.com/docs.git

# Or if you need to update master of a repo you already have a clone of:
$ cd repo-dir && git checkout master && git pull

# Or if you need to create a new repository:
$ mkdir docs && cd docs && git init
```

In either case, determine whether your FC4 repository (the set of files that
make up a corpus of FC4 resources/data/diagrams [_choose one_]) is located in
the root of the Git repo [_shit, it’s clear that overloading “repository” was a
mistake. Must choose new term. Maybe “corpus”?_] or in a subdirectory within the
repo.

```shell
$ mkdir fc4 && cd fc4
```

### Create a New Git Branch

The recommended FC4 workflow uses git “feature branches” to prepare, review, and
ship changes to the corpus.

```shell
$ git checkout -b spline-reticulator
```

### Initialize the Corpus

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

OK, you’re prepped. Next up: []
