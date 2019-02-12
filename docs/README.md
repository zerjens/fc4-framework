# FC4 Website

This directory (`docs`) hosts [the FC4 website][website]. The site is published via
[GitHub Pages][github-pages] and as such is a [Jekyll][jekyll-guide] site.

(Despite the name of this directory, it doesn’t necessarily host _all_ the docs for the framework;
it has this name because it’s [the only subdirectory name][ghp-config-source] that GitHub Pages
supports when one wants to publish _part_ of a [GitHub][github] repository via GitHub Pages.)

## Working On The Site

To make changes to the site we use a common GitHub workflow:

1. Create a feature branch in your local repo
1. Make some changes to your local repo, **testing them frequently**
   1. Changes to the website should be tested locally using [Jekyll][jekyll-guide]
1. Commit the changes to the local repo
1. Push the branch to the GitHub repo
1. Open a Pull Request (PR)
1. Request review of the PR, or wait for it to be reviewed
   1. Most [FC4 committers][fc4-contributors] will be notified automatically that you’ve opened the
      PR
1. Once the PR is approved, you should:
   1. Merge it to master (with the button on the PR page)
   1. Delete the feature branch from the GitHub repo

Most of these steps are described in more detail in GitHub’s [Hello World Guide][hello-world-guide].

[fc4-contributors]: https://github.com/FundingCircle/fc4-framework/graphs/contributors
[ghp-config-source]: https://help.github.com/articles/configuring-a-publishing-source-for-github-pages/
[github]: https://github.com/home
[github-pages]: https://pages.github.com
[hello-world-guide]: https://guides.github.com/activities/hello-world/
[jekyll-guide]: https://help.github.com/articles/setting-up-your-github-pages-site-locally-with-jekyll/
[website]: https://fundingcircle.github.io/fc4-framework/
