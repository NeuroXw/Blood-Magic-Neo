param([switch]$Restart)
# Historical implementation: docs/archive/stage5/scripts/verify-stage5-orbs.ps1
Write-Output 'Running the current stage 6 suite, including the stage 5 regressions.'
& (Join-Path $PSScriptRoot 'verify-stage6-machines.ps1') -Restart:$Restart
