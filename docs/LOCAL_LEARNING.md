# Optional local learning and bounded storage

## User-visible behavior

Learning defaults to **Off**. Settings offer Off, 1, 7, 30 and 90 days, clear-all, and private mode. The keyboard toolbar also exposes private mode. Private mode suppresses both recording and lookup of remembered candidates; ordinary static dictionary/phrase conversion still works.

Settings changes notify the active IME and invalidate previews/callbacks. The Settings private checkbox, height, haptics and flick controls independently refresh on resume and preference notifications without writing the display state back. A failed private-state read displays ON. The preference is also resynchronized at input-view start and before edits, including a return to the same editor. Learned lookup, recording and queued worker processing independently read the current preference. Both directions discard queued choices; switching off never revives old callbacks. A pre-toggle file operation already past its final privacy check may finish; private mode does not erase previously accepted records. Preview cancellation is entirely local; it does not depend on host restoration. A detected dispatch failure retains the ordinary editable draft and does not create a learned record. A true result acknowledges dispatch, not host application; a host can filter/drop it after the keyboard clears the draft, and an otherwise eligible learned choice may then remain. No post-confirmation recovery buffer or automatic resend is added.

Only a candidate tap or an explicitly confirmed preview (確定/Enter) dispatched without a detected error is eligible, and only when the complete reading/word is an exact choice in the bundled dictionary. Records are the reading, selected word and last explicit-selection timestamp. Up to eight exact/prefix remembered candidates precede ordinary dictionary candidates. Prefix choices keep their complete original reading. Previewing, reading, raw kana fallback, typing/moving/switching, cancellation, okurigana-derived words and generated phrases do not create records. Ordinary input has no implicit commit. Space appends locally and does not confirm.

Password variants, NO_PERSONALIZED_LEARNING, NO_SUGGESTIONS, numeric/phone/date, forced-ASCII, email/URI, person-name/postal-address and private mode disallow recording and remembered lookup. Normal editor flags do not reveal semantic sensitivity. Leave learning off or use private mode for secrets in an ordinary field. NO_SUGGESTIONS remains independently ineligible when explicit fixed conversion is enabled;CandidateList never invokes the history supplier. Field-start/count maintenance is gated too. Retained enabled data may initialize/clean up at app startup independently of field text;manual conversion does not query or record it.

## Retention and deletion

Age is elapsed wall-clock time since the last explicit selection. Lookup excludes expired and future-dated records immediately. Start/use schedules background pruning; shortening retention removes newly expired records. Extension first applies the old period, so deleted/expired records cannot revive. Reads/previews do not renew timestamps. No alarm deletes files while the app is stopped.

Clear/Off immediately replaces the visible learned state, advances its generation and drops queued choices. Older in-progress publications are ignored; a queued clear follows any active write. Candidate revisions and learned-state generations reject old callbacks. An expired/cleared learned preview is cancelled locally back to its reading before further confirmation. No host span is restored and no key is blocked by a callback ambiguity fence. Visible preedit and accessibility text use one snapshot taken after invalidation; the expired/cleared preview cannot remain displayed while confirmation uses the restored reading. Host projection tests pass; native Android views remain untested.

Clear-all keeps the period for future explicit choices; Off deletes records and stops recording. The interface reports a completed clear only after the file operation succeeds. Failed I/O/policy writes stop learning and show an error. A persisted cleanup-required flag forces cleanup before a later process restores data. If the policy itself cannot be persisted, durable settings guarantees are unavailable and failure is reported. Interrupted policy changes may discard all learning rather than restore old data.

Clear-all does not delete the static dictionary, display preferences, text already sent to the host, or OS-level copies. A word may still appear from the dictionary after its learned record is deleted. Removing files/Java references is not physical secure erasure.

## Bounds and storage path

- At most **128 records**, additionally limited to **32,768 serialized bytes**.
- Reading up to 32 kana code points; word up to 48 code points; malformed/control/duplicate records are rejected.
- Deterministic oldest-first eviction, with reading/word tie-breaks.
- At most **32 pending choices**, coalesced by lexical pair. A burst does not enqueue one executor task/file write per key.
- One shared application-scoped worker, one notification slot. File/policy operations stay off the input/UI thread; candidate reads use immutable memory only.
- Fixed files learning-v1.bin and learning-v1.tmp beneath Context.getNoBackupFilesDir(), resolved lazily on the worker. Each is capped at 32 KiB. Atomic replacement is required; unsupported filesystems fail closed instead of creating a journal/backup.
- Interrupted temporary files are discarded on read; write completion removes temporary files. Oversized/corrupt stores stop learning and trigger cleanup.

No runtime dictionary download, disk dictionary cache, history log or extra executable dependency exists. The dictionary stays in read-only APK assets: **1,209,080 bytes uncompressed**. The0.1.8 signed candidate is **516,774 bytes**. The managed learning files peak at at most 64 KiB while replacing; Android preference bookkeeping, installation files and filesystem metadata are additional. Actual Android installed size and enabled-learning latency have not been measured.

All nine backup domains remain excluded in compiled legacy/cloud/device-transfer XML and allowBackup=false. Android also documents no-backup storage in [Context](https://developer.android.com/reference/android/content/Context#getNoBackupFilesDir()). These are source/packaged controls, not an OEM-transfer runtime test. Records are plaintext inside the app UID boundary; OS/root compromise is outside the threat model.

## Tested scope

Production core/coordinator and real Linux host file adapter tests pass retention, quotas, repeated bursts, reload/corruption, stale generations, interrupted writes, clear during active writes, Off, failure reporting and preservation of unrelated synthetic files. Production Settings/IME controls run in an explicitly limited host UI model. Android event-loop, native app-private storage ownership, backup/transfer, real settings clicks and touch latency remain **NOT RUN**. See [VERIFICATION.md](VERIFICATION.md).
