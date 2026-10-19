# Downloads every jar this project needs into .\lib
# Run from the csr_jdbc folder:
#   powershell -ExecutionPolicy Bypass -File download_jars.ps1

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
New-Item -ItemType Directory -Force -Path "lib" | Out-Null

$m = "https://repo1.maven.org/maven2"
$jars = @(
  # MySQL driver
  "$m/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar",
  # Apache POI (reads .xlsx)
  "$m/org/apache/poi/poi/5.2.5/poi-5.2.5.jar",
  "$m/org/apache/poi/poi-ooxml/5.2.5/poi-ooxml-5.2.5.jar",
  "$m/org/apache/poi/poi-ooxml-lite/5.2.5/poi-ooxml-lite-5.2.5.jar",
  "$m/org/apache/xmlbeans/xmlbeans/5.2.0/xmlbeans-5.2.0.jar",
  # POI dependencies
  "$m/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar",
  "$m/commons-io/commons-io/2.15.1/commons-io-2.15.1.jar",
  "$m/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.jar",
  "$m/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar",
  "$m/commons-codec/commons-codec/1.16.1/commons-codec-1.16.1.jar",
  "$m/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar",
  "$m/com/zaxxer/SparseBitSet/1.3/SparseBitSet-1.3.jar",
  "$m/com/github/virtuald/curvesapi/1.08/curvesapi-1.08.jar",
  "$m/org/apache/logging/log4j/log4j-api/2.20.0/log4j-api-2.20.0.jar"
)

foreach ($url in $jars) {
  $name = $url.Split('/')[-1]
  $dest = Join-Path "lib" $name
  if (Test-Path $dest) { Write-Host "Already have $name"; continue }
  Write-Host "Downloading $name ..."
  try { Invoke-WebRequest -Uri $url -OutFile $dest }
  catch { Write-Host "  FAILED: $name  ($($_.Exception.Message))" -ForegroundColor Red }
}
Write-Host "`nDone. Jars in lib: $((Get-ChildItem lib -Filter *.jar).Count)"
