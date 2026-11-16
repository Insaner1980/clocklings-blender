CREATE TABLE batch_receipts (
    id TEXT PRIMARY KEY,
    week TEXT NOT NULL
) WITHOUT ROWID;

CREATE INDEX batch_receipts_week ON batch_receipts (week);

CREATE TABLE weekly_totals (
    week TEXT NOT NULL,
    metric_key TEXT NOT NULL,
    total INTEGER NOT NULL,
    contributors INTEGER NOT NULL,
    PRIMARY KEY (week, metric_key)
) WITHOUT ROWID;
