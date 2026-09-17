import subprocess, os, sys
repo = r\ C:\Users\ASUS\Documents\LAPTRINH\engflow\
log = os.path.join(repo, sys.argv[1])
f = open(log, \wb\, 0)
DET = 0x00000008 | 0x00000200
p = subprocess.Popen([\cmd\, \/c\, \mvnw.cmd\, \test\], cwd=repo, stdout=f, stderr=subprocess.STDOUT, creationflags=DET)
print(\launched pid\, p.pid, \log\, log)
