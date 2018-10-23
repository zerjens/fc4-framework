#!/usr/bin/env node

const dataUriToBuffer = require('data-uri-to-buffer');
const {existsSync} = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');

function next(step) {
  process.stderr.write(step + '...' + '\n');
}

function result(is) {
  process.stderr.write(is + '.' + '\n');
}

function puppeteerOpts() {
  const args = [
    // We need to disable web security to enable the main SE page to communicate
    // with the export page (pop-up window, tab, etc) without being blocked by
    // cross-origin restrictions.
    '--disable-web-security',

    // We need this because we’re using the default user in our local Docker-based
    // test running environment, which is apparently root, and Chromium won’t
    // run as root unless this arg is passed.
    '--no-sandbox',

    // Recommended here: https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md#tips
    '--disable-dev-shm-usage'
  ];

  const opts = {headless: true, args: args};

  // When we’re running in CI, there’s a very good chance that we’re going to
  // want to use the Chromium installed via the OS package manager.
  // See <project-root>/.circleci/images/tool/Dockerfile
  // and https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md#running-on-alpine
  const ciChromiumPath = '/usr/bin/chromium-browser';
  if (existsSync(ciChromiumPath)) {
    opts.executablePath = ciChromiumPath;
  }

  return opts;
}

async function render(diagramYaml) {
  next('launching browser');

  const opts = puppeteerOpts();
  const browser = await puppeteer.launch(opts);
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
