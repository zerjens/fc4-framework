# The FC4 Framework

<!-- Using a table because I want the image to “float” to the right. I tried to do so with an inline
style on the img tag (or a wrapping figure tag) — but no dice. It looks like GitHub Pages strips
inline CSS. Understandable. So while this is kinda ugly, it more or less works. -->

<!-- Using inline CSS in the style attribute rather than a style tag or a css file because while
GitHub Pages (Jekyll) supports a style tag, github.com does not; this file is viewed and rendered in
both contexts. This will all get cleaned up when we decouple the two use cases (I plan to move the
GitHub Pages website into a subdirectory in the repo named “docs” after which the markup in its
files will only need to be compatible with a single Markdown processor (Jekyll) rather than two.) -->

<table style="border:none;">
<tr style="border:none;">
<td style="border:none; padding:0;">

<!-- Using HTML inside the table cells, rather than Markdown, because the Markdown processor used
for GitHub pages (via Jekyll) apparently won’t process Markdown nested inside HTML tags. -->

<p>FC4 is a <a href="https://www.writethedocs.org/guide/docs-as-code/"><i>Docs as Code</i></a>
   framework that enables software creators to author, publish, and maintain software architecture
   diagrams more effectively, efficiently, and collaboratively over time.</p>

It has two components:

<ul>
  <li><a href="methodology/">the methodology</a></li>
  <li><a href="tool/">the tool</a></li>
</ul>

</td>
<td width="350" style="border:none; padding:0;">

<img src="https://fundingcircle.github.io/fc4-framework/tool/doc/fc4-tool-container.png"
     width="350" height="248"
     alt="Example: a container diagram of fc4-tool."
     title="Example: a container diagram of fc4-tool.">

</td>
</tr>
</table>

It builds on [the C4 Model](https://c4model.com/) and [Structurizr Express](https://structurizr.com/express), both of which were created by and are maintained by [Simon Brown](http://simonbrown.je/).

It originated at and is maintained by [Funding Circle](https://engineering.fundingcircle.com/).

To get started, we recommend reading [the methodology](methodology/). If you have any questions or feedback please [create an issue](https://github.com/FundingCircle/fc4-framework/issues/new) and one of the maintainers will get back to you shortly.

## The Name

FC4 is not ([yet](https://en.wikipedia.org/wiki/Backronym)) an acronym or initialism; it doesn’t stand for anything — it’s “just” a name.

The name is a combination of “FC” and “C4” — the former is a reference to Funding Circle, the originating context of the framework; the latter to Simon Brown’s C4 model, the foundation of the framework.

## Contributing

* We cannot currently accept contributions of code or documentation, but we’re working on it.
* Any and all feedback — questions, suggestions, bug reports, experience reports, etc — would be greatly appreciated! Please [create an issue](https://github.com/FundingCircle/fc4-framework/issues/new) and one of the maintainers will get back to you shortly.

## Copyright & License

Copyright © 2018–2019 Funding Circle Ltd.

Distributed under [the BSD 3-Clause License](LICENSE).
