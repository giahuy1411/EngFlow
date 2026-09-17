import sys


PAIRS = [
    ("A", "src/main/java/com/datn/engflow/controller/LessonStructureController.java"),
    ("B", "src/main/java/com/datn/engflow/service/LessonSubmissionService.java"),
    ("C", "src/main/java/com/datn/engflow/controller/LessonStructureController.java"),
    ("D", "src/main/java/com/datn/engflow/controller/LessonStructureController.java"),
]


def read(p):
    return open(p, encoding="utf-8").read()


def write(p, s):
    open(p, "w", encoding="utf-8", newline="").write(s)


def sub(path, old, new, label):
    s = read(path)
    n = s.count(old)
    if n != 1:
        print("SKIP", label, "occurrences=" + str(n))
        return False
    write(path, s.replace(old, new))
    print("OK", label)
    return True


A_old = """        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;"""
A_new = """        try {
            String ext = SafeUploadNames.extensionOf(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + "." + ext;"""

B_old = """        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            extension = ".webm"; // Default format for media recorder in browsers
        }

        String uniqueFileName = UUID.randomUUID().toString() + extension;"""
B_new = """        // audit-v8 F81: the returned path is served by /api/resources/** to any
        // visitor, so the recorder upload must not be able to carry a browser-active
        // extension. Default to .webm only when the client sent no name at all.
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            originalFilename = "recording.webm";
        }
        String extension = "." + SafeUploadNames.extensionOf(originalFilename);

        String uniqueFileName = UUID.randomUUID().toString() + extension;"""

C_old = """            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }"""
C_new = """            // audit-v8 F81: pin the type from the allowlisted extension instead of
            // probing the file, so a .html/.svg/.js already on disk cannot be
            // rendered as a first-party document.
            String contentType = SafeUploadNames.contentTypeFor(clean);"""

D_old = """    @GetMapping("/api/resources/{filename:.+}")
    public ResponseEntity<Resource> getResource(@PathVariable String filename) {"""
D_new = """    @GetMapping("/api/resources/{filename:.+}")
    public ResponseEntity<Resource> getResource(@PathVariable String filename) {"""

edits = [
    (PAIRS[0][1], A_old, A_new, "A admin upload ext"),
    (PAIRS[1][1], B_old, B_new, "B lesson submission audio ext"),
    (PAIRS[2][1], C_old, C_new, "C pinned content type"),
]
done = 0
for path, old, new, label in edits:
    if sub(path, old, new, label):
        done += 1
print("applied " + str(done) + "/" + str(len(edits)))
