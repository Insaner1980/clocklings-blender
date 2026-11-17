import assert from "node:assert/strict";
import { test } from "node:test";
import config from "../deepsec.config.ts";

test("DeepSec selects Kotlin code that opens network connections", () => {
  const matcher = config.plugins
    ?.flatMap((plugin) => plugin.matchers ?? [])
    .find(({ slug }) => slug === "clocklings-kotlin-network-connection");
  assert.ok(matcher, "The Kotlin network matcher must be registered in the project config");

  const path = "app/src/main/java/fi/clocklings/analytics/UsageUploadService.kt";
  assert.equal(matcher.match("val connection = network?.openConnection(endpoint)", path).length, 1);
  assert.equal(matcher.match("val connection = endpoint.openConnection()", path).length, 1);
  assert.deepEqual(matcher.match("val endpoint = uri.toURL()", path), []);
});
