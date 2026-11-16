const DAY_MS = 86_400_000;
const WEEK_MS = 7 * DAY_MS;
const MAX_REQUEST_BYTES = 48 * 1024;
const MAX_COUNT = 1_000;
const MIN_CONTRIBUTORS = 10;
const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/u;
const METRIC_KEYS = [
    /^game\.(?:0|[1-9][0-9]?)\.(?:start|finish|read|set|read_first|set_first|read_hint|set_hint|read_reveal|set_reveal)$/u,
    /^lesson\.(?:0|[1-9][0-9]?)\.(?:start|finish|wrong|correct|replay)$/u,
    /^challenge\.(?:start|finish|correct|wrong|timeout)$/u,
    /^screen\.(?:home|lessons|levels|free_clock|play_together|wardrobe|personalize|settings|parent|challenge|result)$/u,
];

// Use the database clock for both admission and export, including at midnight UTC.
const CURRENT_WEEK_SQL = "date('now', 'weekday 0', '-6 days')";
const FIRST_ACCEPTED_WEEK_SQL = "date('now', 'weekday 0', '-34 days')";
const CLAIM_BATCH_SQL = `
    INSERT INTO batch_receipts (id, week)
    SELECT id, week FROM (SELECT ? AS id, ? AS week)
    WHERE week >= ${FIRST_ACCEPTED_WEEK_SQL} AND week < ${CURRENT_WEEK_SQL}
    ON CONFLICT (id) DO NOTHING`;
const ADD_TOTALS_SQL = `
    INSERT INTO weekly_totals (week, metric_key, total, contributors)
    SELECT ?, key, value, 1 FROM json_each(?) WHERE changes() = 1
    ON CONFLICT (week, metric_key) DO UPDATE SET
        total = total + excluded.total,
        contributors = contributors + 1`;

const POSTHOG_HOST = "https://eu.i.posthog.com";
const POSTHOG_EVENT = "clocklings_weekly_metric";
const POSTHOG_APP = "clocklings_blender";
const GAME_EXERCISES = [
    "Taso 1 tasatunnit",
    "Taso 2 puolitunnit",
    "Taso 3 vartit",
    "Taso 4 viisi minuuttia",
    "Taso 5 minuutit",
];
const LESSON_EXERCISES = [
    "Oppitunti 1 Kaksi viisaria",
    "Oppitunti 2 Yksi kierros",
    "Oppitunti 3 Tasatunnit",
    "Oppitunti 4 Puoli tuntia",
    "Oppitunti 5 Vartit",
    "Oppitunti 6 Yli ja vaille",
    "Oppitunti 7 Viiden minuutin välein",
    "Oppitunti 8 Tarkat minuutit",
    "Oppitunti 9 24 tuntia vuorokaudessa",
];
const OUTCOMES = {
    start: "Aloitettu",
    finish: "Suoritettu loppuun",
    read: "Kellon lukeminen",
    set: "Kellon asettaminen",
    read_first: "Lukutehtävä oikein ensimmäisellä yrityksellä",
    set_first: "Asettamistehtävä oikein ensimmäisellä yrityksellä",
    read_hint: "Lukutehtävä oikein vihjeen jälkeen",
    set_hint: "Asettamistehtävä oikein vihjeen jälkeen",
    read_reveal: "Lukutehtävän vastaus näytetty",
    set_reveal: "Asettamistehtävän vastaus näytetty",
    wrong: "Väärä vastaus",
    correct: "Oikea vastaus",
    replay: "Kerrattu",
    timeout: "Aika loppui",
};
const SCREENS = {
    home: "Etusivu",
    lessons: "Oppitunnit",
    levels: "Pelitasot",
    free_clock: "Tutki",
    play_together: "Pelaa yhdessä",
    wardrobe: "Asut",
    personalize: "Hahmon muokkaus",
    settings: "Asetukset",
    parent: "Vanhemman alue",
    challenge: "Minuuttihaaste",
    result: "Tulokset",
};

function empty(status) {
    return new Response(null, { status, headers: { "cache-control": "no-store" } });
}

async function readPayload(request) {
    const contentType = request.headers.get("content-type")?.split(";")[0].trim().toLowerCase();
    if (contentType !== "application/json") return empty(415);
    if (Number(request.headers.get("content-length")) > MAX_REQUEST_BYTES) return empty(413);
    if (!request.body) return empty(400);

    const reader = request.body.getReader();
    const bytes = new Uint8Array(MAX_REQUEST_BYTES);
    let length = 0;
    try {
        for (;;) {
            const { done, value } = await reader.read();
            if (done) break;
            if (length + value.byteLength > MAX_REQUEST_BYTES) {
                await reader.cancel();
                return empty(413);
            }
            bytes.set(value, length);
            length += value.byteLength;
        }
        const text = new TextDecoder("utf-8", { fatal: true }).decode(bytes.subarray(0, length));
        return JSON.parse(text);
    } catch {
        return empty(400);
    } finally {
        reader.releaseLock();
    }
}

function validate(payload, now) {
    if (!payload || typeof payload !== "object" || Array.isArray(payload) || Object.keys(payload).length !== 3) {
        return 400;
    }
    const { id, week, counts } = payload;
    if (typeof id !== "string" || !UUID_V4.test(id)) return 400;
    if (typeof week !== "string" || !/^\d{4}-\d{2}-\d{2}$/u.test(week)) return 400;
    const start = new Date(`${week}T00:00:00.000Z`);
    if (start.getUTCDay() !== 1 || start.toISOString().slice(0, 10) !== week) return 400;

    if (!counts || typeof counts !== "object" || Array.isArray(counts)) return 400;
    const metrics = Object.entries(counts);
    if (!metrics.length) return 400;
    for (const [key, value] of metrics) {
        if (!METRIC_KEYS.some(pattern => pattern.test(key)) || !Number.isInteger(value) || value < 1 || value > MAX_COUNT) {
            return 400;
        }
    }

    const daysSinceMonday = (now.getUTCDay() + 6) % 7;
    const currentWeek = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate())
        - daysSinceMonday * DAY_MS;
    if (start.getTime() >= currentWeek) return 400;
    if (start.getTime() < currentWeek - 4 * WEEK_MS) return 410;
    return 204;
}

async function receive(request, db) {
    const payload = await readPayload(request);
    if (payload instanceof Response) return payload;
    const status = validate(payload, new Date());
    if (status !== 204) return empty(status);

    const { id, week, counts } = payload;
    try {
        // The two statements share a transaction. Only a new receipt contributes counts.
        await db.batch([
            db.prepare(CLAIM_BATCH_SQL).bind(id, week),
            db.prepare(ADD_TOTALS_SQL).bind(week, JSON.stringify(counts)),
        ]);
        return empty(204);
    } catch {
        return empty(503);
    }
}

function metricLabels(metricKey) {
    const [scope, name, action] = metricKey.split(".");
    if (scope === "screen") return { scope, screen: SCREENS[name] };
    if (scope === "challenge") return { scope, outcome: OUTCOMES[name] };

    const index = Number(name);
    const exercise = scope === "game"
        ? GAME_EXERCISES[index] ?? `Taso ${index + 1}`
        : LESSON_EXERCISES[index] ?? `Oppitunti ${index + 1}`;
    return { scope, exercise, outcome: OUTCOMES[action] };
}

async function exportEvent({ week, metric_key, total, contributors }) {
    // RFC 9562 UUIDv8 derived solely from the finalized aggregate cell.
    const seed = new TextEncoder().encode(`${POSTHOG_APP}/${POSTHOG_EVENT}/${week}/${metric_key}`);
    const bytes = new Uint8Array(await crypto.subtle.digest("SHA-256", seed)).subarray(0, 16);
    bytes[6] = (bytes[6] & 0x0f) | 0x80;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const uuid = bytes.toHex().replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/u, "$1-$2-$3-$4-$5");
    return {
        event: POSTHOG_EVENT,
        uuid,
        timestamp: new Date(week).toISOString(),
        properties: {
            distinct_id: "clocklings-blender-weekly-aggregate",
            $process_person_profile: false,
            $geoip_disable: true,
            app: POSTHOG_APP,
            week,
            metric_key,
            ...metricLabels(metric_key),
            total,
            contributors,
        },
    };
}

async function exportFinalWeeks(env) {
    const { DB, POSTHOG_PROJECT_TOKEN } = env;
    await DB.batch([
        DB.prepare(`DELETE FROM batch_receipts WHERE week < ${FIRST_ACCEPTED_WEEK_SQL}`),
        DB.prepare(`DELETE FROM weekly_totals WHERE week < ${FIRST_ACCEPTED_WEEK_SQL} AND contributors < ?`)
            .bind(MIN_CONTRIBUTORS),
    ]);
    const { results } = await DB.prepare(`
        SELECT week, metric_key, total, contributors FROM weekly_totals
        WHERE week < ${FIRST_ACCEPTED_WEEK_SQL} AND contributors >= ?
        ORDER BY week, metric_key LIMIT 200`).bind(MIN_CONTRIBUTORS).all();
    if (!results.length) return;

    if (env.POSTHOG_HOST !== POSTHOG_HOST || !POSTHOG_PROJECT_TOKEN) {
        throw new Error("Missing Clocklings analytics export configuration");
    }
    const batch = await Promise.all(results.map(exportEvent));
    const response = await fetch(`${POSTHOG_HOST}/batch/`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ api_key: POSTHOG_PROJECT_TOKEN, batch }),
        redirect: "manual",
        signal: AbortSignal.timeout(15_000),
    });
    if (!response.ok) {
        await response.body?.cancel();
        throw new Error("Clocklings analytics export failed");
    }
    const acknowledgment = await response.json().catch(() => null);
    if (acknowledgment?.status !== "Ok" || acknowledgment.quota_limited?.length) {
        throw new Error("Clocklings analytics export was not acknowledged");
    }

    // Keep frozen totals until PostHog accepts them. Retries use the same event UUIDs.
    const exportedKeys = results.map(({ week, metric_key }) => [week, metric_key]);
    await DB.prepare(`
        DELETE FROM weekly_totals WHERE (week, metric_key) IN (
            SELECT json_extract(value, '$[0]'), json_extract(value, '$[1]') FROM json_each(?)
        )`).bind(JSON.stringify(exportedKeys)).run();
}

export default {
    fetch(request, env) {
        const { pathname, search } = new URL(request.url);
        if (search) return empty(400);
        if (pathname === "/health" && request.method === "GET") return empty(204);
        if (pathname !== "/v1/metrics") return empty(404);
        if (request.method !== "POST") return empty(405);
        return receive(request, env.DB);
    },

    scheduled(_controller, env, context) {
        context.waitUntil(exportFinalWeeks(env));
    },
};
