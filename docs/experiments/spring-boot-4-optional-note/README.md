# Spring Boot 4 技能对照：可选备注

2026 年 11 月 24 日，两组代理给同一个文档接口增加可选备注。基线是带会话登录、CSRF 和资源归属的 Spring Boot 4.2.0 内存服务。两组使用同一模型和同一任务，各自实施一次。启用组可以读取这次归档的技能。共同验收在实现之后运行。 [任务原文](task.md) · [基线与技能快照](inputs.tar.gz)

## 任务

`PATCH /api/documents/{id}` 增加可选 `note`。`title` 仍然必填。请求省略 `note` 时已保存的备注不变；文档还没有备注时，读取和成功响应保持原来只有 `title` 的 JSON。JSON `null` 清空备注，成功响应也不包含 `note`。空字符串保存为 `""`。40 个字符成功，41 个字符拒绝且文档不变。缺少 `title` 时拒绝且文档不变。登录、归属、CSRF 和偏好设置接口保持原样。

## 共同验收

验收脚本用候选组的 Maven Wrapper 构建应用，在随机端口启动 JAR，再用真实 HTTP 检查上述规则，以及拒绝后的文档、他人文档、缺少 CSRF、匿名请求和偏好设置。基线在保存备注这一步按预期失败，登录和偏好设置通过。 [验收脚本](verify.sh) · [HTTP 测试](acceptance.py) · [基线日志](logs/base-acceptance.log)

只改 `title` 且文档已经有备注时，任务要求备注保持不变，没有规定这次成功响应必须带回 `note`。验收因此接受两种成功响应：只返回新的 `title`，或者同时返回未改动的 `note`。随后的读取都必须仍包含这条备注。

## 对照结果

| 检查 | 启用技能 | 未启用技能 |
| --- | --- | --- |
| 首次测试里的省略、`null`、空字符串 | 三项都有独立用例 | 三项都有独立用例 |
| 长度边界 | 39、40 成功，41 拒绝 | 40 成功，41 拒绝 |
| 首次 `./mvnw test` | 编译失败。`var` 接在返回 `void` 的登录方法上 | 13 项里 2 项错误。400 和 404 的响应体为空，按 JSON 取字段失败 |
| 最终 Maven 测试 | 8 项通过 | 13 项通过 |
| 共同 HTTP 验收 | 通过 | 通过 |
| 生产代码 | 只改 Controller | 只改 Controller |

启用组用请求对象的 setter 是否被调用区分字段是否出现，并用 `@JsonInclude(NON_NULL)` 省略空备注。成功响应返回保存后的文档。未启用组把正文绑定成 `Map`，用 `containsKey("note")` 区分省略和 JSON `null`。只改 `title` 时成功响应仍只有 `title`，读取会返回已保存的备注。 [启用组改动](enabled.patch) · [未启用组改动](disabled.patch)

启用组的最终测试只把那一行 `var` 拆开。未启用组的最终测试不再解析空响应体，改为比较状态码相同的既有 404 的响应体和响应头。

## 这次能说明什么

任务已经写明省略、`null` 和空字符串的差别，两组的第一版测试都覆盖了这三项，最终实现也都通过共同验收。技能多带来的可见差别是长度边界的另一侧：启用组测了 39 和 40，未启用组测了 40。启用组还断言了错误 JSON 的固定字段。每组只运行一次。这次没有发现需要改技能的新缺口。

## 复跑

准备 Java 21、Python 3、Maven 依赖缓存或仓库访问，以及 `rsync` 和 `patch`。在本报告目录执行：

```sh
work=$(mktemp -d)
tar -xzf inputs.tar.gz -C "$work"

for variant in enabled disabled; do
	cp -R "$work/base" "$work/$variant"
	patch -s -d "$work/$variant" -p1 < "$variant.patch"
	sh verify.sh "$work/$variant"
done
```
