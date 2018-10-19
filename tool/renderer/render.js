#!/usr/bin/env node

const dataUriToBuffer = require('data-uri-to-buffer');
const puppeteer = require('puppeteer');

function next(step) {
  process.stderr.write(step + '...' + '\n');
}

function result(is) {
  process.stderr.write(is + '.' + '\n');
}

async function render(diagramYaml) {
  next('launching browser');
  const browser = await puppeteer.launch({headless: true});
  const page = await browser.newPage();

  async function asec(s = 1) {
    next('pausing');
    await page.waitFor(s * 1000);
  }

  next('loading SE');
  await page.goto('https://structurizr.com/express', {'waitUntil' : 'networkidle0'});

  await asec();

  next('setting YAML and updating diagram');

  await page.evaluate(theYaml => {
    function sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }

    (async () => {
      // helps with debugging, screenshots, etc
      document.getElementById('expressIntroductionModal').style = "display: none;"

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
