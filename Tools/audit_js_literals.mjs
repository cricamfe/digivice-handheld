// Prints the string and template literals of a JavaScript file as a JSON array, read from stdin.
// Tools/audit_release.py runs it so a quote inside a comment or a regex is not taken as the start of a string.
// Usage: node Tools/audit_js_literals.mjs < file.js
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(fileURLToPath(new URL("../Frontend/package.json", import.meta.url)));
const { tokenizer } = require("acorn");

const source = readFileSync(0, "utf8");
const literals = [];
for (const token of tokenizer(source, { ecmaVersion: "latest", sourceType: "module", allowHashBang: true })) {
  if (token.type.label === "string" || token.type.label === "template") literals.push(String(token.value));
}
process.stdout.write(JSON.stringify(literals));
