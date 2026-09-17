const J = JSON.stringify;
const msg = "hi";
console.log(J({msg: msg, n: 2}), 1 <= 2, 7 % 2, process.env.HOME || "no-home");
const tpl = "rate_limit:127.0.0.1:global";
console.log(tpl, msg + ":" + 2, "dollar-inside-single-quotes: OK");
