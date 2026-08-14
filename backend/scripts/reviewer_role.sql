-- Run this once against the live Railway Postgres database (not local Docker Postgres).
-- Not a Flyway migration: it's a one-time, out-of-repo credential-issuance step, not
-- part of the application schema, so it deliberately doesn't live under db/migration.
--
-- How to run it:
--   Railway dashboard -> your Postgres service -> Data tab -> Query -> paste and run.
--   (Or connect with psql using the connection string from the Postgres service's
--   "Connect" tab and run this file directly.)
--
-- Replace the password below before running, then share ONLY the resulting connection
-- string with the reviewer, out of band (not committed to the repo, not in this file).

CREATE ROLE reviewer WITH LOGIN PASSWORD 'REPLACE_WITH_A_STRONG_PASSWORD' NOSUPERUSER NOCREATEDB NOCREATEROLE;
GRANT CONNECT ON DATABASE railway TO reviewer;
GRANT USAGE ON SCHEMA public TO reviewer;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO reviewer;
