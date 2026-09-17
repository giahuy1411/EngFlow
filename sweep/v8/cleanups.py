import os, time, json

d = "C:/Users/ASUS/Documents/LAPTRINH/engflow/uploads"
real_d = os.path.realpath(d)
cutoff = time.mktime(time.strptime("2026-09-14", "%Y-%m-%d"))
keep, gone = [], []
for f in sorted(os.listdir(d)):
    p = os.path.realpath(os.path.join(d, f))
    assert p.startswith(real_d + os.sep) and os.path.isfile(p), p
    st = os.stat(p)
    if st.st_mtime >= cutoff:
        os.remove(p)
        gone.append(f)
    else:
        keep.append((f, time.strftime("%Y-%m-%d", time.localtime(st.st_mtime))))
print("removed", len(gone), "mine")
print("kept (pre-existing):", json.dumps(keep))
print("now:", sorted(os.listdir(d)))
