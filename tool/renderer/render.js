#!/usr/bin/env node

const {readFile, writeFile} = require('fs');
const paper = require('paper-jsdom-canvas');
const {promisify} = require('util');

const readFileAsync = promisify(readFile);
const writeFileAsync = promisify(writeFile);

function renderCanvas(json_string) {
  const canvas = paper.createCanvas(500, 400, 'pdf');
  paper.setup(canvas);
  paper.project.importJSON(json_string);
  paper.view.update();
  return canvas.toBuffer()
}

async function main() {
  const stdin = 0;
  const stdout = 1;

  const json_string = await readFileAsync(stdin, { encoding: 'utf8' });
  const pdf_buffer = renderCanvas(json_string);

  await writeFileAsync(stdout, pdf_buffer);
}

main();
