import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { test } from "node:test";

const require = createRequire(import.meta.url);
const deepsecRequire = createRequire(require.resolve("deepsec"));
const agentRequire = createRequire(deepsecRequire.resolve("@anthropic-ai/claude-agent-sdk"));
const mcpRequire = createRequire(agentRequire.resolve("@modelcontextprotocol/sdk/server/index.js"));
const qs = createRequire(mcpRequire.resolve("express"))("qs");

for (const encodeValuesOnly of [false, true]) {
  test(`qs comma arrays tolerate non-callable toString with encodeValuesOnly=${encodeValuesOnly}`, () => {
    const options = { arrayFormat: "comma", encodeValuesOnly };
    const input = JSON.parse('{"values":[{"toString":42}]}');

    assert.doesNotThrow(() => qs.stringify(input, options));
    assert.equal(
      qs.stringify({ values: ["first", "second"] }, options),
      encodeValuesOnly ? "values=first,second" : "values=first%2Csecond",
    );
  });
}
