param([string]$MySqlBin, [string]$RunDir, [string]$ProjectDir, [int]$Port=13307)
$ErrorActionPreference='Stop'
$sql = Get-Content -Raw -LiteralPath "$ProjectDir/src/main/resources/schema/init-schema.sql"
# Reconstruct the immediately preceding structure from the audited fresh schema, without loading legacy passwords/data.
$sql = [regex]::Replace($sql, '(?m)^ALTER TABLE sys_user ADD COLUMN auth_version[^;]+;\r?\n', '')
$sql = [regex]::Replace($sql, '(?s)CREATE TABLE t_inspection_record \(.*?\) ENGINE[^;]+;', '')
$sql = [regex]::Replace($sql, '(?m)^ALTER TABLE t_harvest_record (ADD COLUMN active_record|ADD UNIQUE KEY uk_harvest_batch_pond_active|DROP INDEX batch_no)[^;]*;\r?\n', '')
$sql = [regex]::Replace($sql, '(?m)^ALTER TABLE t_(farm|pond) ADD COLUMN delete_batch[^;]+;\r?\n', '')
$sql = [regex]::Replace($sql, '(?s)ALTER TABLE sys_registration_application\s+ADD COLUMN pending_phone.*?;', '')
$sql = [regex]::Replace($sql, '(?s)CREATE TABLE `t_supplier_seedling` \(.*?\) ENGINE[^;]+;', '')
$sql = [regex]::Replace($sql, '(?m)^\s*`quarantine_(reviewer_id|reviewed_at)`[^\r\n]+\r?\n', '')
if ($sql.Contains('auth_version') -or $sql.Contains('CREATE TABLE t_inspection_record') -or $sql.Contains('ADD COLUMN pending_phone') -or $sql.Contains('CREATE TABLE `t_supplier_seedling`') -or $sql.Contains('quarantine_reviewed_at')) { throw 'Legacy fixture reconstruction failed' }
$sql += "`r`nALTER TABLE t_supplier ADD COLUMN user_id BIGINT NULL;`r`nALTER TABLE t_seedling_dict ADD COLUMN user_id BIGINT NULL;`r`n"
Set-Content -LiteralPath "$RunDir/legacy-schema.sql" -Value $sql -Encoding utf8
$clientArgs=@('--no-defaults','--host=127.0.0.1',"--port=$Port",'--user=root','--default-character-set=utf8mb4')
& "$MySqlBin/mysql.exe" @clientArgs --execute='CREATE DATABASE fams_migration_smoke CHARACTER SET utf8mb4'
if ($LASTEXITCODE -ne 0) { throw 'Migration fixture database creation failed' }
$source='source '+"$RunDir/legacy-schema.sql".Replace('\','/')
& "$MySqlBin/mysql.exe" @clientArgs fams_migration_smoke --execute=$source
if ($LASTEXITCODE -ne 0) { throw 'Legacy schema creation failed' }
& "$MySqlBin/mysql.exe" @clientArgs fams_migration_smoke --execute="INSERT INTO t_harvest_record(batch_no,pond_id,harvest_date,actual_total_weight_kg,buyer_name,operator_id,is_deleted) VALUES('MIGRATION-HISTORY',123,'2026-01-01',100,'Migration test',1,1)"
if ($LASTEXITCODE -ne 0) { throw 'History fixture creation failed' }
foreach ($migration in @('migration-20260906-auth-version.sql','migration-20260906-delete-batch.sql','migration-20260906-inspections.sql','migration-20260906-pond-harvest.sql','migration-20260907-pending-registration.sql','migration-20260908-supplier-seedlings.sql')) {
    Copy-Item -LiteralPath "$ProjectDir/src/main/resources/schema/$migration" -Destination "$RunDir/migration.sql"
    $source='source '+"$RunDir/migration.sql".Replace('\','/')
    & "$MySqlBin/mysql.exe" @clientArgs fams_migration_smoke --execute=$source
    if ($LASTEXITCODE -ne 0) { throw "Migration failed: $migration" }
}
& "$MySqlBin/mysql.exe" @clientArgs fams_migration_smoke --execute="INSERT INTO t_harvest_record(batch_no,pond_id,harvest_date,actual_total_weight_kg,buyer_name,operator_id,is_deleted) VALUES('MIGRATION-HISTORY',123,'2026-02-01',100,'Migration test',1,0),('MIGRATION-HISTORY',124,'2026-02-01',100,'Migration test',1,0)"
if ($LASTEXITCODE -ne 0) { throw 'Per-pond active records should coexist with soft-deleted history' }
$historyCount = & "$MySqlBin/mysql.exe" @clientArgs --batch --skip-column-names fams_migration_smoke --execute="SELECT COUNT(*) FROM t_harvest_record WHERE batch_no='MIGRATION-HISTORY'"
if ($LASTEXITCODE -ne 0 -or $historyCount -ne '3') { throw 'Migration must preserve soft-deleted history' }
Write-Output 'PASS six incremental MySQL migrations; supplier offerings enforced; soft-deleted history preserved; two active ponds allowed'
