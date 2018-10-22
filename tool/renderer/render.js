#!/usr/bin/env node

const dataUriToBuffer = require('data-uri-to-buffer');
const path = require('path');
const puppeteer = require('puppeteer');

function next(step) {
  process.stderr.write(step + '...' + '\n');
}

function result(is) {
  process.stderr.write(is + '.' + '\n');
}

async function render(diagramYaml) {
  next('launching browser');

  // We need to disable web security to enable the main SE page to communicate
  // with the export page (pop-up window, tab, etc) without being blocked by
  // cross-origin restrictions.
  const args = ["--disable-web-security"];

  const browser = await puppeteer.launch({headless: true, args: args});
  const page = await browser.newPage();
  page.setOfflineMode(true);

  // This is here inside render because it has to be a closure that captures page.
  async function asec(s = 1) {
    next('pausing');
    await page.waitFor(s * 1000);
  }

  const url = `file:${path.join(__dirname, 'structurizr/Structurizr Express.html')}`

  next(`loading SE from ${url}`);
  await page.goto(url, {'waitUntil' : 'domcontentloaded'});

  await asec();

  next('setting YAML and updating diagram');

  await page.evaluate(theYaml => {
    function sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }

    (async () => {
      // helps with debugging, screenshots, etc
      document.getElementById('expressIntroductionModal').style = 'display: none;';

      await sleep(200);
      document.querySelector('a[href="#yaml"]').click();

      await sleep(200);
      const yamlTextArea = document.getElementById('yamlDefinition');

      await sleep(200);
      yamlTextArea.value = theYaml;

      await sleep(1000);
      changes = true;

      await sleep(1000);
      structurizrExpressToDiagram();

      await sleep(1000);
    })();
  }, diagramYaml);

  next('calling export function');
  await asec(4);
  await page.evaluate(() => Structurizr.diagram.exportCurrentView(1, true, false, false, false));
  await asec();

  const pages = await browser.pages();
  const exportPage = pages[2];
  exportPage.setOfflineMode(true);
  const exportPageTitle = await exportPage.title();
  result('export page opened with title: ' + exportPageTitle);

  await asec();

  next('getting image');
  const image = await exportPage.$('#exportedContent > img');

  await asec();

  next('getting image source');
  const imageSourceHandle = await image.getProperty('src');
  const imageSource = await imageSourceHandle.jsonValue();
  const imageBuffer = dataUriToBuffer(imageSource);

  next('closing browser');
  await browser.close();

  return imageBuffer;
}

async function readEntireTextStream(stream) {
  let str = '';

  stream.setEncoding('utf8');

  for await (const chunk of stream) {
    str += chunk;
  }

  return str;
}

async function main() {
  const theYaml = await readEntireTextStream(process.stdin);
  const imageBuffer = await render(theYaml);
  process.stdout.write(imageBuffer);
}

main();
