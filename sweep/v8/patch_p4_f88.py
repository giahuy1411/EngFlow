import io, sys

def patch(path, pairs, eol="\n"):
    t = io.open(path, encoding="utf-8", newline="").read()
    use_crlf = t.count("\r\n") == t.count("\n")
    t = t.replace("\r\n", "\n")
    for old, new in pairs:
        if old not in t:
            print("NOT FOUND in", path, "::", old[:80].replace("\n", "\\n")); sys.exit(1)
        t = t.replace(old, new, 1)
    io.open(path, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if use_crlf else t)
    print("patched", path)

# p4b: bai tao ra la NHAP (isPublished:false) -> guest 404 (audit-v8 F88), admin 200;
# sau khi update sang isPublished:true thi guest phai 200 lai.
patch("sweep/v8/p4b.js", [(
'''    r = await probe("VL public detail", "GET", "/api/v1/video-lessons/" + vid, "none", 200);
    let tc = null; try { tc = JSON.parse(r.txt).transcript.length; } catch (e) {}
    console.log("  transcript lines=" + tc);''',
'''    // audit-v8 F88: fixture nay la NHAP (isPublished:false) -> guest phai 404, admin van 200.
    await probe("VL draft detail guest 404", "GET", "/api/v1/video-lessons/" + vid, "none", 404);
    r = await probe("VL draft detail admin 200", "GET", "/api/v1/video-lessons/" + vid, "admin", 200);
    let tc = null; try { tc = JSON.parse(r.txt).transcript.length; } catch (e) {}
    console.log("  transcript lines=" + tc);'''),
(
'''    await probe("VL bad level 400"''',
'''    await probe("VL published detail guest 200", "GET", "/api/v1/video-lessons/" + vid, "none", 200);
    await probe("VL bad level 400"'''),
], "\n")

# p4c: cung fixture nhap -> guest 404 + admin 200
patch("sweep/v8/p4c.js", [(
'''  r = await probe("VL public detail", "GET", "/api/v1/video-lessons/" + vid, "none", 200);''',
'''  // audit-v8 F88: fixture la NHAP -> guest 404, admin 200 (xem p4b cho nhanh published).
  await probe("VL draft detail guest 404", "GET", "/api/v1/video-lessons/" + vid, "none", 404);
  r = await probe("VL draft detail admin 200", "GET", "/api/v1/video-lessons/" + vid, "admin", 200);'''),
], "\n")
