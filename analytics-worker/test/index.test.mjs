import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import test from "node:test";

import worker from "../src/index.mjs";

const migration = readFileSync(new URL("../migrations/0001_weekly_metrics.sql", import.meta.url), "utf-8");
const ORIGIN = "https://worker.invalid";

function setup(t) {
    const sql = new DatabaseSync(":memory:");
    t.after(() => sql.close());
    sql.exec(migration);

    const DB = {
        prepare(query) {
            const statement = sql.prepare(query);
            const bind = (...args) => ({
                run: () => statement.run(...args),
                all: () => ({ results: statement.all(...args) }),
            });
            return { ...bind(), bind };
        },
        batch(statements) {
            sql.exec("BEGIN");
            try {
                const results = statements.map(statement => statement.run());
                sql.exec("COMMIT");
                return results;
            } catch (error) {
                sql.exec("ROLLBACK");
                throw error;
            }
        },
    };
    const week = sql.prepare("SELECT date('now', 'weekday 0', '-13 days') AS week").get().week;
    const payload = { id: crypto.randomUUID(), week, counts: { "game.0.start": 1, "lesson.99.wrong": 4 } };
    const env = { DB, POSTHOG_HOST: "https://eu.i.posthog.com", POSTHOG_PROJECT_TOKEN: "test-token" };

    return { sql, env, payload };
}

async function send(env, payload, headers = { "content-type": "application/json" }) {
    const body = typeof payload === "string" ? payload : JSON.stringify(payload);
    const request = new Request(`${ORIGIN}/v1/metrics`, { method: "POST", headers, body });
    return (await worker.fetch(request, env)).status;
}

test("rejects unsupported input and caps streamed bytes before touching D1", async t => {
    const { sql, env, payload } = setup(t);
    const current = sql.prepare("SELECT date('now', 'weekday 0', '-6 days') AS week").get().week;
    const invalid = [
        { ...payload, profile: "someone@example.invalid" },
        { ...payload, id: "profile-id" },
        { ...payload, id: `${payload.id}\n` },
        { ...payload, week: "2026-02-30" },
        { ...payload, week: current },
        { ...payload, counts: {} },
        { ...payload, counts: { "game.00.read": 1 } },
        { ...payload, counts: { "game.100.finish": 1 } },
        { ...payload, counts: { "game.3.start\n": 1 } },
        { ...payload, counts: { "screen.settings": 0 } },
        { ...payload, counts: { "screen.settings": 1.5 } },
        { ...payload, counts: { "challenge.start": 1001 } },
        { ...payload, counts: { "lesson.2.correct": "07:00" } },
        { ...payload, counts: { "screen.raw-input": 1 } },
    ];
    for (const candidate of invalid) {
        assert.equal(await send(env, candidate), 400, JSON.stringify(candidate));
    }
    assert.equal(await send(env, { ...payload, week: "2025-01-06" }), 410);
    assert.equal(await send(env, payload, { "content-type": "text/plain" }), 415);
    assert.equal(await send(env, " ".repeat(48 * 1024) + JSON.stringify(payload)), 413);
    assert.equal(sql.prepare("SELECT count(*) AS size FROM batch_receipts").get().size, 0);
});

test("aggregates a batch once and holds only the receipt ID and week", async t => {
    const { sql, env, payload } = setup(t);
    assert.deepEqual(await Promise.all([send(env, payload), send(env, payload)]), [204, 204]);
    assert.equal(await send(env, { ...payload, counts: { "screen.parent": 1 } }), 204);
    const oldest = sql.prepare("SELECT date('now', 'weekday 0', '-34 days') AS week").get().week;
    assert.equal(await send(env, { ...payload, week: oldest }), 204);
    assert.equal(await send(env, { ...payload, id: crypto.randomUUID(), counts: { "game.0.start": 3 } }), 204);
    assert.deepEqual(sql.prepare("SELECT * FROM weekly_totals ORDER BY metric_key").all().map(row => ({ ...row })), [
        { week: payload.week, metric_key: "game.0.start", total: 4, contributors: 2 },
        { week: payload.week, metric_key: "lesson.99.wrong", total: 4, contributors: 1 },
    ]);
    assert.deepEqual({ ...sql.prepare("SELECT * FROM batch_receipts WHERE id = ?").get(payload.id) }, {
        id: payload.id,
        week: payload.week,
    });
});

test("a failed increment rolls the receipt back so a retry can contribute", async t => {
    const { sql, env, payload } = setup(t);
    sql.exec("CREATE TRIGGER fail_write BEFORE INSERT ON weekly_totals BEGIN SELECT RAISE(FAIL, 'test'); END");
    assert.equal(await send(env, payload), 503);
    assert.equal(sql.prepare("SELECT count(*) AS size FROM batch_receipts").get().size, 0);
    sql.exec("DROP TRIGGER fail_write");
    assert.equal(await send(env, payload), 204);
    assert.equal(sql.prepare("SELECT count(*) AS size FROM weekly_totals").get().size, 2);
});

test("exports only frozen, sufficiently contributed cells and can retry safely", async t => {
    const { sql, env, payload } = setup(t);
    const frozen = sql.prepare("SELECT date('now', 'weekday 0', '-41 days') AS week").get().week;
    const insert = sql.prepare("INSERT INTO weekly_totals VALUES (?, ?, ?, ?)");
    insert.run(frozen, "game.0.finish", 57, 10);
    insert.run(frozen, "game.1.finish", 41, 9);
    insert.run(frozen, "game.99.set_reveal", 22, 10);
    insert.run(frozen, "lesson.8.replay", 22, 10);
    insert.run(frozen, "challenge.timeout", 22, 10);
    insert.run(frozen, "screen.play_together", 22, 10);
    insert.run(payload.week, "game.2.finish", 80, 14);
    sql.prepare("INSERT INTO batch_receipts VALUES (?, ?)").run(payload.id, frozen);

    const requests = [];
    t.mock.method(globalThis, "fetch", async (url, options) => {
        requests.push({ url, options });
        assert.equal(options.redirect, "manual");
        if (requests.length === 1) return new Response(null, { status: 302 });
        if (requests.length === 2) return new Response(null, { status: 503 });
        if (requests.length === 3) return Response.json({ status: "Ok", quota_limited: ["events"] });
        return Response.json({ status: "Ok" });
    });
    const run = () => new Promise((resolve, reject) => {
        worker.scheduled({}, env, { waitUntil: task => task.then(resolve, reject) });
    });
    await assert.rejects(run(), /analytics export failed/u);
    await assert.rejects(run(), /analytics export failed/u);
    await assert.rejects(run(), /analytics export was not acknowledged/u);
    assert.equal(sql.prepare("SELECT count(*) AS n FROM weekly_totals").get().n, 6);
    assert.equal(sql.prepare("SELECT count(*) AS n FROM batch_receipts").get().n, 0);
    await run();
    await run();

    assert.equal(requests.length, 4);
    const delivered = requests[3];
    assert.equal(delivered.url, "https://eu.i.posthog.com/batch/");
    assert.deepEqual(delivered.options.headers, { "content-type": "application/json" });
    assert.ok(requests.every(request => request.options.body === delivered.options.body));
    assert.equal(delivered.options.body.includes(payload.id), false);
    const { batch } = JSON.parse(delivered.options.body);
    const [challenge, game, otherGame, lesson, screen] = batch;
    assert.equal(batch.length, 5);
    assert.equal(game.event, "clocklings_weekly_metric");
    assert.match(game.uuid, /^[0-9a-f]{8}-[0-9a-f]{4}-8[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/u);
    assert.equal(game.timestamp, new Date(frozen).toISOString());
    assert.deepEqual(game.properties, {
        distinct_id: "clocklings-blender-weekly-aggregate",
        $process_person_profile: false,
        $geoip_disable: true,
        app: "clocklings_blender",
        week: frozen,
        metric_key: "game.0.finish",
        scope: "game",
        exercise: "Taso 1 tasatunnit",
        outcome: "Suoritettu loppuun",
        total: 57,
        contributors: 10,
    });
    assert.equal(challenge.properties.outcome, "Aika loppui");
    assert.equal(otherGame.properties.exercise, "Taso 100");
    assert.equal(otherGame.properties.outcome, "Asettamistehtävän vastaus näytetty");
    assert.equal(lesson.properties.exercise, "Oppitunti 9 24 tuntia vuorokaudessa");
    assert.equal(lesson.properties.outcome, "Kerrattu");
    assert.equal(screen.properties.screen, "Pelaa yhdessä");
    assert.deepEqual({ ...sql.prepare("SELECT * FROM weekly_totals").get() }, {
        week: payload.week,
        metric_key: "game.2.finish",
        total: 80,
        contributors: 14,
    });
});
