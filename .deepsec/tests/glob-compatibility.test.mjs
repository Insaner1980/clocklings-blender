import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdirSync, mkdtempSync, readdirSync, realpathSync, rmSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { test } from "node:test";
import { fileURLToPath, pathToFileURL } from "node:url";

const require = createRequire(import.meta.url);
const deepsecEntry = require.resolve("deepsec");
const deepsecRequire = createRequire(deepsecEntry);
const agentRoot = realpathSync(resolve(dirname(deepsecEntry), "../../@earendil-works/pi-coding-agent"));
const agentRequire = createRequire(resolve(agentRoot, "package.json"));

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

test("agent package discovery returns absolute prompt paths and honors exclusions", async (t) => {
  const { DefaultPackageManager, SettingsManager } = await import(pathToFileURL(resolve(agentRoot, "dist/index.js")).href);
  const scratch = realpathSync(mkdtempSync(resolve(dirname(fileURLToPath(import.meta.url)), "agent-glob-")));
  t.after(() => rmSync(scratch, { recursive: true, force: true }));
  const packageRoot = resolve(scratch, "package");
  mkdirSync(resolve(packageRoot, "prompts/nested"), { recursive: true });
  writeFileSync(resolve(packageRoot, "package.json"), JSON.stringify({
    name: "local-prompts",
    version: "1.0.0",
    pi: { prompts: ["prompts/**/*.md", "prompts/.explicit.md", "!prompts/skip.md"] },
  }));
  for (const file of ["visible.md", "nested/visible.md", ".explicit.md", ".hidden.md", "skip.md", "other.txt"]) {
    writeFileSync(resolve(packageRoot, "prompts", file), "# Local prompt\n");
  }

  const manager = new DefaultPackageManager({
    cwd: scratch,
    agentDir: resolve(scratch, "agent"),
    settingsManager: SettingsManager.inMemory(),
  });
  const { prompts, installFailures } = await manager.resolveExtensionSources([packageRoot], { local: true });
  assert.deepEqual(installFailures, []);
  assert.deepEqual(
    prompts.filter(({ enabled }) => enabled).map(({ path }) => path).sort(),
    [".explicit.md", "nested/visible.md", "visible.md"].map((file) => resolve(packageRoot, "prompts", file)).sort(),
  );
});
