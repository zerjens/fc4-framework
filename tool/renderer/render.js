#!/usr/bin/env node

// NB: here’s how I’ve been testing this on my Mac:
// renderer $ cat ../test/data/structurizr/express/diagram_valid_cleaned.yaml | time ./render.js | open -a Preview.app -f

const dataUriToBuffer = require('data-uri-to-buffer');
const {existsSync, readFileSync} = require('fs');
const pageFunctions = require('./page-functions');
const path = require('path');
const puppeteer = require('puppeteer-core');

const STRUCTURIZR_EXPRESS_URL = 'https://structurizr.com/express';

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

function puppeteerOpts({ debugMode }) {
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
    ignoreHTTPSErrors: true,
    executablePath: chromiumPath(),
    headless: !debugMode
  };
}

async function loadStructurizrExpress(browser) {
  log.next(`loading Structurizr Express from ${STRUCTURIZR_EXPRESS_URL}`);
  const page = await browser.newPage();
  await page.goto(STRUCTURIZR_EXPRESS_URL);

  // I copied this step from
  // https://github.com/structurizr/puppeteer/blob/d8b625aef77b404de42199a1ff9a9f6730795913/export-express-diagram.js#L24
  await page.waitForXPath("//*[name()='svg']");

  return page;
}

// If successful, returns undefined. If unsuccessful or an error occurs, throws an exception. The
// value of the exception will be an Error object, as such it will have a broad error message in its
// `message` property; the more detailed errors will be in its property `errors`. The value of
// that property is described in the docs on `pageFunctions.getErrorMessages`.
async function setYamlAndUpdateDiagram(page, diagramYaml) {
  log.next('setting YAML and updating diagram');
  await page.evaluate(pageFunctions.renderExpressDefinition, diagramYaml);
  if (await page.evaluate(pageFunctions.hasErrorMessages)) {
    const err = new Error("Errors were found in the diagram definition");
    err.errors = await page.evaluate(pageFunctions.getErrorMessages);
    throw err;
  }
}

async function exportDiagram(page) {
  log.next('calling export function');
  const diagramImageBase64DataURI = await page.evaluate(pageFunctions.exportCurrentDiagramToPNG);

  // TODO: add some error handling: check that it actually is a data URI,
  // call the Structurizr Express predicate function that checks whether there
  // were any errors, etc.
  return dataUriToBuffer(diagramImageBase64DataURI);
}

async function render(diagramYaml, browser, args) {
  const page = await loadStructurizrExpress(browser);
  await setYamlAndUpdateDiagram(page, diagramYaml);
  const imageBuffer = await exportDiagram(page);
  return imageBuffer;
}

// On success: returns a Puppeteer browser object
// On failure: logs an error then returns null
async function launchBrowser(args) {
  try {
    const opts = puppeteerOpts(args);
    log.next('launching browser');
    return await puppeteer.launch(opts);
  } catch (err) {
    console.error(`Could not launch browser: ${err}\n${err.stack}`);
    return null;
  }
}

async function closeBrowser(browser, { debugMode }) {
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

function parseArgs() {
  const args = process.argv.join();
  return {
    debugMode: args.includes('--debug')
  }
}

function printErrorMessages(err, preppedYaml) {
  let humanOutput;
  const machineOutput = { message: err.message };

  if (err.errors) {
    // If the error has a property `errors` then it’s an Error object that’s been thrown within
    // `render`.
    humanOutput = `RENDERING FAILED: ${err.message}:\n`
    humanOutput += err.errors.map(errErr => `  💀 ${errErr.message}`).join('\n');
    machineOutput.errors = err.errors;
  } else {
    // general failure
    humanOutput = `RENDERING FAILED: ${err.stack}\nPrepped YAML:\n${preppedYaml}`
  }

  console.error(`🚨🚨🚨\n${humanOutput}\n🚨🚨🚨`);
  console.error(`🤖🤖🤖\n${JSON.stringify(machineOutput)}\n🤖🤖🤖`);
}

async function main() {
  const args = parseArgs();

  // Read stdin first; if it fails or blocks, no sense in launching the browser
  const rawYaml = readFileSync("/dev/stdin", "utf-8");
  const preppedYaml = prepYaml(rawYaml);

  // This is outside of the try block so that the binding will be visible to
  // both the try block below and the finally block, because if an error occurs
  // it’s really important to close the browser; if we don’t then the program
  // will hang and not exit, even though rendering failed.
  const browser = await launchBrowser(args);

  if (!browser) {
    // An error message will have been printed out by launchBrowser
    process.exitCode = 1;
    return;
  }

  try {
    const imageBuffer = await render(preppedYaml, browser, args);
    process.stdout.write(imageBuffer);
  } catch (err) {
    printErrorMessages(err, preppedYaml);
    process.exitCode = 1;
  } finally {
    closeBrowser(browser, args);
  }
}

main();
