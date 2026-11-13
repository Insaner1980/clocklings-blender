import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdirSync, mkdtempSync, readdir, readdirSync, realpathSync, rmSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, isAbsolute, resolve } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const deepsecEntry = require.resolve("deepsec");
const deepsecRequire = createRequire(deepsecEntry);
const agentRequire = createRequire(
  realpathSync(resolve(dirname(deepsecEntry), "../../@earendil-works/pi-coding-agent/package.json")),
);

for (const [consumer, consumerRequire] of [["DeepSec", deepsecRequire], ["agent", agentRequire]]) {
  test(`${consumer} minimatch supports file exclusions and case-insensitive model patterns`, () => {
    const { minimatch } = consumerRequire("minimatch");
    assert.equal(minimatch("app/src/main/Clock.kt", "**/*.kt"), true);
    assert.equal(minimatch("app/src/main/Clock.kt", "**/*.java"), false);
    assert.equal(minimatch("provider/Model-Large", "provider/model-*", { nocase: true }), true);
  });
}

test("shipped DeepSec CLI includes Kotlin files and honors Java exclusions", () => {
  const scratch = realpathSync(mkdtempSync(resolve(dirname(fileURLToPath(import.meta.url)), "cli-glob-")));
  try {
    mkdirSync(resolve(scratch, "source/src"), { recursive: true });
    for (const extension of ["kt", "java"]) {
      writeFileSync(resolve(scratch, `source/src/Clock.${extension}`), '@Path("/clock")\n');
    }
    writeFileSync(
      resolve(scratch, "deepsec.config.mjs"),
      'export default { projects: [{ id: "glob-test", root: "./source" }] };\n',
    );
    for (const excludeJava of [false, true]) {
      const dataRoot = resolve(scratch, excludeJava ? "excluded" : "baseline");
      mkdirSync(resolve(dataRoot, "glob-test"), { recursive: true });
      writeFileSync(
        resolve(dataRoot, "glob-test/config.json"),
        JSON.stringify({ ignorePaths: excludeJava ? ["**/*.java"] : [] }),
      );
      const env = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
        /^(PATH|SYSTEMROOT|WINDIR|COMSPEC|PATHEXT|TEMP|TMP|HOME|USERPROFILE)$/i.test(key),
      ));
      const result = spawnSync(process.execPath, [
        deepsecEntry, "scan", "--project-id", "glob-test", "--matchers", "jvm-jaxrs-resource",
      ], {
        cwd: scratch,
        env: { ...env, DEEPSEC_DATA_ROOT: dataRoot, NO_COLOR: "1" },
        encoding: "utf8",
        timeout: 30_000,
        windowsHide: true,
      });
      assert.ifError(result.error);
      assert.equal(result.status, 0, result.stderr || result.stdout);
      assert.deepEqual(
        readdirSync(resolve(dataRoot, "glob-test/files/src")).sort(),
        excludeJava ? ["Clock.kt.json"] : ["Clock.java.json", "Clock.kt.json"],
      );
    }
  } finally {
    rmSync(scratch, { recursive: true, force: true });
  }
});

test("agent glob supports its absolute package discovery options", () => {
  const { globSync } = agentRequire("glob");
  const files = globSync("tests/*.test.mjs", {
    cwd: resolve(dirname(fileURLToPath(import.meta.url)), ".."),
    absolute: true,
    dot: false,
    nodir: false,
  });
  assert.ok(files.length >= 3);
  assert.ok(files.every(isAbsolute));
  assert.ok(files.some((file) => file.endsWith("qs-comma.test.mjs")));
});

test("agent glob stops directory reads when cancelled during traversal", async () => {
  const { glob } = agentRequire("glob");
  const controller = new AbortController();
  const reason = new Error("test cancellation");
  let reads = 0;
  const traversal = glob("**/*", {
    cwd: resolve(dirname(fileURLToPath(import.meta.url)), ".."),
    signal: controller.signal,
    fs: {
      readdir(path, options, callback) {
        reads++;
        if (reads > 1) {
          // Bound a regression to the root's immediate children, never the installed dependency tree.
          queueMicrotask(() => callback(null, []));
          return;
        }
        readdir(path, options, (error, entries) => {
          controller.abort(reason);
          callback(error, entries);
        });
      },
    },
  });
  await assert.rejects(traversal, reason);
  const readsAtRejection = reads;
  await new Promise((done) => setImmediate(done));
  assert.equal(readsAtRejection, 1, "Cancellation must prevent traversal into child directories");
  assert.equal(reads, readsAtRejection, "Directory reads must stop after rejection");
});
