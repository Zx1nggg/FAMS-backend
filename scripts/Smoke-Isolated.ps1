param(
    [string]$MySqlBin = 'E:/MySQL/bin',
    [string]$RedisBin = 'E:/Redis-x64-3.0.504'
)
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path $PSScriptRoot -Parent
$runDir = Join-Path $env:TEMP ('fams-smoke-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runDir | Out-Null
$started = @()
$savedEnv = @{}
function Set-TestEnv([string]$name, [string]$value) {
    $savedEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
function Assert-Code($result, [int]$expected, [string]$scenario) {
    if ($result.code -ne $expected) { throw "$scenario failed: expected $expected, got $($result.code)" }
    Write-Output "PASS $scenario ($expected)"
}
function Send-TestApi([string]$method, [string]$path, $payload) {
    $args = @{ Uri=("http://127.0.0.1:18080/api" + $path); Method=$method; WebSession=$webSession }
    if ($null -ne $payload) { $args.ContentType='application/json'; $args.Body=($payload | ConvertTo-Json -Depth 8) }
    $result = Invoke-RestMethod @args
    Assert-Code $result 200 "$method $path" | Out-Host
    return $result.data
}
try {
    foreach ($port in @(13307,16379,18080,15173)) {
        $occupied = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() |
            Where-Object Port -eq $port
        if ($occupied) { throw "Test port $port is already listening; no services will be started" }
    }
    # Every invocation uses its own empty database directory and loopback-only ports.
    & "$MySqlBin/mysqld.exe" --no-defaults --initialize-insecure "--basedir=$(Split-Path $MySqlBin -Parent)" "--datadir=$runDir/data" "--log-error=$runDir/mysql-init.log"
    if ($LASTEXITCODE -ne 0) { throw 'Isolated MySQL initialization failed' }
    $started += Start-Process "$MySqlBin/mysqld.exe" -ArgumentList @('--no-defaults','--standalone',"--basedir=$(Split-Path $MySqlBin -Parent)","--datadir=$runDir/data",'--port=13307','--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin',"--log-error=$runDir/mysql.log") -WindowStyle Hidden -PassThru
    $started += Start-Process "$RedisBin/redis-server.exe" -ArgumentList @('--port','16379','--bind','127.0.0.1','--appendonly','no','--dir',$runDir,'--logfile',"$runDir/redis.log") -WindowStyle Hidden -PassThru
    Start-Sleep -Seconds 4
    Copy-Item -LiteralPath "$projectDir/src/main/resources/schema/init-schema.sql" -Destination "$runDir/schema.sql"
    & "$MySqlBin/mysql.exe" --no-defaults --host=127.0.0.1 --port=13307 --user=root --execute='CREATE DATABASE fams_smoke CHARACTER SET utf8mb4'
    if ($LASTEXITCODE -ne 0) { throw 'Test database creation failed' }
    $source = 'source ' + "$runDir/schema.sql".Replace('\','/')
    & "$MySqlBin/mysql.exe" --no-defaults --host=127.0.0.1 --port=13307 --user=root --default-character-set=utf8mb4 fams_smoke --execute=$source
    if ($LASTEXITCODE -ne 0) { throw 'Schema import failed' }
    Write-Output 'PASS fresh MySQL schema import'
    & "$PSScriptRoot/Verify-Migrations.ps1" -MySqlBin $MySqlBin -RunDir $runDir -ProjectDir $projectDir
    Set-TestEnv 'JWT_SECRET' ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
    Set-TestEnv 'DB_URL' 'jdbc:mysql://127.0.0.1:13307/fams_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
    Set-TestEnv 'DB_USERNAME' 'root'
    Set-TestEnv 'DB_PASSWORD' ''
    Set-TestEnv 'REDIS_PORT' '16379'
    Set-TestEnv 'REDIS_HOST' '127.0.0.1'
    Set-TestEnv 'REDIS_PASSWORD' ''
    Set-TestEnv 'REDIS_DATABASE' '0'
    Set-TestEnv 'SERVER_PORT' '18080'
    Set-TestEnv 'SERVER_ADDRESS' '127.0.0.1'
    Set-TestEnv 'APP_BOOTSTRAP_ENABLED' 'true'
    Set-TestEnv 'APP_SCHEDULING_ENABLED' 'false'
    Set-TestEnv 'BOOTSTRAP_ADMIN_PHONE' '13900009999'
    Set-TestEnv 'BOOTSTRAP_ADMIN_PASSWORD' ([guid]::NewGuid().ToString('N'))
    $jar = Join-Path $projectDir 'target/FAMS-0.0.1-SNAPSHOT.jar'
    # ASCII temporary path also avoids native Windows program path encoding issues.
    Copy-Item -LiteralPath $jar -Destination "$runDir/app.jar"
    $started += Start-Process java -ArgumentList @('-jar',"$runDir/app.jar") -WorkingDirectory $runDir -WindowStyle Hidden -PassThru -RedirectStandardOutput "$runDir/backend.log" -RedirectStandardError "$runDir/backend-error.log"
    $ready = $false
    for ($attempt=0; $attempt -lt 60; $attempt++) {
        try { $health = Invoke-RestMethod 'http://127.0.0.1:18080/api/test/health' -TimeoutSec 2; $ready = $true; break } catch { Start-Sleep -Seconds 1 }
    }
    if (!$ready) { throw "Backend startup failed; inspect $runDir/backend.log" }
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/user/profile') 401 'anonymous access rejected'
    $login = @{ phone=$env:BOOTSTRAP_ADMIN_PHONE; password=$env:BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json
    $response = Invoke-RestMethod 'http://127.0.0.1:18080/api/auth/login' -Method Post -ContentType 'application/json' -Body $login -SessionVariable webSession
    Assert-Code $response 200 'bootstrap admin login'
    foreach ($path in @('/user/profile','/user/list','/base/farm/list','/base/pond/list','/base/seedling-dict/list','/base/supplier/list','/base/purchase-batch/list','/base/stocking/list','/base/sop-template/list','/lifecycle/harvest-record/list','/lifecycle/patrol-log/list','/lifecycle/batch-growth-log/list','/lifecycle/pond-task/list','/log/pond-feed-log/list','/log/alarm-record/list','/regulator/inspections/list','/regulator/inspections/stats','/regulator/dashboard/stats')) {
        Assert-Code (Invoke-RestMethod ("http://127.0.0.1:18080/api" + $path) -WebSession $webSession) 200 $path
    }
    $farm = Send-TestApi 'Post' '/base/farm' @{ farmName='Isolated smoke farm' }
    $pondA = Send-TestApi 'Post' '/base/pond' @{ farmId=$farm.id; pondName='A'; areaMu=2 }
    $pondB = Send-TestApi 'Post' '/base/pond' @{ farmId=$farm.id; pondName='B'; areaMu=3 }
    $seedling = Send-TestApi 'Post' '/base/seedling-dict' @{ categoryName='Smoke seedling'; growthCycleDays=100 }
    $supplier = Send-TestApi 'Post' '/base/supplier' @{ supplierName='Smoke supplier' }
    $template = Send-TestApi 'Post' '/base/sop-template' @{ categoryId=$seedling.id; stageName='start'; dayOffset=1; taskType='TEST'; taskDesc='smoke task' }
    $batch = Send-TestApi 'Post' '/base/purchase-batch' @{ farmId=$farm.id; supplierId=$supplier.id; seedlingId=$seedling.id; purchaseUnit='box'; unitQty=10; densityPerUnit=100; unitPrice=100; batchStatus=1; purchaseDate='2026-01-01' }
    $stockA = Send-TestApi 'Post' '/base/stocking' @{ batchId=$batch.id; pondId=$pondA.id; stockedUnits=4; stockingDate='2026-01-02' }
    $stockB = Send-TestApi 'Post' '/base/stocking' @{ batchId=$batch.id; pondId=$pondB.id; stockedUnits=6; stockingDate='2026-01-02' }
    $taskPage = Send-TestApi 'Get' ("/lifecycle/pond-task/list?batchNo=" + $batch.batchNo) $null
    if ($taskPage.total -ne 2) { throw 'Expected one SOP task for each pond' }
    $firstHarvest = Send-TestApi 'Post' '/lifecycle/harvest-record' @{ batchNo=$batch.batchNo; pondId=$pondA.id; harvestDate='2026-02-01'; buyerName='Smoke buyer'; actualTotalWeightKg=100; actualAvgWeightG=250; unitPrice=20; feedCost=0; medicineCost=0; otherCost=0; settlementStatus=1 }
    $afterFirst = Send-TestApi 'Get' ("/base/purchase-batch/" + $batch.id) $null
    if ($afterFirst.batchStatus -ne 2 -or $firstHarvest.seedlingCost -ne 400) { throw 'First pond must preserve growing batch and allocate cost 400' }
    $secondHarvest = Send-TestApi 'Post' '/lifecycle/harvest-record' @{ batchNo=$batch.batchNo; pondId=$pondB.id; harvestDate='2026-02-01'; buyerName='Smoke buyer'; actualTotalWeightKg=150; actualAvgWeightG=250; unitPrice=20; feedCost=0; medicineCost=0; otherCost=0; settlementStatus=1 }
    $afterSecond = Send-TestApi 'Get' ("/base/purchase-batch/" + $batch.id) $null
    if ($afterSecond.batchStatus -ne 3 -or $secondHarvest.seedlingCost -ne 600) { throw 'Last pond must close batch and allocate cost 600' }
    Write-Output 'PASS real MySQL multi-pond settlement and SOP generation'
    $inspection = Send-TestApi 'Post' '/regulator/inspections' @{ farmId=$farm.id; pondId=$pondA.id; inspectionDate='2026-02-01'; inspectionType='water_quality'; result='unqualified'; unqualifiedReason='Smoke check'; inspectionItem='Water sample' }
    foreach ($nextState in @('rectifying', 'rectified', 'accepted')) {
        $step = Send-TestApi 'Put' ("/regulator/inspections/" + $inspection.id + '/rectify') @{ rectifyStatus=$nextState; rectifyRemark='Smoke validation' }
    }
    $accepted = Send-TestApi 'Get' ("/regulator/inspections/" + $inspection.id) $null
    if ($accepted.rectifyStatus -ne 'accepted') { throw 'Inspection rectification did not reach accepted' }
    Write-Output 'PASS real MySQL inspection and rectification workflow'
    $applicationPassword = [guid]::NewGuid().ToString('N')
    $applicationPhone = '13900008888'
    $submitted = Send-TestApi 'Post' '/auth/register' @{ phone=$applicationPhone; password=$applicationPassword; username='Smoke applicant'; farmName='Applicant farm' }
    $applicationStatus = Send-TestApi 'Post' '/auth/registration-status' @{ phone=$applicationPhone; password=$applicationPassword }
    if ($applicationStatus.username -ne 'Smoke applicant' -or $applicationStatus.status -ne 0) { throw 'Verified application query failed' }
    $wrongPasswordBody = @{ phone=$applicationPhone; password=[guid]::NewGuid().ToString('N') } | ConvertTo-Json
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/auth/registration-status' -Method Post -ContentType 'application/json' -Body $wrongPasswordBody) 401 'application credentials rejected'
    Assert-Code (Invoke-RestMethod ("http://127.0.0.1:18080/api/auth/registration-status?phone=" + $applicationPhone)) 405 'phone-only query removed'
    $approval = Send-TestApi 'Put' ("/admin/registrations/" + $applicationStatus.id + '/approve') @{ status=1 }
    $farmerLogin = @{ phone=$applicationPhone; password=$applicationPassword } | ConvertTo-Json
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/auth/login' -Method Post -ContentType 'application/json' -Body $farmerLogin -SessionVariable farmerSession) 200 'approved farmer login'
    $profileBody = @{ realName='Smoke real name' } | ConvertTo-Json
    $farmerProfileResponse = Invoke-RestMethod 'http://127.0.0.1:18080/api/user/profile' -Method Put -ContentType 'application/json' -Body $profileBody -WebSession $farmerSession
    Assert-Code $farmerProfileResponse 200 'post-login real-name update'
    if ($farmerProfileResponse.data.realName -ne 'Smoke real name') { throw 'Post-login real-name update failed' }
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/user/profile' -WebSession $farmerSession) 200 'farmer profile'
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/user/list' -WebSession $farmerSession) 403 'farmer admin endpoint denied'
    Assert-Code (Invoke-RestMethod ("http://127.0.0.1:18080/api/base/pond/" + $pondA.id) -WebSession $farmerSession) 403 'farmer foreign pond denied'
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/auth/logout' -Method Post -WebSession $farmerSession) 200 'farmer logout'
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/auth/logout' -Method Post -WebSession $webSession) 200 'logout'
    Assert-Code (Invoke-RestMethod 'http://127.0.0.1:18080/api/user/profile' -WebSession $webSession) 401 'logged out access rejected'
    $frontendSmoke = Join-Path (Split-Path $projectDir -Parent) 'FAMS-Vue/scripts/smoke-dev.mjs'
    & node $frontendSmoke
    if ($LASTEXITCODE -ne 0) { throw 'Frontend development server smoke failed' }
    Write-Output "Isolated smoke completed; diagnostic logs: $runDir"
} finally {
    # Only processes started by this invocation; existing MySQL/Redis services are untouched.
    [array]::Reverse($started)
    foreach ($process in $started) {
        if (!$process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
    }
    # Native launchers can replace themselves with a child process. Match only this run's unique directory.
    Get-CimInstance Win32_Process -Filter "name='java.exe' OR name='mysqld.exe' OR name='redis-server.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine.Contains($runDir) } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -ErrorAction SilentlyContinue }
    foreach ($name in $savedEnv.Keys) { [Environment]::SetEnvironmentVariable($name, $savedEnv[$name], 'Process') }
}
