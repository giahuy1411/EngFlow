import urllib.request, os, email.parser

D = "C:/Users/ASUS/Documents/LAPTRINH/engflow/uploads"
names = sorted(f for f in os.listdir(D) if f.endswith((".html", ".js", ".svg")))

def head(base, path):
    req = urllib.request.Request(base + path, method="GET")
    try:
        r = urllib.request.urlopen(req, timeout=20)
        return r.status, r.headers, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers, e.read()

print("files on disk:", names)
for f in names:
    for base in ["http://localhost:8080", "http://localhost:5173"]:
        st, h, body = head(base, "/api/resources/" + f)
        print("  %s | %s%s -> %s CT=%s DISP=%s LEN=%d" % (
            f[:20], base.split("//")[1][:9], "", st,
            h.get("Content-Type"), h.get("Content-Disposition"), len(body)))
