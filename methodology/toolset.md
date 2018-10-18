# 4. The Toolset « The FC4 Methodology

The current toolset for authoring and editing FC4 diagrams is:

| Tool                                                         | Uses                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Any text editor                                              | Creating the diagram files; authoring/editing the semantic contents of the diagrams: the elements, relationships, etc |
| [Structurizr Express](https://structurizr.com/help/express)  | Graphical authoring/editing the positioning of the elements; rendering the diagrams |
| [fc4-tool][fc4-tool] | Reformatting and normalizing the source of the diagrams so they’re diffable and easier to edit; snapping elements to a virtual grid |

## fc4-tool

[fc4-tool][fc4-tool] was created because when one uses Structurizr Express (SE) to position the elements of a diagram, SE regenerates the diagram source YAML in such a way that the YAML becomes noisy and the sorting can change. This makes the source harder to work with in a text editor and impossible to usefully diff from revision to revision — and without useful diffing it’s very difficult to do effective peer review.

So fc4-tool processes the YAML: cleans it up, applies a stable sort to all properties, removes empty properties, etc — so as to ensure that the changes applied in each revision are very small and specific and all extraneous changes are filtered out. This will hopefully enable effective peer review of revisions to the diagrams.

fc4-tool also “snaps” the elements and vertices in a diagram to a virtual grid.

The functionality of fc4-tool may expand over time to include additional features to assist with authoring and maintaining FC4 diagrams.

----

Please continue to [The Repository](repository.md) or go back to [the top page](README.md).

[fc4-tool]: https://github.com/FundingCircle/fc4-framework/tree/master/tool
