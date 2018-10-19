#!/usr/bin/env node

const dataUriToBuffer = require('data-uri-to-buffer');
const {writeFileSync} = require('fs')
const puppeteer = require('puppeteer');

function next(step) {
  console.log(step + '...');
}

async function waitasec(page) {
  next('pausing');
  await page.waitFor(1000);
}

async function render(diagramYaml) {
  console.log('launching browser...');
  const browser = await puppeteer.launch({headless: true});
  const page = await browser.newPage();

  async function asec() {
    next('pausing');
    await page.waitFor(1000);
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
    await sleep(200);
      changes = true;
    await sleep(200);
      structurizrExpressToDiagram();
    })();
  }, diagramYaml);

  await asec();
  await page.evaluate(() => Structurizr.diagram.exportCurrentView(1, true, false, false, false));
  await asec();

  const pages = await browser.pages()
  const exportPage = pages[2];
  const exportPageTitle = await exportPage.title()
  console.log('export page opened with title', exportPageTitle);

  await waitasec(page);

  next('getting image');
  const image = await exportPage.$('#exportedContent > img');

  await waitasec(page);

  console.log('getting image source...');
  const imageSourceHandle = await image.getProperty('src');
  const imageSource = await imageSourceHandle.jsonValue();
  const imageBuffer = dataUriToBuffer(imageSource);

  console.log('closing browser...')
await browser.close();

  return imageBuffer;
}

async function main() {
  const theYaml = `---
elements:
  -
    type: 'Software System'
    name: 'Fuck Off'
    position: '300,300'
type: 'System Context'
scope: 'Fuck Off'
size: A6_Landscape
`
  const ib = await render(theYaml);
  writeFileSync('/tmp/diagram.png', ib);
}

main()
