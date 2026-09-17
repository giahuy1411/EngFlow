import io, sys, json, struct, zlib, time, urllib.request, urllib.error
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
BASE = "http://localhost:8080"

def png_bytes():
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    w = h = 8
    raw = b"".join(b"\x00" + b"\x18\x84\x18" * w for _ in range(h))
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data, headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

def mp(path, token, field, fname, buf, ctype, extra=None):
    b = "----zz" + ("%x" % (int(time.time() * 1000) & 0xFFFFFFFF))
    pre = ("--" + b + "\r\n").encode()
    if extra:
        pre += ("Content-Disposition: form-data; name=\"" + extra[0] + "\"\r\n\r\n" + extra[1] + "\r\n--" + b + "\r\n").encode()
    head = ("Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + fname + "\"\r\n"
            "Content-Type: " + ctype + "\r\n\r\n").encode()
    body = pre + head + buf + ("\r\n--" + b + "--\r\n").encode()
    req = urllib.request.Request(BASE + path, data=body, method="POST",
                                 headers={"Authorization": "Bearer " + token,
                                          "Content-Type": "multipart/form-data; boundary=" + b})
    t0 = time.time()
    try:
        r = urllib.request.urlopen(req, timeout=120)
        return r.status, r.read().decode("utf-8", "replace")[:200]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:200]

admin = login("admin@gmail.com", "123456")
user = login("user@gmail.com", "123456")
png = png_bytes()
wav = open("frontend/public/e2e-tts.wav", "rb").read()
print("sizes: png", len(png), "wav", len(wav), "wav magic", wav[:4], wav[8:12])

print("avatar/upload REAL png      ->", mp("/api/auth/avatar/upload", user, "file", "a.png", png, "image/png"))
print("admin/audio-upload REAL wav ->", mp("/api/admin/audio-upload", admin, "file", "t.wav", wav[:400000], "audio/wav"))
print("admin/audio-upload fake     ->", mp("/api/admin/audio-upload", admin, "file", "f.wav", b"RIFF____fake", "audio/wav"))
