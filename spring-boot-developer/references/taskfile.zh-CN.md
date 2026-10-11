# Taskfile

[Task](https://taskfile.dev/) 是跨平台任务运行器，可为 Spring Boot 仓库提供一组易发现的命名开发命令。适合项目需要简洁命令入口的场景；`Makefile`、Gradle task、Maven profile 或脚本也都是可选方案。每条命令只保留一个事实来源，不要把所有构建工具目标再包一层。

## 安装

开发者除项目要求的 JDK 和构建工具外，还需要安装 Task CLI。请按 [Task 官方安装说明](https://taskfile.dev/installation/)选择安装方式。在仓库根目录执行 `task --list` 应能正常工作。

## 设计约定

- 在构建文件旁放置根目录 `Taskfile.yml`，并为公开任务设置简洁的 `desc`，让 `task --list` 成为命令目录。
- 任务应保持精简：编译、测试、打包和格式化交给 Maven 或 Gradle，不要在 Shell 命令中重复实现构建逻辑。
- 使用项目 Wrapper（`./mvnw` 或 `./gradlew`）锁定构建工具版本。支持 Windows 时应处理 `mvnw.cmd` / `gradlew.bat`。
- 一个任务只承担一个清晰职责。默认任务应列出可用任务，或执行安全且符合预期的操作。
- 必须先完成的操作可通过 task 调用表达。依赖任务会在当前任务主体之前执行；互相独立的依赖可能并行运行，不适合表达有顺序要求或共享可变状态的操作。
- 本地基础设施生命周期要明确。`compose:up` 可启动依赖；`compose:stop` 应停止服务但保留卷。删除数据应使用独立且名称醒目的任务，并增加确认或其他保护。
- 不要把凭据放入 Taskfile 变量或提交 dotenv 文件。让工具从环境变量或被忽略的本地文件读取密钥。
- 优先使用 Task 的平台命令支持，不要假设所有开发机都有 POSIX 的 `sleep`、`sed`、`rm` 等工具。
- Task 应保持可选。除非仓库明确将 Task 作为前置要求，CI 和生产部署仍应能直接调用构建工具。

## Maven 示例

```yaml
version: '3'

vars:
  MVNW: '{{if eq OS "windows"}}mvnw.cmd{{else}}./mvnw{{end}}'

tasks:
  default:
    desc: 列出可用任务
    cmds:
      - task --list

  test:
    desc: 运行测试套件
    cmds:
      - '{{.MVNW}} test'

  verify:
    desc: 执行验证并检查打包
    cmds:
      - '{{.MVNW}} verify'

  run:
    desc: 使用本地 Profile 运行应用
    cmds:
      - '{{.MVNW}} spring-boot:run -Dspring-boot.run.profiles=local'

  compose:up:
    desc: 启动本地依赖服务
    cmds:
      - docker compose up -d

  compose:stop:
    desc: 停止本地依赖并保留数据
    cmds:
      - docker compose stop
```

Gradle 项目可将 Wrapper 变量改为 `{{if eq OS "windows"}}gradlew.bat{{else}}./gradlew{{end}}`，并使用仓库已有任务，例如 `test`、`check` 和 `bootRun`。本地 Profile 参数也应按项目配置调整。若启用了 Spring Boot Docker Compose 支持，应说明它可能自动启动服务，并保留显式 Compose 任务作为手动备用方式（参见[本地开发](local-development.zh-CN.md)）。

## 验证 Taskfile

新增或修改 Taskfile 后运行 `task --list`，确认文件可加载且任务可发现；再运行相关任务（例如 `task verify`）。根据所用功能检查 Taskfile Schema 版本要求的最低 Task CLI 版本；Schema 版本不等于 Task CLI 发布版本。

## 常见问题

- 假设 Task 已经安装——应记录前置条件，并保留直接调用 Wrapper 的方式。
- 每项任务都执行 `clean`、格式化、测试和打包——让任务可组合，避免意外副作用。
- 将删除 Compose 卷放入普通清理流程——默认保留开发数据，将破坏性清理做成显式操作。
- 用任务依赖表达执行顺序——依赖可能并行运行；顺序操作应通过 `cmds` 中的任务调用表达。
- 认为 Task 变量会自动成为进程环境变量——命令需要导出值时使用 `env:`。
- 将密钥放进已提交的 `.env` 或 Taskfile——从环境变量或被忽略的本地配置读取。
