$filesToDelete = @(
    "src\main\java\com\datn\engflow\service\CoinService.java",
    "src\main\java\com\datn\engflow\controller\CoinController.java",
    "frontend\src\services\coinService.js",
    "frontend\src\components\bauhaus\CoinDisplay.vue",
    "src\main\java\com\datn\engflow\model\entity\GameSession.java",
    "src\main\java\com\datn\engflow\repository\GameSessionRepository.java"
)

foreach ($file in $filesToDelete) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "Đã xóa: $file" -ForegroundColor Green
    } else {
        Write-Host "Không tìm thấy: $file" -ForegroundColor Yellow
    }
}
Write-Host "Hoàn tất dọn dẹp các file rác (Coins & Game Sessions)!" -ForegroundColor Cyan
