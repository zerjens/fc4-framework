# FC4 Renderer

This is a PoC of using Paper.js in Node.js to render FC4 diagrams to PDF and/or PNG files.

## Setup

### On MacOS

```shell
# FIRST! Install Homebrew from https://brew.sh/

brew install node cairo pango \
  && npm install
```

## Usage

```shell
cat examples/test1.json | ./render.js > examples/test1_canvas.pdf
```
