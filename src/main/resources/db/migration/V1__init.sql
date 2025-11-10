CREATE TABLE IF NOT EXISTS companies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    reg_no TEXT NOT NULL,
    reputation REAL NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS branches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company_id INTEGER NOT NULL,
    world TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    rent REAL NOT NULL,
    upkeep REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS ledgers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company_id INTEGER NOT NULL,
    entry_at TEXT NOT NULL,
    debit_acct TEXT NOT NULL,
    credit_acct TEXT NOT NULL,
    amount REAL NOT NULL,
    memo TEXT
);

CREATE TABLE IF NOT EXISTS payroll_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company_id INTEGER NOT NULL,
    period TEXT NOT NULL,
    gross REAL NOT NULL,
    net REAL NOT NULL,
    processed_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS contracts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    a_company INTEGER NOT NULL,
    b_company INTEGER NOT NULL,
    kind TEXT NOT NULL,
    terms_json TEXT NOT NULL,
    status TEXT NOT NULL,
    signed_a INTEGER NOT NULL,
    signed_b INTEGER NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tender_id INTEGER NOT NULL,
    company_id INTEGER NOT NULL,
    price REAL NOT NULL,
    lead_time INTEGER NOT NULL,
    score REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS deliveries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    vehicle_id TEXT NOT NULL,
    route_json TEXT NOT NULL,
    eta TEXT NOT NULL,
    sla TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audits (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_uuid TEXT NOT NULL,
    action TEXT NOT NULL,
    detail_json TEXT NOT NULL,
    created_at TEXT NOT NULL,
    sig TEXT
);
