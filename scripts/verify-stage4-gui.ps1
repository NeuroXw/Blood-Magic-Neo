param([switch]$Restart)
# Historical implementation: docs/archive/stage4/scripts
Write-Output 'Running the current stage 6 suite, including the altar GUI regressions.'
& (Join-Path $PSScriptRoot 'verify-stage6-machines.ps1') -Restart:$Restart
