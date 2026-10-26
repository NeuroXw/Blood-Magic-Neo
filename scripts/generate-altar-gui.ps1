# Reproducible code-drawn GUI texture; no third-party art or graphics context required.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$bitmap = [System.Drawing.Bitmap]::new(176, 204)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
function Fill-Rect([string]$color, [int]$x, [int]$y, [int]$width, [int]$height) {
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($color))
    try { $graphics.FillRectangle($brush, $x, $y, $width, $height) } finally { $brush.Dispose() }
}
function Draw-Slot([int]$x, [int]$y) {
    Fill-Rect '#645357' ($x - 1) ($y - 1) 18 18
    Fill-Rect '#F1E3DF' $x $y 17 17
    Fill-Rect '#9E8C8D' $x $y 16 16
}
try {
    Fill-Rect '#342126' 0 0 176 204
    Fill-Rect '#EEE1DB' 1 1 174 202
    Fill-Rect '#897378' 3 3 172 200
    Fill-Rect '#CDBDB7' 3 3 170 198
    Fill-Rect '#7B2335' 6 17 164 1
    Draw-Slot 26 42
    Fill-Rect '#6B5055' 63 42 106 10
    Fill-Rect '#E4D5CE' 64 43 104 8
    Fill-Rect '#6B5055' 7 95 162 8
    Fill-Rect '#A18B8A' 8 96 160 6
    foreach ($row in 0..2) { foreach ($column in 0..8) { Draw-Slot (8 + 18 * $column) (122 + 18 * $row) } }
    foreach ($column in 0..8) { Draw-Slot (8 + 18 * $column) 180 }
    $destination = Join-Path $PSScriptRoot '../src/main/resources/assets/bloodmagic/textures/gui/altar.png'
    New-Item -ItemType Directory -Force (Split-Path $destination) | Out-Null
    $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}
