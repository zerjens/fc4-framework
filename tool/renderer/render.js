const fs = require('fs');
const paper = require('paper-jsdom-canvas');
const path = require('path');
const {promisify} = require('util');

const readFileAsync = promisify(fs.readFile);
const writeFileAsync = promisify(fs.writeFile);

async function renderCanvas(paper_json_input_path, output_path) {
  const canvas = paper.createCanvas(612, 792, 'pdf');
  paper.setup(canvas);

  const json_string = await readFileAsync(paper_json_input_path, { encoding: 'utf8' });

  paper.project.importJSON(json_string);
  paper.view.update();

  await writeFileAsync(path.resolve(output_path), canvas.toBuffer())

  console.log('Saved!');
}

renderCanvas('examples/test1.json', 'examples/test1_canvas.pdf');
