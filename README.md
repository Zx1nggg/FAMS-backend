# 智渔 / FAMS 后端

Java 21、Spring Boot 3.5.14、Spring Security、MyBatis-Plus、MySQL、Redis。前端位于同级 `FAMS-Vue`。
项目审计与未决事项见工作区根目录 `PROJECT_AUDIT.md`、`BLOCKERS.md`。以当前代码和这两份记录为准，历史 CLAUDE.md / 监管计划存在过时描述。

## 初始化与启动

1. 安装 Java 21、Maven、MySQL、Redis。实际验证环境为 Maven 3.9.11、MySQL 8.4.6；隔离冒烟使用本机 Redis 3.0.504，仅代表测试环境。
2. **新空库**执行 `src/main/resources/schema/init-schema.sql`。此脚本不删除表、不插入用户或业务演示数据，仅插入默认告警规则。不要向已有库重复导入。
3. Windows MySQL 原生客户端可能无法读取中文路径：复制 SQL 到 ASCII 临时路径，然后在 mysql 客户端执行 `CREATE DATABASE fams CHARACTER SET utf8mb4;`、`USE fams;`、`SOURCE C:/temp/fams-init.sql;`。账号及权限由本地数据库管理员配置。
4. 配置下表环境变量，再执行 `mvn spring-boot:run`，或 `mvn package` 后 `java -jar target/FAMS-0.0.1-SNAPSHOT.jar`。

| 配置 | 用途 / 默认值 |
|---|---|
| `JWT_SECRET` | 必填、至少 32 字节随机密钥；部署时从秘密管理或进程环境注入，不提交仓库 |
| `DB_URL` | 默认本机 3306/fams；完整 JDBC URL 可指定时区、连接选项 |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 / 密码；默认 root / 空，实际部署显式设置 |
| `REDIS_HOST` / `REDIS_PORT` | 默认 127.0.0.1 / 6379 |
| `REDIS_PASSWORD` / `REDIS_DATABASE` | 默认空 / 0；认证后的请求需要可用 Redis 黑名单服务 |
| `SERVER_PORT` | 默认 8080；上下文 `/api` |
| `APP_CORS_ALLOWED_ORIGINS` | 逗号分隔可信来源；默认 localhost:5173、127.0.0.1:5173 |
| `APP_COOKIE_SECURE` | HTTPS 部署设置 true；本机 HTTP 默认 false |
| `APP_SCHEDULING_ENABLED` | 默认 false；明确设 true 才开启 IoT 模拟和小时汇总任务，会生成模拟采集及告警数据 |
| `AVATAR_DIR` | 头像存储路径，默认 uploads/avatar |
| `APP_UPLOAD_INSPECTION_DIR` | 抽检附件路径，默认 uploads/inspection |
| `AI_ENABLED` | AI Agent 总开关，默认 false；完成数据库迁移并配置模型后再设 true |
| `AI_PROVIDER` | 当前支持 `openai-responses`，模型调用封装在独立适配层 |
| `AI_BASE_URL` / `AI_API_KEY` / `AI_MODEL` | Responses API 地址、服务端密钥和模型ID；密钥不得提交仓库或传给前端 |
| `AI_MAX_TOOL_ROUNDS` / `AI_MAX_OUTPUT_TOKENS` | 单轮最大工具循环（默认6）和最大输出Token（默认1600） |
| `AI_HISTORY_LIMIT` / `AI_TIMEOUT_SECONDS` | 服务端会话历史条数（默认16）和上游超时秒数（默认60） |
| `AI_PER_MINUTE_REQUEST_LIMIT` / `AI_DAILY_REQUEST_LIMIT` | 单用户每分钟/每日请求上限，默认10/200，计数存 Redis |
| `AI_STREAM_TIMEOUT_SECONDS` | 流式聊天超时秒数，默认120 |
| `AI_KNOWLEDGE_ENABLED` / `AI_VECTOR_STORE_ID` | 企业知识库开关和 OpenAI Vector Store ID；仅放置允许所有已登录角色查看的全局资料 |
| `AI_KNOWLEDGE_MAX_RESULTS` | 每轮知识库最多召回片段数，默认8，允许1-20 |

首次空库需要显式设置 `APP_BOOTSTRAP_ENABLED=true`、`BOOTSTRAP_ADMIN_PHONE`、`BOOTSTRAP_ADMIN_PASSWORD` 后启动，创建一个 ADMIN。已经存在管理员时不覆盖账号；后续关闭引导开关并移除引导密码环境变量。没有内置默认密码。其余农户通过入驻申请→管理员审批创建账号。

PowerShell 可用 `Read-Host -MaskInput` 读取密码后赋给相应进程环境变量，避免把密码写进脚本或历史命令；随机 JWT 密钥可用 `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))` 生成并直接赋值，不打印。生产部署应稳定保管 JWT 密钥，改变密钥将撤销已有会话。

## 已有库升级

先备份并核对实际表结构，在维护窗口仅执行尚未应用的增量。历史脚本包含 DROP/演示数据，不能将整个 schema 目录按文件名一键执行。

当前增量按以下顺序执行：

1. `migration-username-to-nickname.sql`：登录凭证改为手机号，昵称取消唯一约束（仅尚未执行过该历史迁移的数据库执行）。
2. `migration-20260906-auth-version.sql`：账号认证版本。
3. `migration-20260906-delete-batch.sql`：农场与池塘删除批次标记。
4. `migration-20260906-inspections.sql`：抽检档案。
5. `migration-20260906-pond-harvest.sql`：按批次+池塘约束有效出塘记录，保留软删除历史；需要已有出塘金额等历史字段。
6. `migration-20260907-pending-registration.sql`：待审申请手机号唯一；已拒绝申请允许重提并保留历史。
7. `migration-20260908-supplier-seedlings.sql`：将苗种与供应商改为监管维护的公共目录，删除两表旧 `user_id` 字段，并建立供应商与可供应苗种的核准关系。脚本不会猜测已有供应关系，升级后需由监管方为现有供应商配置品种。
8. `migration-20260908-ai-agent.sql`：新增 AI 会话归属、消息、Token 与工具调用审计表；不存储 API 密钥或工具原始结果。

除脚本内显式保护的部分外，增量只执行一次。预检查出现重复/缺关联记录时停止并核对，不删历史来凑约束。旧批次可能提前关闭、旧结算成本可能按全批次计算，脚本列出待核对记录但不自动重写历史状态与金额。旧删除池塘无批次标记，不随农场自动恢复。

## 业务约定与接口

- 认证用 HttpOnly `aqua_token` Cookie，兼容 Bearer。统一响应 `{code,message,data}`；业务错误可能仍为 HTTP 200，客户端必须检查 code。
- AI 同步接口为 `POST /api/ai/chat`；流式接口为 `POST /api/ai/chat/stream`，使用 SSE 事件 `status/tool_start/tool_end/delta/done/error`。流式 POST 前端需显式携带 Cookie 和农户的 `X-Current-Farm-Id`。
- AI 知识库当前只接全局可见 Vector Store。含角色、农场或个人敏感范围的文档不得上传到该库，后续应拆分检索器并在服务端执行元数据权限过滤。
- 登录字段为 phone/password；个人资料为 `/api/user/profile`；修改密码 `PUT /api/user/password` 需要 oldPassword/newPassword，成功后旧会话失效。
- 申请状态查询使用 `POST /api/auth/registration-status`，请求体为 phone/password，以最新申请的密码验证后返回资料与审批意见。旧 GET 查询已关闭；查询失败不清除当前登录会话。
- FARMER 的选中农场使用 `X-Current-Farm-Id`，后端按数据库实时归属校验；不能以请求 userId/farmId 覆盖身份。
- 苗种为监管维护的公共目录；供应商必须配置可供应品种，采购只能选择该供应商目录内的苗种。
- 采购状态：0 待检疫、1 入库、2 养殖、3 已出塘。采购登记固定进入待检疫；只有监管方可签发检疫证并推进到入库，投放及出塘服务维护 2/3 状态。
- 同批次每池塘仅一条有效出塘记录；全部已投放池塘出塘后关闭批次。收购方/去向必填。preview 必须传 batchId 和 pondId。
- 苗种成本按该池塘投放件数/采购总件数分配。投喂成本参考只汇总通过巡塘归属于该批次的记录；未关联批次的公共投喂不自动分摊，结算时人工核对。前端可录入实际成本，金额由后端计算。
- 抽检 `/api/regulator/inspections`，ADMIN/REGULATOR 管理；整改按 pending→rectifying→rectified→accepted 逐级推进，开始整改后原始档案锁定。
- SOP 模板作用于后续首次投放的池塘；修正首次投放日期同步平移未完成任务，已有完成任务或业务台账时阻止破坏历史的投放变更。

## 验证

```powershell
mvn test
mvn package
```

测试使用随机 JWT 密钥和隔离 H2 数据库，不依赖原有业务数据库。安全测试覆盖真实 Security 过滤链；集成测试覆盖行锁、并发、事务回滚、跨场、逐池结算与审批。原有破坏性 InitDataTest 保持禁用。

Windows 可运行 `./scripts/Smoke-Isolated.ps1 -MySqlBin <mysql的bin目录> -RedisBin <redis目录>`。先完成后端 package 和前端 npm ci。脚本创建全新临时目录和数据库，验证新库与增量迁移、随机管理员登录、业务接口、逐池结算、Vite 启动与代理，结束后停止本次进程；不使用现有 3306/6379 数据。需要 13307/16379/18080/15173 空闲。仅诊断日志留在临时目录，不向输出写密码/token。

## 项目预览
<img width="1851" height="842" alt="image" src="https://github.com/user-attachments/assets/a8082610-6eea-4987-aa5d-acce96463eae" />
养殖户登录界面
<img width="1877" height="838" alt="image" src="https://github.com/user-attachments/assets/df2d74be-5198-448c-ba3d-0b2d7506674d" />
养殖户操作界面
<img width="1858" height="847" alt="image" src="https://github.com/user-attachments/assets/70cb4db6-7217-4b89-8ba5-af55c8812c92" />
