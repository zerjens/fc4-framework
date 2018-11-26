// These functions are in this file so they won’t be compiled by pkg when this
// sub-tool (the renderer) is packaged up with a Node runtime into a native
// executable, because if they are compiled then they can’t be passed into the
// headless browser tab controlled by Puppeteer, because they need to be
// serialized to strings in order to make it over into the browser to be
// evaluated and executed in that context.
//
// For more information, see https://github.com/zeit/pkg/issues/204
// ...and especially these comments:
// * Problem:    https://github.com/zeit/pkg/issues/204#issuecomment-345624945
// * Workaround: https://github.com/zeit/pkg/issues/204#issuecomment-378929002

module.exports = {
  renderExpressDefinition: (diagramYaml) => {
    structurizr.scripting.renderExpressDefinition(diagramYaml);
  },

  exportCurrentDiagramToPNG: () => {
    return structurizr.scripting.exportCurrentDiagramToPNG({ crop: false });
  }
}
