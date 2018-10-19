#!/usr/bin/env node

const dataUriToBuffer = require('data-uri-to-buffer');
const {writeFileSync} = require('fs')
const puppeteer = require('puppeteer');

async function render(diagramYaml) {
  const browser = await puppeteer.launch();
  console.log('browser launched');

  const page = await browser.newPage();

  await page.goto('https://structurizr.com/express', {'waitUntil' : 'networkidle0'});
  console.log('SE loaded');

  await page.evaluate(()=> {
      Structurizr.diagram.exportCurrentView(1, true, false, false, false);
  });

  console.log('export function called');

  console.log('waiting 1 second...');
  await page.waitFor(1000);

  const pages = await browser.pages()
  const exportPage = pages[2];
  const exportPageTitle = await exportPage.title()
  console.log('export page opened with title', exportPageTitle);

  console.log('getting image...');
  const image = await exportPage.$('#exportedContent > img');
  console.log('got image.');

  console.log('getting image source...');
  const imageSourceHandle = await image.evaluate('src');
  const imageSource = await imageSourceHandle.jsonValue();
  console.log('image source:', imageSource);

  const imageBuffer = dataUriToBuffer(imageSource);

  console.log('got image source?', imageBuffer)

  console.log('closing browser')
  await browser.close();

  return imageBuffer;
}

const ib = render(null);
writeFileSync('/tmp/diagram.png', ib);
