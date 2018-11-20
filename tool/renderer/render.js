#!/usr/bin/env node

// NB: here’s how I’ve been testing this on my Mac:
// renderer $ cat ../test/data/structurizr/express/diagram_valid_cleaned.yaml | time ./render.js | open -a Preview.app -f

const dataUriToBuffer = require('data-uri-to-buffer');
const {existsSync, readFileSync} = require('fs');
const pageFunctions = require('./page-functions');
const path = require('path');
const puppeteer = require('puppeteer-core');

// This program must log to stderr rather than stdout because it outputs its
// result to stdout.
const logStream = process.stderr;
const args = process.argv.join();
const verboseMode = args.includes('--verbose');
const quietMode = args.includes('--quiet');
const debugMode = args.includes('--debug');

// top-level const so we don’t have to thread it through everything.
const log = step => {
  if (!quietMode) {
    logStream.write(verboseMode ? `${step}...\n` : '.');
  }
};

log.finish = () => !quietMode && !verboseMode ? logStream.write('\n') : null;

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
  log(`loading Structurizr Express from ${url}`);
  const page = await browser.newPage();
  await page.goto(url);

  // I copied this step from
  // https://github.com/structurizr/puppeteer/blob/d8b625aef77b404de42199a1ff9a9f6730795913/export-express-diagram.js#L24
  await page.waitForXPath("//*[name()='svg']");

  return page;
}

async function setYamlAndUpdateDiagram(page, diagramYaml) {
  log('setting YAML and updating diagram');
  await page.evaluate(pageFunctions.renderExpressDefinition, diagramYaml);
}

async function exportDiagram(page) {
  log('calling export function');
  const diagramImageBase64DataURI = await page.evaluate(pageFunctions.exportCurrentDiagramToPNG);

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
    log('DEBUG MODE: leaving browser open; script may be blocked until the browser quits.');
  } else {
    log('closing browser');
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

  log('launching browser');
  const opts = puppeteerOpts(debugMode);
  const browser = await puppeteer.launch(opts);

  const imageBuffer = await render(preppedYaml, browser, url, debugMode);
  closeBrowser(browser, debugMode);

  process.stdout.write(imageBuffer);

  log.finish();
}

const url = 'https://structurizr.com/express?autoLayout=false';
main(url, debugMode);
