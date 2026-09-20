const lib = require("./lib.js");
const { probe } = lib;
(async () => {
  await lib.initTokens();
  await probe("SRS review vocab 10017 q5", "POST", "/api/srs/review", "user", 200, { vocabId: 10017, quality: 5 });
  lib.report("F106-REPRO");
})().catch(e => { console.error(e); process.exit(1); });