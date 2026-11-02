# Privacy and Data Handling

MoneyMap is designed with a strict **offline-first** philosophy.

1. **Storage**: All data is stored locally on the device in an encrypted SQLite database.
2. **Transmission**: No financial data is ever transmitted, uploaded, or shared.
3. **Receipts**: Images are stored in the device's local application storage and referenced by path in the database.
4. **OCR**: Smart Entry processes screenshots locally on the device using Google ML Kit.
5. **Permissions**:
   - `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (for receipt selection).
   - `POST_NOTIFICATIONS` (for backup/restore reminders).

MoneyMap does not collect, track, or share personal data.
