# `dictionary/` — data used by the tra-từ dictionary lookup

## `common-words.txt`

The word list `DictionaryWarmupService` pre-warms into the Redis dictionary cache at startup and
nightly, so frequent words answer instantly instead of paying the upstream's ~20 s cold lookup.

**Source (pinned).** New General Service List, version **1.2** (NGSL 1.2) — Browne, C., Culligan, B.,
& Phillips, J. (2013; v1.2 released 2023). Retrieved 2026-09-27 from
<https://www.newgeneralservicelist.com/new-general-service-list>. The file used is
`NGSL_12_stats.csv`; its `SFI Rank` column supplies the descending-frequency order of the words
below. The NGSL is a **2,809-word** list of the most important general-English words for
second-language learners, covering ~92% of general English text.

**License: CC BY-SA 4.0** (Creative Commons Attribution-ShareAlike 4.0 International). The NGSL
project states the lists are free to use **including commercially**, provided they are cited.

**Attribution (required by the license):**

> Browne, C., Culligan, B., & Phillips, J. *The New General Service List*. Retrieved from
> <https://www.newgeneralservicelist.com>.

**ShareAlike:** this data file is distributed under CC BY-SA 4.0. License text:
<https://creativecommons.org/licenses/by-sa/4.0/>.

### Format

One word per line, lower-case, in descending frequency order. Lines starting with `#` and blank
lines are ignored by `DictionaryWarmupService.readWords()`.

### Notes

- The list is frequency-ordered, so it starts with function words (`the`, `be`, `and`, `of`, …).
  Some of those are not served by the upstream (measured: `be` → HTTP 404), so the warm-up skips
  them — expected, and it costs only a handful of lookups.
- Only **`dictionary.warmup.limit`** words are warmed per run (default 200, ≈70 min at the measured
  ~21 s/word). Raise `DICTIONARY_WARMUP_LIMIT` if the upstream is ever replaced with a faster one
  (see `dictionary.upstream-url`).
