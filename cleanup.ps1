$filesToDelete = @(
    "backend-err.log",
    "backend-jar-err.log",
    "backend-jar.log",
    "backend-out.log",
    "backend-start.log",
    "engflow.log",
    "compile.log",
    "frontend-err.log",
    "frontend.log",
    "frontend\frontend.err",
    "frontend\frontend.log",
    "frontend\vite-err.txt",
    "frontend\vite-log.txt",
    "frontend\decks-public-initial.txt",
    "chrome-wrapper.cs",
    "chrome-wrapper.exe",
    "csc-help.txt",
    "homepage.txt",
    "snapshot_public.txt",
    "wayback-a1-grammar.html",
    "login.json",
    "flashcard-game.png",
    "lessons_page.png",
    "listening-game.png",
    "memory-game.png",
    "quiz-game-v2.png",
    "quiz-game.png",
    "screenshot_mydecks.png",
    "screenshot_mydecks_fixed.png",
    "screenshot_public.png",
    "screenshot_public_fixed.png",
    "typing-game-check.png",
    "typing-game-v2.png",
    "typing-game.png",
    "frontend\my-decks-tab-after.png",
    "frontend\my-decks-tab-before.png",
    "frontend\public-tab-after.png",
    "frontend\public-tab-before.png",
    "files_to_delete.txt"
)

$foldersToDelete = @(
    ".brave-debug-user-data"
)

Write-Host "Bắt đầu xóa file rác..." -ForegroundColor Cyan

foreach ($file in $filesToDelete) {
    if (Test-Path $file) {
        Remove-Item -Path $file -Force
        Write-Host "Đã xóa: $file" -ForegroundColor Green
    }
}

foreach ($folder in $foldersToDelete) {
    if (Test-Path $folder) {
        Remove-Item -Path $folder -Recurse -Force
        Write-Host "Đã xóa thư mục: $folder" -ForegroundColor Green
    }
}

Write-Host "Hoàn tất dọn dẹp!" -ForegroundColor Cyan
