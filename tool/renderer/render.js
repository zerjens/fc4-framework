#!/usr/bin/env node

// NB: here’s how I’ve been testing this on my Mac:
// renderer $ cat ../test/data/structurizr/express/diagram_valid_cleaned.yaml | time ./render.js | open -a Preview.app -f

const dataUriToBuffer = require('data-uri-to-buffer');
const {existsSync, readFileSync} = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');

const log = function(msg) {
  // This program must log to stderr rather than stdout because it outputs its
  // result to stdout.
  process.stderr.write(msg);
  // Calling process.stderr.write twice might be slightly more efficient than
  // concatenating the newline to msg.
  process.stderr.write('\n');
}

log.next = function(step) {
  this(step + '...');
}

function chromiumPath() {
  // TODO: accept a path as a command-line argument
  const possiblePaths = [
    '/usr/bin/chromium-browser', // Alpine
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'];

  return possiblePaths.find(path => existsSync(path)) || null;
}

function puppeteerOpts(debugMode) {
  const args = [
    // We need this because we’re using the default user in our local Docker-based
    // test running environment, which is apparently root, and Chromium won’t
    // run as root unless this arg is passed.
    '--no-sandbox',

    // Recommended here: https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md#tips
    '--disable-dev-shm-usage'
  ];

  return {
    args: args,
    executablePath: chromiumPath(),
    headless: !debugMode
  };
}

function abit(ms) {
  log.next(`pausing ${ms}ms`);
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function loadStructurizrExpress(browser, url) {
  log.next(`loading Structurizr Express from ${url}`);
  const mainPage = await browser.newPage();
  await mainPage.goto(url, {'waitUntil' : 'networkidle2'});
  return mainPage;
}

async function setYamlAndUpdateDiagram(page, diagramYaml) {
  log.next('setting YAML and updating diagram');
  await page.evaluate(theYaml => {
    // This might shadow the outer context’s function of the same name, but
    // that’s OK because that function doesn’t cross the boundary to the browser
    // successfully. I guess maybe this isn’t a closure… honestly I’m not sure.
    function abit(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }

    (async () => {
      // helps with debugging, screenshots, etc
      document.getElementById('expressIntroductionModal').style.display = 'none';

      // Show the YAML tab. Not sure why but without this the diagram doesn’t render.
      document.querySelector('a[href="#yaml"]').dispatchEvent(
        new MouseEvent('click', {view: window, bubbles: true, cancelable: true})
      );

      const yamlTextArea = document.getElementById('yamlDefinition');
      yamlTextArea.value = theYaml;
      changes = true;

      await abit(200);
      structurizrExpressToDiagram();
    })();
  }, diagramYaml);
}

async function doExport(mainPage) {
  log.next('calling export function');

  // from https://github.com/GoogleChrome/puppeteer/issues/386#issuecomment-343059315
  const newPagePromise = new Promise(resolve => {
    mainPage.browser().once('targetcreated', target => resolve(target.page()));
  });

  await abit(200);
  await mainPage.evaluate(() => {
    Structurizr.diagram.exportCurrentView(1, true, false, false, false);
  });

  log.next('getting export page');
  const exportPage = await newPagePromise;

  const exportPageTitle = await exportPage.title();
  log(`export page opened with title: ${exportPageTitle}.`);

  return exportPage;
};

async function getImageBuffer(exportPage) {
  log.next('getting image');
  const image = await exportPage.waitForSelector('#exportedContent > img');

  log.next('getting image source');
  const imageSourceHandle = await image.getProperty('src');
  const imageSource = await imageSourceHandle.jsonValue();
  const imageBuffer = dataUriToBuffer(imageSource);

  return imageBuffer;
}

async function render(diagramYaml, browser, url, debugMode) {
  const mainPage =    await loadStructurizrExpress(browser, url);
                      await setYamlAndUpdateDiagram(mainPage, diagramYaml);
  const exportPage =  await doExport(mainPage);
  const imageBuffer = await getImageBuffer(exportPage)
  return imageBuffer;
}

async function closeBrowser(browser, debugMode) {
  if (debugMode) {
    log.next('DEBUG MODE: leaving browser open; script may be blocked until the browser quits.');
  } else {
    log.next('closing browser');
    await browser.close();
  }
}

async function main(url, debugMode) {
  // Read stdin first; if it fails or blocks, no sense in launching the browser
  const theYaml = readFileSync("/dev/stdin", "utf-8");

  log.next('launching browser');
  const opts = puppeteerOpts(debugMode);
  const browser = await puppeteer.launch(opts);

  const imageBuffer = await render(theYaml, browser, url, debugMode);
  closeBrowser(browser, debugMode);

  process.stdout.write(imageBuffer);
}

const url = 'https://structurizr.com/express';
const debugMode = false;
main(url, debugMode);
