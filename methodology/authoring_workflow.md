# 5. The Authoring Workflow « The FC4 Methodology

Please note: the current workflow is _temporary._ We know it’s highly manual and that that’s not efficient or sustainable. But it’s sufficient for now, for the current pilot/prototyping phase of this methodology.

----

Once a basic YAML file has been created with some initial contents, the basic authoring workflow is:

1. Create a new git branch in your local instance of [the diagram repository](repository.md)
1. In your text editor: either create a new diagram source file or open an existing diagram source file
1. In a terminal, in your FC4C working dir:
   1. Open a Clojure REPL with `clj`
   1. Evaluate `(use 'fc4c.repl')` then `(wcb)`
   1. This starts a background process that will watch your clipboard for diagram source YAML and process (clean up) that YAML when it sees that it’s been changed.
1. In your text editor, add/revise elements and relationships, then select-all and cut the diagram source from your editor into your system clipboard.
   1. This will cause FC4C to process the contents of your clipboard.
1. Switch to [Structurizr Express](https://structurizr.com/help/express) (SE) » paste the source into the YAML textarea » press tab to blur the textarea
   1. SE will either render the diagram, or display a red error indicator in its toolbar
   2. If SE shows its red error indicator, click the indicator button to bring up a dialog listing the errors
1. Use SE to arrange the elements and edges as desired
1. Cut the diagram source from the SE YAML textarea into your system clipboard.
   1. This will cause FC4C to process the contents of your clipboard.
1. Paste the diagram source back into the SE YAML textarea so as to re-render the diagram, now that the elements have been “snapped” to a virtual grid.
1. Continue to cut and past the diagram source between your text editor and SE, using SE to preview and adjust the rendered diagram, while FC4C cleans up the diagram as you work.
1. When you’re ready to wrap up:
   1. Paste the diagram source into your text editor and save the YAML file.
   1. Paste the diagram source into SE, hit tab to re-render the diagram, then click the _Export to PNG_ button in the toolbar.
   1. The rendered diagram will open in a new browser tab.
   1. Save the rendered diagram to a PNG file next to the YAML file and with the same name as the YAML file, except for the filename extension of course.
1. Use your git UI of choice to commit your changes.
1. Push your local changes to the origin repository regularly.
1. When you’re ready to submit your changes for review, open a (Merge|Pull) Request to get your changes reviewed and then merged into master.

----

That’s pretty much it, right now.

Soon we’ll start looking into less manual, more automated workflows for authoring and rendering the diagrams.

----

Please continue to [Publishing](publishing.md) or go back to [the top page](README.md).
