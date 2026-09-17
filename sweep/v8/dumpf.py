import os, sys, glob
names = sys.argv[1].split(chr(44))
out = []
for r, d, fs in os.walk(sys.argv[2]):
    for f in fs:
        if f in names:
            p = os.path.join(r, f)
            s = open(p, encoding=chr(117)+chr(116)+chr(102)+chr(45)+chr(56), errors=chr(114)+chr(101)+chr(112)+chr(108)+chr(97)+chr(99)+chr(101)).read()
            out.append(chr(61)*30 + chr(32) + p + chr(10) + s)
# join
open(sys.argv[3], chr(119), encoding=chr(117)+chr(116)+chr(102)+chr(45)+chr(56)).write(chr(10).join(out))
print(len(out))
