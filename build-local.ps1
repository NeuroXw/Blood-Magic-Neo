param(
    [string]$JavaHome,
    [string]$ProxyHost = '',
    [int]$ProxyPort = 6789,
    [string[]]$GradleTasks = @('build')
)

$ErrorActionPreference = 'Stop'
$previousJavaHome = $env:JAVA_HOME
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
Push-Location $PSScriptRoot
try {
    if (-not $JavaHome) {
        $localJdk = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '../.tools') -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | Select-Object -First 1
        $JavaHome = if ($localJdk) { $localJdk.FullName } else { $env:JAVA_HOME }
    }
    if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/javac.exe'))) {
        throw 'Set -JavaHome to a Java 25 JDK directory, or set JAVA_HOME.'
    }
    $env:JAVA_HOME = $JavaHome
    if ($ProxyHost) {
        $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions -Dhttp.proxyHost=$ProxyHost -Dhttp.proxyPort=$ProxyPort -Dhttps.proxyHost=$ProxyHost -Dhttps.proxyPort=$ProxyPort -Dhttp.nonProxyHosts=localhost|127.*".Trim()
    }
    & ./gradlew.bat @GradleTasks --console=plain
    $buildExit = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    Pop-Location
}
exit $buildExit
