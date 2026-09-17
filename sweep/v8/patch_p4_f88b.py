import io, sys
def patch(path, pairs):
    t = io.open(path, encoding="utf-8", newline="").read()
    crlf = t.count("\r\n") == t.count("\n")
    t = t.replace("\r\n", "\n")
    for old, new in pairs:
        if old not in t:
            print("NOT FOUND in", path, "::", old[:90].replace("\n", "\\n")); sys.exit(1)
        t = t.replace(old, new, 1)
    io.open(path, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
    print("patched", path)

# upload xong doc lai bang ADMIN token (bai upload la nhap -> guest 404 theo F88)
patch("sweep/v8/p4b.js", [(
'const d = await (await fetch("http://localhost:8080/api/v1/video-lessons/" + upId)).json();',
'const d = await (await fetch("http://localhost:8080/api/v1/video-lessons/" + upId, { headers: { Authorization: "Bearer " + lib.getAdmin() } })).json();')])
patch("sweep/v8/p4c.js", [(
'const dd = await (await fetch("http://localhost:8080/api/v1/video-lessons/" + uid)).json();',
'const dd = await (await fetch("http://localhost:8080/api/v1/video-lessons/" + uid, { headers: { Authorization: "Bearer " + lib.getAdmin() } })).json();')])
