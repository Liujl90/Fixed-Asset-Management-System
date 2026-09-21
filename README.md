# 企业固定资产全生命周期管理系统

第一版采用 Vue 3 + Spring Boot + MyBatis-Plus + MySQL 的前后端分离架构，实现固定资产核心业务闭环。

## 已实现

- Spring Security + JWT 登录与三类角色 RBAC。
- 部门、员工、资产分类和系统用户管理。
- 固定资产新增、编辑、删除、详情、分页与条件查询。
- 资产领用申请、审批、归还申请和归还确认。
- 在用资产跨部门、跨负责人调拨。
- Dashboard 统计、资产变动记录和 AOP 操作日志。
- Redis 缓存 Dashboard 和资产分类，写操作自动失效。
- Quartz 执行月度折旧和保养到期检查。
- EasyExcel 支持资产批量导入导出。
- 供应商、采购单、采购审核和入库生成资产。
- 维修记录、盘点任务、盘点差异和报废审批处置。
- Flyway 数据库迁移和开发环境 H2 配置。

## 项目结构

```text
server/   Spring Boot REST API
src/      Vue 3 + Element Plus 前端
```

## 开发运行

后端默认使用 H2 文件数据库，监听 `8081`：

```bash
cd server
mvn spring-boot:run
```

前端开发服务器会代理 `/api` 到 `http://127.0.0.1:8081`：

```bash
npm install
npm run dev
```

Swagger UI：

```text
http://127.0.0.1:8081/swagger-ui.html
```

## MySQL 运行

安装 Docker 后可使用 Compose 启动 MySQL 和后端：

```bash
docker compose up --build
```

也可以手动设置以下环境变量，使用 `prod` 配置运行：

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
CACHE_PROVIDER
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
REDIS_DATABASE
```

## 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `123456` |
| 资产管理员 | `manager` | `123456` |
| 普通员工 | `employee` | `123456` |

首次启动时，Flyway 会创建表并写入种子数据，密码初始化器会将演示密码转换为 BCrypt。

## 测试

```bash
npm test
npm run build

cd server
mvn test
```

后端集成测试使用 H2 MySQL 模式，通过 MockMvc 验证 JWT、403 权限、资产分页、Dashboard、领用归还和调拨。
