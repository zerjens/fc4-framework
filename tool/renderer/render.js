#!/usr/bin/env node

const {readFileSync} = require('fs');
const paper = require('paper-jsdom-canvas');
const {promisify} = require('util');

// These values are currently hard-coded for the content of examples/test1.json
const [canvasWidth, canvasHeight] = [550, 550];
const [offsetX, offsetY] = [200, 170];

function renderCanvas(jsonString, file_type) {
  const canvas = paper.createCanvas(canvasWidth, canvasHeight, file_type);
  paper.setup(canvas);
  paper.project.importJSON(jsonString);

  // I don’t know why, but without this the image is cut off.
  paper.view.translate(new paper.Point(offsetX, offsetY));

  paper.view.update();
  return canvas.toBuffer()
}

function main() {
  // TODO: read this from the command-line, defaulting to 'pdf'
  const fileType = 'pdf';

  const stdin = 0;
  const stdout = 1;

  const jsonString = readFileSync(stdin, { encoding: 'utf8' });
  const imageBuffer = renderCanvas(jsonString, fileType);

  process.stdout.write(imageBuffer);
}

main();
