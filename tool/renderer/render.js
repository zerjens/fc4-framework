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
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/usr/bin/chromium', // Debian
    '/usr/bin/chromium-browser']; // Alpine

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

async function loadStructurizrExpress(browser, url) {
  log.next(`loading Structurizr Express from ${url}`);
  const page = await browser.newPage();
  await page.goto(url);

  // I copied this step from
  // https://github.com/structurizr/puppeteer/blob/d8b625aef77b404de42199a1ff9a9f6730795913/export-express-diagram.js#L24
  await page.waitForXPath("//*[name()='svg']");

  return page;
}

async function setYamlAndUpdateDiagram(page, diagramYaml) {
  log.next('setting YAML and updating diagram');
  await page.evaluate(diagramYaml => {
    structurizr.scripting.renderExpressDefinition(diagramYaml);
  }, diagramYaml);
}

async function exportDiagram(page) {
  const diagramImageBase64DataURI = await page.evaluate(() => {
    return structurizr.scripting.exportCurrentDiagramToPNG();
  });

  // TODO: add some error handling: check that it actually is a data URI,
  // call the Structurizr Express predicate function that checks whether there
  // were any errors, etc.
  return dataUriToBuffer(diagramImageBase64DataURI);
}

async function render(diagramYaml, browser, url, debugMode) {
  const page = await loadStructurizrExpress(browser, url);
  await setYamlAndUpdateDiagram(page, diagramYaml);
  const imageBuffer = await exportDiagram(page);
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

function prepYaml(yaml) {
  // Structurizr Express will only recognize the YAML as YAML and parse it if
  // it begins with the YAML document separator. If this isn’t present, it will
  // assume that the diagram definition string is JSON and will fail.
  const sepLoc = yaml.indexOf('---');
  return sepLoc >= 0 ? yaml.substring(sepLoc) : `---\n${yaml}`;
}

async function main(url, debugMode) {
  // Read stdin first; if it fails or blocks, no sense in launching the browser
  const rawYaml = readFileSync("/dev/stdin", "utf-8");
  const preppedYaml = prepYaml(rawYaml);

  log.next('launching browser');
  const opts = puppeteerOpts(debugMode);
  const browser = await puppeteer.launch(opts);

  const imageBuffer = await render(preppedYaml, browser, url, debugMode);
  closeBrowser(browser, debugMode);

  process.stdout.write(imageBuffer);
}

const url = 'https://structurizr.com/express?autoLayout=false';
const debugMode = false;
main(url, debugMode);
