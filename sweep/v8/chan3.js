const J = JSON.stringify;
const o = {a: 1, b: [2,3]};
console.log(J(o), 1 <= 2, 5 % 2, o.a | o.b.length);
if (o) { console.log('ok inside'); }
