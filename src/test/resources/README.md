# Test data exports

`merchant-104180-export.sql` is a sanitized snapshot of the `merchants-core`
tables used by the test Oracle for merchant `104180`:

- `AP#MERCHANTS` — 1 row;
- `MERC_CONFIG` — 73 configuration rows, including historical validity periods;
- `TERMINALSETTINGS` — 20 terminal rows.

The export is intended for local and H2 based tests after `schema.sql`. It keeps
the configuration keys, values used by limit and feature logic, and validity
intervals. Credentials, personal names, account numbers, tax identifiers,
terminal identifiers, URLs, and endpoint credentials are deterministic test
values or `NULL`; no raw test-Oracle secrets are stored in the repository.

The file is an explicit fixture and is not loaded by Spring's default
`data.sql` initialization. Load it from a test that needs the `104180` dataset.
