import json, urllib.request, uuid

BASE = "http://localhost:8080"

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data,
                                 headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

tok = login("admin@gmail.com", "123456")
print("token?", bool(tok))

marker = "XSSPROBE" + uuid.uuid4().hex[:8]
payload = ("<html><body><h1>" + marker + "</h1></body></html>").encode()

for fname in ["probe.html", "probe.svg", "probe.js"]:
    b = uuid.uuid4().hex
    body = b"".join([
        ("--" + b + "\r\n").encode(),
        ('Content-Disposition: form-data; name="file"; filename="' + fname + '"\r\n').encode(),
        ("Content-Type: application/octet-stream\r\n\r\n").encode(),
        payload, b"\r\n",
        ("--" + b + "--\r\n").encode(),
    ])
    req = urllib.request.Request(BASE + "/api/admin/upload", data=body, method="POST", headers={
        "Authorization": "Bearer " + tok,
        "Content-Type": "multipart/form-data; boundary=" + b,
    })
    try:
        r = urllib.request.urlopen(req)
        loc = json.load(r).get("url")
        print("UPLOAD", fname, "->", loc)
        r2 = urllib.request.urlopen(BASE + loc)
        head = r2.read(200).decode("utf-8", "replace")
        print("   CT:", r2.headers.get("Content-Type"),
              "| nosniff:", r2.headers.get("X-Content-Type-Options"),
              "| CSP:", str(r2.headers.get("Content-Security-Policy"))[:40],
              "| marker:", marker in head)
    except Exception as e:
        print("UPLOAD", fname, "ERR", e)
