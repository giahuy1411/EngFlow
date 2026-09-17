p = "src/main/java/com/datn/engflow/controller/LessonStructureController.java"
s = open(p, encoding="utf-8").read()

old = """            org.springframework.core.io.Resource res =
                    new org.springframework.core.io.FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(7)))
                    .body(res);"""

new = """            org.springframework.core.io.Resource res =
                    new org.springframework.core.io.FileSystemResource(filePath.toFile());
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(7)));
            if (SafeUploadNames.forceDownload(clean)) {
                builder = builder.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment");
            }
            return builder.body(res);"""

print("occurrences", s.count(old))
if s.count(old) == 1:
    open(p, "w", encoding="utf-8", newline="").write(s.replace(old, new))
    print("OK disposition")
