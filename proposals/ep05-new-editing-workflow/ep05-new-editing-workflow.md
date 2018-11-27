# Enhancement Proposal EP05: Unified Authoring Workflow

<table>
  <tr>
    <th>EP:</th>
    <td>5</td>
  </tr>
  <tr>
    <th>Title:</th>
    <td>Unified Authoring Workflow</td>
  </tr>
  <tr>
    <th>Author:</th>
    <td><a href="https://github.com/aviflax">Avi Flax</a></td>
  </tr>
  <tr>
    <th>Status:</th>
    <td>Draft</td>
  </tr>
  <tr>
    <th>Created:</th>
    <td>2018-11-26</td>
  </tr>
  <tr>
    <th>Note:</th>
    <td></td>
  </tr>
</table>


## Summary

The [current authoring workflow](https://fundingcircle.github.io/fc4-framework/methodology/authoring_workflow.html)
is slow, awkward, disjointed, inefficient, and hard to learn:

1. Run `fc4 wcb` to move diagram YAML back-and-forth between YAML file(s) open in your text editor
   and Structurizr Express, using the system clipboard, and clean the file(s) up while doing so.
2. Run `fc4 render path/to/yaml/file(s)` to render the diagram as PNG file(s).
3. Run `git commit` to commit the changes to the YAML and PNG files together.

Now that we’ve recently added automated rendering (see
[EP02](https://github.com/FundingCircle/fc4-framework/pull/74)), we were able to simplify the above
workflow by replacing the manual rendering in step 2 with much simpler automated rendering. In doing
so, implementing `render` as a standalone command that does _only_ rendering was a smart move, but
now that we have it, it’s opened the door to a new
[adjacent possible](https://medium.com/@SeloSlav/what-is-the-adjacent-possible-17680e4d1198): a new
authoring workflow with a single command that integrates the above steps _and_ makes usage of the
clipboard optional rather than required.


## New User Experience (Workflow)

### Single File Workflow

1. The documentarian runs the new `edit` command like so:
   1. `fc4 edit path/to/diagram.yaml`
1. fc4-tool immediately processes the file:
   1. Cleans up the YAML (overwriting the file)
   1. Renders the diagram to a PNG file
   1. Copies the cleaned-up YAML to the clipboard (overwriting its contents) in case the
      documentarian wants to paste it into Structurizr Express to edit the diagram graphically
1. fc4-tool stays running in a persistent mode, watching the file and the clipboard for changes
1. Whenever the watched file is changed, fc4-tool automatically processes it again, just as it did
   when first starting
1. Whenever the contents of the clipboard are changed (by the documentarian) and contain a
   Structurizr Express diagram in YAML, fc4-tool:
   1. Assumes that the documentarian has edited the diagram graphically using Structurizr Express
      and wishes to move the edits back to the files
   1. Cleans up the YAML in the clipboard
   1. Writes the cleaned-up YAML to the file (overwriting the file)
   1. Renders the diagram to a PNG file
   1. Copies the cleaned-up YAML back to the clipboard (overwriting its contents) in case the
      documentarian wants to paste it into Structurizr Express to edit the diagram graphically again
1. When the documentarian is done editing the diagram, they:
   1. Wait for the last rendering step to complete, if it hasn’t already
   1. Hit ctrl-c in their terminal to shut down fc4-tool
   1. Run `git commit` to commit their changes to the diagram

### Multi-File Workflow

This is essentially the same as the single-file workflow, except it does not involve the clipboard
and it does not proactively render or process any of the watched files when starting up.

1. The documentarian runs the new `edit` command like so:
   1. `fc4 edit path/to/diagrams/*.yaml`
1. fc4-tool starts up in a persistent mode, watching those files for changes
1. Whenever one of the watched files is changed, fc4-tool automatically:
   1. Cleans up the YAML in the file
   1. Renders the diagram to a PNG file
1. When the documentarian is done editing the diagrams, they:
   1. Wait for the last rendering step to complete, if it hasn’t already
   1. Hit ctrl-c in their terminal to shut down fc4-tool
   1. Run `git commit` to commit their changes to the diagrams


## Implementation Status

* I’ve got the new workflow mostly working in the branch
  [ep05-spike](https://github.com/FundingCircle/fc4-framework/tree/ep05-spike)
* I’ve demoed it for a few people and the response has been positive
* Remaining work:
  * More error-handling
  * More automated testing
  * Solicit and incorporate feedback from documentarians based on both this enhancement proposal and
    on the spike
