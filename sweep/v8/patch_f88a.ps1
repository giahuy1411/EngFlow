$ErrorActionPreference = "Stop"
function Patch($path, $eol, $pairs) {
  $t = [IO.File]::ReadAllText($path)
  foreach ($p in $pairs) {
    $old = ($p[0] -split "`n") -join $eol
    $new = ($p[1] -split "`n") -join $eol
    if (-not $t.Contains($old)) { throw "PATTERN NOT FOUND in $path :: " + $p[0].Substring(0, [Math]::Min(60, $p[0].Length)) }
    $t = $t.Replace($old, $new)
  }
  [IO.File]::WriteAllText($path, $t)
  "patched $path"
}

# A) VideoDtos: transcript khong con @NotNull (null = giu nguyen khi update)
Patch "src/main/java/com/datn/engflow/model/dto/video/VideoDtos.java" "`n" @(
  ,@('            @NotNull List<TranscriptLine> transcript,',
     "            // audit-v8 F88: null = `"giu nguyen phu de hien co`" khi UPDATE (audit-v6 F21), nen KHONG duoc`n            // @NotNull o day — annotation do tung lam guard trong service thanh dead code va`n            // PUT tu admin UI (bo trong o phu de) luon 400. CREATE van bi chan boi`n            // VideoLessonService.validateTranscript: 400 `"Transcript can it nhat 2 dong`".`n            List<TranscriptLine> transcript,")
)
