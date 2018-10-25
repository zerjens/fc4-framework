#!/usr/bin/env node

const dataUriToBuffer = require('data-uri-to-buffer');
const {existsSync} = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');

const log = {
  // This program must log to stderr rather than stdout because it outputs its
  // result to stdout.
  stderr(msg) {
    // Calling process.stderr.write twice might be slightly more efficient than
    // concatenating the newline to msg.
    process.stderr.write(msg);
    process.stderr.write('\n');
  },

  next(step) {
    this.stderr(step + '...');
  },

  result(is) {
    this.stderr(is + '.');
  }
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

  // If we’re running on Alpine Linux and Chromium has been installed via apk
  // (the Alpine package manager) then we want to use that install of Chromium
  // rather than the Chromium that Puppeteer downloads itself by default.
  // See <project-root>/.circleci/images/tool/Dockerfile
  // and https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md#running-on-alpine
  const ciChromiumPath = '/usr/bin/chromium-browser';
  if (existsSync(ciChromiumPath)) {
    opts.executablePath = ciChromiumPath;
  }

  return opts;
}

function abit(ms) {
  log.next(`pausing ${ms}ms`);
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function render(diagramYaml, browser, url) {
  const page = await browser.newPage();
  page.setOfflineMode(true);

  log.next(`loading Structurizr Express from ${url}`);
  await page.goto(url, {'waitUntil' : 'domcontentloaded'});

  log.next('setting YAML and updating diagram');
  await page.evaluate(theYaml => {
    function abit(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }

    (async () => {
      // helps with debugging, screenshots, etc
      document.getElementById('expressIntroductionModal').style = 'display: none;';

      // Show the YAML tab. Not sure why but without this the diagram doesn’t render.
      document.querySelector('a[href="#yaml"]').click();

      await abit(200);
      const yamlTextArea = document.getElementById('yamlDefinition');
      yamlTextArea.value = theYaml;
      changes = true;

      structurizrExpressToDiagram();
    })();
  }, diagramYaml);

  log.next('calling export function');
  await abit(200);
  await page.evaluate(() => Structurizr.diagram.exportCurrentView(1, true, false, false, false));

  log.next('getting export page')
  await abit(250);
  const pages = await browser.pages();
  const exportPage = pages[2];
  exportPage.setOfflineMode(true);
  const exportPageTitle = await exportPage.title();
  log.result('export page opened with title: ' + exportPageTitle);

  log.next('getting image');
  const image = await exportPage.$('#exportedContent > img');

  log.next('getting image source');
  const imageSourceHandle = await image.getProperty('src');
  const imageSource = await imageSourceHandle.jsonValue();
  const imageBuffer = dataUriToBuffer(imageSource);

  log.next('closing browser');
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
  // Read stdin first; if it fails or blocks, no sense in launching the browser
  const theYaml = await readEntireTextStream(process.stdin);

  log.next('launching browser');
  const opts = puppeteerOpts();
  const browser = await puppeteer.launch(opts);

  const url = `file:${path.join(__dirname, 'structurizr/Structurizr Express.html')}`

  const imageBuffer = await render(theYaml, browser, url);
  process.stdout.write(imageBuffer);
}

main();
