# Invalid-data fixtures

Each folder is the golden dataset with exactly one defect. The loader must reject it and report the rule shown.
Regenerate with `java -jar leaselens-generator/target/leaselens-generator.jar fixtures`.

| Folder | Expected rule | Defect |
|---|---|---|
| `01-missing-file` | `MISSING_FILE` | expenses.csv is not supplied. |
| `02-empty-file` | `EMPTY_FILE` | properties.csv is completely empty. |
| `03-missing-column` | `MISSING_COLUMN` | leases.csv has no monthly_rent column. |
| `04-duplicate-property-id` | `DUPLICATE_ID` | P001 appears twice in properties.csv. |
| `05-duplicate-lease-id` | `DUPLICATE_ID` | Lease L003 is renamed L002, duplicating an id. |
| `06-duplicate-transaction-id` | `DUPLICATE_ID` | Transaction TX008 is renamed TX007. |
| `07-unknown-property-reference` | `UNKNOWN_REFERENCE` | Lease L002 refers to property P999. |
| `08-unknown-lease-reference` | `UNKNOWN_REFERENCE` | Obligation O008 refers to lease L999. |
| `09-unknown-obligation-reference` | `UNKNOWN_REFERENCE` | Transaction TX008 refers to obligation O999. |
| `10-invalid-date` | `INVALID_DATE` | Obligation O002 is due on 2026-02-30. |
| `11-invalid-lease-period` | `INVALID_LEASE_PERIOD` | Lease L003 ends before it starts. |
| `12-negative-rent` | `INVALID_AMOUNT` | Lease L001 has a monthly rent of -25000.00. |
| `13-negative-expense` | `INVALID_AMOUNT` | Expense E003 has an amount of -12000.00. |
| `14-invalid-transaction-amount` | `INVALID_NUMBER` | Transaction TX005 has amount 'abc'. |
| `15-overpayment` | `OVERPAYMENT` | Transaction TX008 pays 35000.00 against a 30000.00 obligation. |
| `16-reconciliation-mismatch` | `RECONCILIATION_MISMATCH` | Obligation O005 declares amount_paid 100000.00 but its transactions total 120000.00. |
| `17-missing-value` | `MISSING_VALUE` | Property P001 has a blank rentable_area_sqft. |
| `18-leased-area-exceeds-rentable` | `LEASED_AREA_EXCEEDS_RENTABLE` | Lease L002 covers 5000 sq ft of a 3000 sq ft property. |
| `19-overlapping-occupancy` | `OCCUPANCY_EXCEEDS_RENTABLE` | From 2026-02-01, L001 (1000) and L003 (1500) together exceed P001's 2000 sq ft. |
| `20-invalid-category` | `INVALID_VALUE` | Expense E005 has category MISC. |
| `21-malformed-row` | `MALFORMED_ROW` | A tenants.csv row has an extra value. |
| `22-invalid-billing-day` | `INVALID_VALUE` | Lease L001 has billing day 31 (allowed 1-28). |
| `23-no-data-rows` | `EMPTY_FILE` | rent_obligations.csv has a header but no rows. |
