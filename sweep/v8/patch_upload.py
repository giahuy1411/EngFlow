import io

ROOT = r"C:\Users\ASUS\Documents\LAPTRINH\engflow\src\main\java\com\datn\engflow"


def rw(path, pairs):
    s = io.open(path, encoding="utf-8").read()
    for old, new in pairs:
        n = s.count(old)
        if n != 1:
            raise SystemExit("MATCH %d for %s in %s" % (n, old[:60].replace(chr(10), "|"), path))
        s = s.replace(old, new)
    io.open(path, "w", encoding="utf-8", newline="").write(s)
    print("patched", path)


# ---------- LessonStructureController: write site ----------
OLD_UP = """            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(\".\")) {
                ext = originalName.substring(originalName.lastIndexOf(\".\"));
            }
            String filename = UUID.randomUUID().toString() + ext;"""
NEW_UP = """            // audit-v8 F81: extension allowlist at write time -- /api/resources is
            // permitAll and same-origin as the SPA, so an uploaded .html became a
            // first-party page (stored XSS -> JWT theft). See SafeUploadNames.
            String ext = SafeUploadNames.extensionOf(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + "." + ext;"""

# ---------- LessonStructureController: read site ----------
OLD_CT = """            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = \"application/octet-stream\";
            }"""
NEW_CT = """            // audit-v8 F81: pin the content type from the extension instead of
            // probing it, so a file already on disk (or any future writer) can
            // never be served as an active document.
            String contentType = SafeUploadNames.contentTypeFor(clean);"""

rw(ROOT + r"\controller\LessonStructureController.java", [
    (OLD_UP, NEW_UP),
    (OLD_CT, NEW_CT),
])

# ---------- LessonSubmissionService: write site ----------
OLD_SV = """        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(\".\")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(\".\"));
        } else {
            extension = \".webm\"; // Default format for media recorder in browsers
        }

        String uniqueFileName = UUID.randomUUID().toString() + extension;"""
NEW_SV = """        // audit-v8 F81: this endpoint is reachable by any logged-in learner, and the
        // returned URL is served from uploads/ by the permitAll /api/resources route.
        // A client-chosen extension (lesson.html) turned it into same-origin script.
        String extension = SafeUploadNames.extensionOf(file.getOriginalFilename());

        String uniqueFileName = UUID.randomUUID().toString() + \".\" + extension;"""

rw(ROOT + r"\service\LessonSubmissionService.java", [(OLD_SV, NEW_SV)])
