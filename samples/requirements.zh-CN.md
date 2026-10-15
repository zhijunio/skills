# Auth Platform：产品与工程需求

**状态：** 实施规划草案
**目的：** 定义基于 Spring Boot 4.1.1 和 Spring Security 7 的可复用身份平台需求，明确框架能力与应用职责边界。本文件是独立需求说明，规定产品行为及必须遵守的框架集成约束；不限定数据库、ORM、缓存或代码架构，也不构成合规声明。

## 1. 产品目标

提供可复用的身份与认证服务，通过已文档化的 HTTP API 支持本地账户、联合登录、多因素认证、会话和管理控制。开发者应能在本地运行服务、配置所选存储和集成、执行验证套件，并在不修改源代码的情况下提供生产环境密钥。

### 框架职责边界

Spring Boot 提供依赖管理、自动配置、配置绑定、健康状态与指标集成以及应用运行环境。Spring Security 提供认证、授权、OAuth2/OIDC Client 与 Resource Server 机制、WebAuthn 支持和 MFA 授权原语。两个框架都不会替应用实现完整的账户生命周期、因子注册、账户恢复、Provider 绑定、反滥用策略或产品专属 API 契约。

Spring Security 7 支持 `FactorGrantedAuthority`、`@EnableMultiFactorAuthentication`、编程式因子规则、WebAuthn 和 One-Time Token 登录。这些机制可用于约束必须通过的因子组合；应用仍负责因子注册与验证。One-Time Token（OTT）是通过邮件等方式发送的登录令牌，不是 TOTP。TOTP、恢复码、账户级因子策略以及 REST API 的分阶段令牌签发仍由应用负责。参见 [Spring Security MFA 文档](https://docs.spring.io/spring-security/reference/servlet/authentication/mfa.html)。

OAuth2 Client 有不同用途：OAuth2/OIDC 登录、代表用户访问 Provider API，以及服务间 Client Credentials。OAuth2 Login 是基于 OAuth2 Client 实现的授权码登录功能。适用时应使用 Spring Security 的协议处理和 `OAuth2AuthorizedClientManager`，不要自行实现协议重定向、state、回调或令牌刷新。参见 [Spring Security OAuth2 文档](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html) 和 [OAuth2 Login 文档](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)。

## 2. 产品决策与假设

1. **身份服务职责：** 服务认证自身用户，并接受选定的外部身份 Provider。首版不作为 OAuth2 Authorization Server。如果产品需要向第三方客户端签发标准 OAuth2/OIDC 令牌，应作为独立范围评估 Spring Authorization Server。
2. **API 风格：** 首版采用 JSON REST API，路径使用不带版本号的 `/api/...`。引入破坏性变更前，需单独确定并记录 API 版本兼容策略。
3. **客户端支持：** 本服务是前后端分离架构中的后端，只提供 JSON API，不渲染页面，也不提供 BFF 或基于服务端会话 Cookie 的登录。首版客户端为独立部署的 Web 前端（SPA）和原生/移动端 App，两者都通过 `Authorization: Bearer` 携带本平台签发的访问令牌；令牌交付与存储见 4.4。API 不使用 Cookie，也不创建 `HttpSession`。客户端不得持有 Provider access token 或 client secret。
4. **存储与运行环境：** 在实施规划阶段选择存储、缓存、迁移和共享状态技术。所选方案必须满足本文规定的持久性、原子性、过期、可用性和安全要求。
5. **基础令牌能力：** JWT 是平台的基础能力，使用非对称签名签发和验证自身访问 JWT，并发布对应公钥 JWKS；具体要求见 4.4。
6. **可选功能开关：** MFA 与 OAuth2 Client 可由部署配置决定是否提供。若部署提供 MFA 管理能力，管理员可通过受保护的管理 API 配置并运行时启用/停用 MFA；若部署未提供 MFA 能力，则不暴露因子 API 或 MFA 管理 API。OAuth2 Client 按部署配置启停。敏感操作可配置为要求近期 MFA；只有 MFA 运行时启用后该策略才可生效。启动或运行时停用 MFA 前，必须确认既不存在强制 MFA 的操作/用户策略，也不存在用户已激活的因子；否则拒绝启动或拒绝变更（运行时返回 `409`），不能静默降级认证要求，也不能让已自愿注册因子的用户退化为仅密码登录。如确需停用，须先经受审计的数据处理流程处置已激活因子并通知受影响用户。本条是 MFA 停用保护的唯一规范定义，其他章节引用本条。
7. **MFA 能力：** 启用 MFA 后支持 TOTP、WebAuthn/passkey、钱包签名挑战和单次使用的恢复码。邮件/短信 OTP 可用于验证或恢复，但不得自动视为独立的高保证因子。邮箱同时是登录标识和密码重置渠道，控制邮箱即可同时重置密码并通过邮件 OTP，因此默认不得以“密码 + `EMAIL_OTP`”组合满足 MFA；例外须在因子策略中明确记录其保证等级。
8. **OAuth2 Client 能力：** 启用 OAuth2 Client 后支持 OIDC Provider 登录，Google 为首个配置 Provider。增加 Provider 时应复用协议处理，避免重复实现协议逻辑。
9. **首版不自建 Authorization Server：** OAuth2 Client 登录不等同于签发 OAuth2 授权。Authorization Server 功能需单独进行产品和安全评审。
10. **单租户：** 首版不支持多租户，数据模型和 API 不包含租户维度。
11. **注册邮箱验证：** 注册后必须先完成邮箱验证才能登录，流程见 4.1。

## 3. 用户角色

- **未认证访客：** 注册、登录、发起密码恢复或开始已启用的外部登录。
- **已认证用户：** 管理个人资料、已验证联系方式、会话、MFA 因子、恢复码和已绑定外部身份。
- **管理员：** 搜索和管理账户、查看安全审计记录、执行范围受限的管理操作。
- **运维人员：** 配置 Provider 凭据、邮件/短信发送、存储与共享状态服务、日志、健康检查和部署策略；默认不拥有面向用户的管理员权限。

## 4. 功能需求

### 4.1 账户与凭据

- 首版使用邮箱和密码注册、登录。邮箱按明确的规范化规则处理，登录标识符唯一性由存储层原子保证。若将来支持手机号和密码登录，新增独立的 `/api/auth/phone/login` 接口，请求使用 `phoneNumber` 字段；不得增加 `identifierType`，也不得改变现有邮箱登录接口。
- 所有用户名/密码认证必须进入 Spring Security `SecurityFilterChain`，经认证过滤器、`AuthenticationManager` 和对应 `AuthenticationProvider` 完成。JSON 请求使用 Spring Security 的 `AuthenticationConverter` 转换认证请求，登录成功/失败由 `AuthenticationSuccessHandler` 和 `AuthenticationFailureHandler` 处理。不得创建 Controller 或 `@RestController` 自定义登录接口，不得由 Controller 直接调用认证服务来建立登录认证。登录以外需要校验密码的场景（修改密码时校验当前密码、重新认证）须复用同一 `AuthenticationProvider` 与 `PasswordEncoder`，同样计入失败计数和限流、对齐响应耗时，并拒绝已锁定或停用的账户。成功处理器只能按 MFA 策略创建待 MFA 状态或完成认证；MFA 未完成前不得建立完整登录会话或签发完整令牌。
- 注册后账户处于待验证状态，完成邮箱验证前不能登录。注册时向该邮箱发送短时效、单次有效的验证凭据；确认验证时须同时提交注册时设置的密码，防止攻击者用他人邮箱抢先注册后由邮箱持有人误点链接激活攻击者的密码。对仍处于待验证状态的邮箱再次注册时，替换待验证的密码并使旧验证凭据失效，按邮箱限频。超过期限未验证的待验证账户到期清除并释放邮箱。验证成功后不自动登录。
- 对用户不存在、密码错误、账户未启用、邮箱未验证或被锁定，过滤器失败处理器返回相同的通用 `401` 响应，避免账户枚举；锁定原因仅用于内部安全事件和运营处置。用户不存在时仍须执行一次等成本的密码散列校验，使响应耗时无法区分账户是否存在。认证成功和失败通过 Spring Security 认证事件机制接入认证事件记录，不在 Controller 中记录登录结果。
- 密码长度按 Unicode 码点计数，至少 15 个字符；最大可接受长度不得低于 64 个字符，并设置有文档的上限以防超长输入拖慢散列。完整校验和散列密码，不得静默截断；所选 `PasswordEncoder` 的输入限制不得低于最大可接受长度的 UTF-8 字节数。BCrypt 只处理 72 字节，不满足该要求，应选用 Argon2id、PBKDF2 或有文档的预散列方案。接受空格和 Unicode，并按 NFKC 规范化；规范化在注册、修改、重置和登录时一致执行。引入规范化前已存储的密码散列须有兼容策略（例如规范化值校验失败时以原始输入重试一次，成功后按规范化值重新散列）。不得强制字符类别组合或无依据的定期改密；发现泄露或用户主动请求时应允许换密。密码策略应符合目标部署采用的安全标准并在注册、修改、重置时一致执行。
- 在注册、密码修改和密码重置时，将完整新密码与包含常见、预期及已泄露密码的弱密码阻止列表比对；命中时拒绝并向用户说明需另选密码。允许选用维护活跃、许可兼容的开源库评估密码强度、检测常见/泄露密码；不得为了检查向第三方发送明文密码。不得自行编写脆弱的规则/正则集合代替成熟检查器。
- 密码只使用 Spring Security `PasswordEncoder` 支持的自适应单向散列保存，并保留算法升级/迁移能力；登录时只执行规范化，不得重复执行长度、阻止列表等注册密码策略，以免历史密码无法认证。
- 对连续登录失败分别按 IP 和规范化标识符限流；标识符维度无论账户是否存在都计数，使 `429` 不泄露账户存在性。账户级认证锁定在可配置阈值触发且有期限；单个认证器连续失败上限不得超过 100 次，可按风险设定更低阈值。由于任何知道邮箱的人都能累积账户维度失败次数，账户锁定阈值须高于单 IP 限流阈值，以提高单一来源锁定账户的门槛；该措施无法阻止多 IP 分布式攻击，剩余拒绝服务风险由锁定有期限和用户自助解锁兜底。成功认证后清除失败计数；锁定期限、跨实例原子性和解锁流程必须明确，避免永久锁定。对外失败响应与普通无效凭据相同，因此触发锁定时须向已验证联系方式发送通知并附解锁入口；锁定通知按账户限频。密码重置成功后清除相应的密码失败锁定并撤销既有会话；用户也可通过已验证邮箱发起限流解锁，管理员可通过受审计的管理操作解锁。
- 对成功登录进行风险评估；若来自明显新地点或新设备，向已验证联系方式发送安全通知。允许选用维护活跃、许可兼容的开源库解析 User-Agent，并在必要时使用粗略 IP 地理定位；User-Agent 和地理信息只作为可伪造、近似的风险信号，不作为身份凭据。不得因 VPN、旅行或单个 IP 变化自动拒绝登录；通知仅包含时间、粗略地点/设备摘要和账户安全入口，不包含凭据。日志/认证事件保存解析后的最少必要摘要，不保存完整 User-Agent 或精确位置；用户认证历史提供相同的安全摘要。
- 使用有时效、单次有效的验证码验证邮箱和手机号；新验证码替代旧验证码，并限制重发、目标地址、账户和 IP。
- 修改密码前要求近期认证；校验当前密码或经验证的 step-up 因子。没有本地密码的账户（例如通过外部登录开通）可设置首个密码，须先通过近期认证（重新完成外部登录或 MFA step-up）。
- 提供通用密码重置发起接口和单次有效的重置完成接口；重置成功后撤销现有会话。
- 账户状态变更和软删除仅由具备相应权限的管理员或受控的数据生命周期流程执行；用户可申请个人数据导出，但不得通过自助 API 停用或删除自己的用户。

### 4.2 MFA、钱包、恢复码与 Passkey

- 允许已认证用户注册 TOTP：生成密钥、展示配置 URI，并要求正确验证码后才激活。
- TOTP 密钥需使用受管理且可轮换的密钥加密存储。注册完成后，常规 API 响应和日志不得再包含该密钥。
- 启用 MFA 和重新生成时签发哈希保存、单次使用的恢复码；明文只在一次性响应中显示。该不变式适用于所有因子类型：首个因子激活时，若响应可返回恢复码（如 TOTP、钱包），应在激活响应中同时创建；经框架端点注册的 Passkey 无法在协议响应中返回恢复码时，客户端须随即调用 `POST /api/users/me/mfa/recovery-codes`。恢复码签发前，该因子不计入登录必需因子，因子列表须将其标记为“待签发恢复码”。
- 记录因子注册、移除、替换、恢复码重置和恢复使用事件；通过已验证渠道通知用户安全敏感的因子变更。MFA 启用时，可按操作配置 step-up 要求；策略要求近期 MFA 的操作必须验证对应因子后才能继续。
- TOTP 使用有界时间窗口验证，并拒绝重复使用已接受的时间步。
- 在适合所选认证架构时使用 Spring Security 7 因子权限及选择性 MFA 授权规则。只有完成对应因子验证后才能授予该因子的权限标记。
- 使用受支持的协议实现提供 WebAuthn/passkey 注册与断言流程。凭据必须在服务重启后保留；RP ID 与允许的 origin 由部署配置约束；当保证策略依赖用户验证时，必须要求用户验证。
- 支持钱包持有证明因子：使用新生成、单次有效的签名挑战。挑战需绑定依赖方域名、目标操作、账户/会话、过期时间、nonce 和配置的网络/链；验证签名及钱包地址并防止重放。EVM 链的消息格式采用 EIP-4361（Sign-In with Ethereum），其他链采用其等效标准；若支持智能合约钱包，须按 EIP-1271 校验，不能只依赖从签名恢复地址。绝不索取或接收钱包助记词。产品和部署配置必须明确列出支持的钱包协议与链。
- 定义因子丢失后的恢复及支持流程。不得仅凭邮件或短信访问权悄然绕过更强因子。
- 明确因子生命周期 API：列出已注册因子但不返回秘密、开始注册、确认注册、验证待处理因子，以及近期认证后禁用或替换因子。完成持有证明前因子不得激活。
- 主凭据验证后使用短时效的待 MFA 状态。该状态只允许因子发现、挑战、验证和取消操作；不得将其表示为完整认证会话，也不得签发完整权限令牌。所需因子和允许组合由服务端决定，客户端不能通过切换因子类型降低或跳过策略。
- 对敏感用户安全操作可配置近期 MFA 要求。管理员启用 MFA 且某操作被策略标记为必需 MFA 时，用户必须先完成 MFA；停用保护见第 2 节第 6 条。对用户强制 MFA 的策略必须配置注册宽限期：宽限期内，尚无活动因子的用户完成主凭据验证后只获得受限会话，仅允许注册首个因子和签发恢复码，不得访问其他资源或签发完整令牌；宽限期结束后仍未注册的用户须经支持恢复流程处理，管理员在该流程中清除用户因子时为其重新开启宽限期。受限会话的下发方式：登录、`token/exchange` 和 MFA 验证接口返回 `202` 及错误码 `MFA_ENROLLMENT_REQUIRED`；客户端获得用途标记为仅限注册因子的受限访问令牌，只被 5.7 列出的接口接受，不签发 refresh token。注册首个因子后，客户端须重新登录以获得完整会话。MFA 关闭时不能将强制 MFA 视为已满足，也不能允许绕过检查。普通资料读取不得静默延长 MFA 时间窗口。
- 仅为已配置因子提供挑战发送和验证接口。按账户、目标地址和 IP 限流，并以原子方式消费成功挑战。

### 4.3 OAuth2/OIDC Client 与外部身份

- 使用 Spring Security OAuth2 Login 执行授权码登录。Provider 注册属性放在外部配置中；client secret 不得提交到代码仓库。
- 使用稳定的 `(issuer, subject)` 作为 OIDC 身份键。邮箱是资料字段，不能单独用于自动合并账户。
- 使用 Spring Security 的协议处理验证 Provider 回调的 state、nonce、issuer、签名、audience、过期时间、redirect URI 和预期注册信息。
- 外部登录只完成主凭据验证，之后与密码登录一样按 MFA 策略处理：用户有活动因子或被强制 MFA 时，回调签发的一次性兑换码在 `POST /api/auth/token/exchange` 时返回待 MFA 响应而非令牌。默认不信任 Provider 声明的 MFA；仅当策略显式信任某 Provider 且其返回的 `amr` 表明已完成 MFA 时，才可视为满足 MFA，并在认证日志中记录。
- 仅按已记录的开通策略，在首次登录时创建本地账户。绑定外部身份到已有账户前，用户必须先完成本地认证和近期认证检查。
- 支持列出、绑定和解绑身份。除非用户先添加其他凭据，否则不能解绑最后一种可用登录方式。可用登录方式指用户当前能独立完成登录的凭据：已设置且所属邮箱已验证的本地密码，或所属 Provider 已启用的外部身份；临时锁定不影响其计数。
- Provider token 保存在服务端。OAuth 回调后签发短时效、单次有效的不透明兑换码；只保存兑换码校验值并以原子方式消费。兑换码须绑定发起登录的客户端：客户端发起登录时生成随机 `code_verifier`，只提交其 S256 摘要 `code_challenge`，兑换时提交 `code_verifier` 由服务端核对，防止攻击者把自己账户的回调或兑换码交给受害者完成登录（登录 CSRF），也防止 App Link 被拦截后兑换码被冒用。
- API 无状态，框架默认依赖 `HttpSession` 的 OAuth2 Login 组件须定制：授权请求存放在共享状态中，以 `state` 为键、短时效、单次消费（自定义 `AuthorizationRequestRepository`）；自定义 `OAuth2AuthorizationRequestResolver` 接收 `continue` 和 `code_challenge` 参数，以及绑定或重新认证事务的标识；成功处理器只签发兑换码或推进对应事务，不建立会话。`continue` 只接受部署配置登记的跳回目标 ID（例如 `web`、`ios`、`android`），不接受任意 URL。
- 当委托访问 Provider API 或需要保持 refresh token 连续性时，安全保留 authorized-client 状态。使用 `OAuth2AuthorizedClientManager` 处理支持的授权授予和令牌刷新，不自行实现刷新协议。
- 仅在确有命名集成需求时支持服务间 Client Credentials。限制 scope 和外连目标，不提供通用令牌签发接口。
- 注销时清除本地会话；如有配置，可发起 Provider OIDC logout。明确本地注销不一定会结束 Provider 会话。
- 提供公开且精简的已启用登录 Provider 列表供登录页面展示；不得包含密钥、私有端点信息或未经筛选的配置。
- 决定上游 Provider 注册信息由部署配置管理还是由管理员动态管理。若支持动态管理，需提供受限的创建/更新/禁用/删除操作、密钥只写语义、变更审计和配置校验。运行时配置端点必须使用严格 HTTPS/主机白名单及网络出站控制以防 SSRF；连通性检查不能证明登录配置有效。
- Provider 模板只能作为可选安全默认值。模板不代表凭据或端点已验证；Provider 配置仍需依据其当前文档审核。

### 4.4 会话与访问令牌

- JWT 是平台基础能力，不受 MFA 或 OAuth2 Client 开关控制。平台使用非对称私钥签发短时效访问 JWT，并作为 Resource Server 按固定算法允许列表校验签名、公钥版本、issuer、audience、过期时间、令牌用途和撤销状态；拒绝对称签名算法、`none` 和令牌自行指定的未允许算法。私钥须由密钥管理器或等效安全设施保护，JWT 头部的 `kid` 只用于在受信任的密钥集合中选择密钥；`/.well-known/jwks.json` 只发布对应公钥。访问 JWT 包含 `sid` 声明，关联服务端会话记录；撤销检查、近期认证、近期 MFA 和当前会话识别都以 `sid` 查找该记录。
- 显示用户活动会话的创建时间、最近使用时间、设备标签，以及适用时经过掩码处理的 IP/位置数据。
- 支持撤销单个或所有会话。刷新凭据每次使用时轮换；重用已撤销凭据时撤销整个凭据族。为并发刷新定义有界的短暂宽限窗口：窗口内同一旧凭据的重复提交不触发凭据族撤销，窗口外的重用才视为凭据泄露。若窗口内需返回同一轮换结果，该结果只能在窗口期内加密保存，不得保存明文。
- 刷新时按当前账户状态、角色和 MFA 策略重新评估会话：账户已停用时拒绝刷新；会话的认证上下文不再满足策略强制的必需因子（例如管理员启用强制 MFA）时拒绝刷新并要求重新登录；角色从当前数据重新读取。账户停用会撤销全部会话并立即生效；不撤销会话的角色变更最长滞后访问 JWT 的有效期，须在文档中写明。
- 用户自愿注册首个因子时，完成注册的当前会话视为已完成 MFA 并更新最近 MFA 时间；其他活动会话不因此被拒绝刷新，而是通过已验证渠道通知用户，并在通知和会话列表中提供一键撤销其他会话的入口。
- **近期认证** 以服务端会话记录的最近认证时间为准，初始值为登录时间，窗口时长可配置。可通过 `POST /api/users/me/reauthentication` 刷新：有本地密码的用户提交密码；只有外部身份的用户提交已绑定身份的 `registrationId`，经 OIDC 重新认证（`prompt=login` 或 `max_age`，并校验返回的 `auth_time` 与原绑定的 `(issuer, subject)`）。重新认证回调只更新当前会话的认证时间，不建立新会话。
- **近期 MFA** 以服务端会话记录的最近 MFA 时间为准，登录时完成的 MFA 和 `/api/users/me/mfa/step-up` 验证都会更新该时间；重新验证密码不满足近期 MFA。绑定具体操作的一次性 step-up 授权只用于策略明确标记的高风险操作。用户未注册任何活动因子时，要求近期 MFA 的操作返回 `403` 及机器可读错误码 `MFA_FACTOR_REQUIRED`，提示先注册因子；该错误码与登录时下发受限会话的 `MFA_ENROLLMENT_REQUIRED` 区分。
- 对 bearer token 客户端，仅在所有必需因子通过后签发短时效访问令牌。
- 在访问凭据中包含可信认证上下文（`amr` 或等效信息）和认证时间。不得依据客户端请求字段或请求头判断保证等级。`auth_time` 只表示登录时间，重新认证和 step-up 不会更新已签发的令牌；下游服务不得用它判断本平台定义的近期认证或近期 MFA。
- 定义 refresh token 的保存、轮换、撤销、缓存指令和密钥轮换行为。所有客户端的 refresh token 都在令牌响应体中返回，刷新时在请求体中提交，不使用 Cookie 交付。客户端可通过 `POST /api/auth/token/revocation` 用 refresh token 注销对应会话，访问令牌过期时也能注销。CORS 只允许已配置的前端 origin，不允许携带凭据。
- 原生 App 的外部登录使用系统浏览器完成授权（RFC 8252），不得使用内嵌 WebView，回调后经已声明的 App Link/Universal Link 把一次性兑换码交给 App，再调用 `/api/auth/token/exchange`。
- **客户端接入约定**（写入 API 文档，后端无法强制）：访问令牌、待 MFA 事务句柄和受限访问令牌只保存在内存中。Web 前端的 refresh token 存储方式由前端项目决定，前端应部署严格的内容安全策略（CSP）以降低 XSS 风险；原生 App 将 refresh token 保存在系统安全存储（iOS Keychain、Android Keystore）中。

### 4.5 管理与审计

- 管理接口需配置明确角色；适用时要求近期 MFA。初始管理员由部署配置一次性引导：系统中不存在有效管理员时，在启动时或配置指定的账户完成邮箱所有权验证时，为该账户授予内置引导角色并记录审计；不得为邮箱未验证的账户授予，以防攻击者抢先用配置邮箱注册。存在有效管理员后该配置不再生效。此后角色只能通过受审计的 `PUT /api/users/{userId}/roles` 授予或撤销，管理员不得修改自己的角色。具备 MFA 配置权限的管理员可配置并启用/停用 MFA，所有变更写入操作审计；停用操作需通过当前生效的强认证策略。
- 认证事件和操作审计事件在 schema 和存储集合上分开。认证事件需能记录未知/未解析身份的失败尝试，因此用户标识可缺省。操作审计事件描述用户、管理员或系统执行的操作，并在适用时记录操作者和目标信息。
- 为认证历史和操作审计分别提供分页、可筛选的查询 API。
- 为用户提供本人账户相关操作审计记录的自助查询视图，范围限于本人作为操作者或受影响用户的记录。仅返回安全投影，不得泄露其他用户数据、特权调查详情或敏感值；该视图与认证历史自助查询分开。
- 认证事件包括登录结果、重新认证、MFA 挑战/证明（含 step-up）、令牌/会话签发或刷新、注销和账户恢复认证。操作审计事件包括密码/联系方式变更、因子注册/移除、MFA 设置及策略变更、会话撤销、身份绑定、账户状态、Provider 配置及管理员操作等状态变更。每个事件写入对应数据集；除非存在已记录的安全用途，不重复记录同一操作。
- 使用稳定的事件/操作值记录结果。两类日志及应用日志均不得包含密码、OTP、令牌、完整 PII、Provider 密钥或恢复码。

### 4.6 运维行为

- 提供健康、就绪、存活状态和基数受控的指标。敏感运维端点需限制在管理访问边界内。
- 在日志和响应中传递请求关联 ID。
- 配置分布式限流及共享短时效安全状态。必须明确失败行为；撤销、防重放和反滥用限制所需状态不可用时应 fail closed。访问 JWT 的撤销检查依赖共享状态，因此共享状态不可用时所有受保护 API 都会返回 `503`；部署须据此设定共享状态的可用性目标。
- 设置请求/请求头大小限制、可信代理规则、CORS、安全响应头、TLS 要求和无状态会话策略（不创建 `HttpSession`）。
- 提供启动配置错误、存储/共享状态服务故障、密钥轮换、邮件/短信 Provider 故障、备份和事件响应手册，以及最后一个管理员丢失全部因子或凭据时的受审计应急访问（break-glass）流程。

## 5. HTTP API 契约

### 5.1 通用约定

- 基础路径为 `/api`；版本策略见第 2 节第 2 条。
- JSON 请求与响应使用 `application/json; charset=utf-8`。OAuth2/OIDC 重定向及框架协议端点不属于 JSON 业务 API。
- 遵循标准 HTTP 语义：`GET` 安全；`PUT` 替换资源；`PATCH` 部分更新；`DELETE` 幂等。创建资源返回 `201 Created`，成功且无响应体返回 `204 No Content`，异步或有意采用通用响应的操作返回 `202 Accepted`，其他场景使用适当的 `4xx`/`5xx`。
- HTTP 边界只暴露契约 DTO。不得序列化持久化对象、密码状态、因子秘密、Provider 密钥、原始令牌或内部审计元数据。
- OpenAPI 中每个端点都必须声明认证状态、授权规则、校验约束、限流决策、审计决策和预期错误。
- MFA 因子和认证流程 API 仅在 MFA 运行时启用时提供；若部署提供 MFA 管理能力，`/api/mfa-settings` 管理 API 在 MFA 停用时仍可供获授权管理员重新启用。部署未提供 MFA 能力时，所有 MFA 路由（包括设置管理）均不可用。OAuth2 Client 专属 API 仅在 OAuth2 Client 部署开关启用时提供。关闭功能时，不得返回可用的因子或 Provider；不可用路由不注册或按统一契约返回 `404`，不得留下可绕过的半启用流程。
- 业务资源路径使用名词和复数集合名，资源状态变化优先使用 `PUT`、`PATCH`、`DELETE` 表达；认证、MFA 因子注册/证明/恢复、密码恢复/解锁、令牌刷新和外部协议回调等有明确流程语义的端点可采用流程型路径。创建 challenge/request 等过程资源时优先使用集合路径和 `POST`，不得把秘密凭据放入 URL。挑战类接口需经邮件/短信异步发送时返回 `202`，在响应中直接返回挑战载荷时返回 `200`。
- 所有匿名可调用的认证流程（注册与验证、登录、待 MFA 流程、密码重置、解锁、令牌刷新与兑换、公开 Provider 列表）统一位于 `/api/auth/` 下，便于集中配置匿名访问和限流规则；`/api/users` 只用于管理员用户集合，`/api/users/me` 用于已认证用户自助。
- 注册验证、密码重置和解锁邮件中的链接指向前端页面，凭据放在 URL 片段（`#` 之后）中，避免进入服务端访问日志和 Referer；前端在请求体中提交凭据，服务端接口不通过查询参数接收凭据。框架规定的 OAuth2/OIDC 与 WebAuthn 协议端点保留其协议路径。

用户名/密码登录的过滤器链约束见 4.1。`AuthenticationEntryPoint` 与 `AccessDeniedHandler` 负责过滤器链中的 `401`/`403`；`@RestControllerAdvice`（`GlobalExceptionHandler`）只处理进入 MVC 后的请求绑定、校验及应用异常，不能代替 Security 处理器。Spring Security 认证事件通过事件发布机制进入认证日志。

当前邮箱登录请求体示例：

```json
{
  "email": "user@example.test",
  "password": "user-supplied-password"
}
```

将来的手机号密码登录接口约束见 4.1；手机号须先验证后才能作为登录凭据。不得把密码写入日志、事件、URL 或错误响应。

非分页业务响应使用如下统一 envelope：

```json
{
  "message": "操作成功",
  "code": null,
  "data": {}
}
```

错误使用同一稳定 envelope 和机器可读应用错误码，HTTP 状态仍是最终依据。校验错误可在 `data` 中返回字段详情；意外错误应返回通用消息和关联 ID，不得返回异常消息或堆栈。

```json
{
  "message": "请求校验失败",
  "code": "VALIDATION_FAILED",
  "data": [{ "field": "email", "message": "must be a well-formed email address" }]
}
```

列表 API 使用稳定且有文档说明的分页契约，限制 `page` 和 `size`，采用确定性排序并列出允许排序字段。分页对象包含 `content`、`page`、`size`、`totalElements`、`totalPages` 和 `last`；除非客户端兼容要求有明确理由，不要在分页结果外再套一层自定义 envelope。

Bearer-token 响应是业务 envelope 的明确例外。使用 OAuth 2.0 字段名：`access_token`、`token_type`、`expires_in`（秒），以及启用时的 `refresh_token`；扩展字段必须有文档，并返回 `Cache-Control: no-store` 和 `Pragma: no-cache`。这些字段并不意味着本服务是 OAuth2 Authorization Server。

待 MFA 登录响应不是令牌响应。客户端只获得短时效事务句柄、过期时间和允许的下一步操作。句柄是不透明 bearer secret，客户端只在内存中保存；不得写入日志或 Referer。客户端在 `/api/auth/mfa/*` 请求中通过 `Authorization: MFA <transaction-handle>` 传递，绝不放在 URL 中。所有必需因子通过后返回令牌响应。示例：

```json
{
  "message": "需要进一步验证",
  "code": "MFA_REQUIRED",
  "data": { "transactionHandle": "opaque-one-time-value", "expiresIn": 300 }
}
```

需先注册因子时返回 `202` 及 `MFA_ENROLLMENT_REQUIRED`（见 4.2）。`data` 包含受限访问令牌、过期时间和允许执行的操作列表。示例：

```json
{
  "message": "需要先注册多因素认证",
  "code": "MFA_ENROLLMENT_REQUIRED",
  "data": {
    "restrictedAccessToken": "opaque-or-jwt-restricted-token",
    "expiresIn": 900,
    "allowedOperations": ["TOTP_ENROLLMENT", "WALLET_ENROLLMENT", "RECOVERY_CODES", "LOGOUT"]
  }
}
```

`POST /api/auth/mfa/verify` 通过 `Authorization: MFA` Header 接收事务凭据，并通过请求体接收因子类型和证明。所有必需因子通过后返回最终登录结果（`200`）；若仍需验证其他因子，则返回有文档说明的下一步响应（`202`）。挑战证明不得放在查询参数中。

### 5.2 认证与恢复

| 方法 | 路径 | 用途 | 访问与成功响应 | 重要错误/规则 |
|---|---|---|---|---|
| `POST` | `/api/auth/registrations` | 使用邮箱和密码注册本地用户 | 公开；通用 `202` | 先完成全部输入校验（含密码策略），再检查邮箱唯一性，使校验错误不随邮箱是否存在而变化；新邮箱与重复邮箱返回相同响应，不返回用户 ID 或 `Location`，重复邮箱时向原邮箱发送提示；限流；账户须按 4.1 完成邮箱验证后才能登录，因此登录结果也不泄露注册时邮箱是否已存在 |
| `POST` | `/api/auth/registrations/verification-requests` | 重新发送注册邮箱验证凭据 | 公开；通用 `202` | 请求使用 `email`；无论邮箱是否存在或是否已验证都返回相同响应；新凭据使旧凭据失效；按 IP 和规范化邮箱限流 |
| `POST` | `/api/auth/registrations/verifications` | 使用验证凭据激活待验证账户 | 公开，需单次验证凭据；`204` | 请求体包含验证凭据和注册密码；原子消费；凭据无效、过期、已用或密码不符时返回相同的通用 `401`，并限制尝试次数；成功后不自动登录 |
| `POST` | `/api/auth/email/login` | 通过 Spring Security 过滤器链验证邮箱和密码 | 完成时 `200`，需 MFA 时 `202`（`MFA_REQUIRED`），需先注册因子时 `202`（`MFA_ENROLLMENT_REQUIRED`，见 4.2） | JSON 字段为 `email`、`password`；无效凭据和锁定均返回相同 `401`；超限 `429`；所有必需因子完成前不得返回完整令牌/会话；不得由 Controller 处理 |
| `GET` | `/api/auth/mfa/options` | 列出本次待处理登录允许的因子 | 使用 `Authorization: MFA` Header 中的待 MFA 凭据；`200` | 仅返回允许的因子类型、掩码目标提示和策略进度；因子资格由服务端确定，主凭据验证前不得泄露账户状态 |
| `POST` | `/api/auth/mfa/challenge` | 请求配置的二次因子挑战 | 使用 `Authorization: MFA` Header 中的待 MFA 凭据；需经邮件/短信发送时 `202`，直接返回挑战载荷（如钱包）时 `200` | 请求指定允许的因子类型；通用发送结果只含重试时间，不泄露其他账户/联系方式是否存在；钱包挑战返回钱包客户端需要签署的载荷 |
| `POST` | `/api/auth/mfa/verify` | 验证待登录因子证明 | 使用 `Authorization: MFA` Header 中的待 MFA 凭据；完成时 `200`，仍需因子时 `202` | 请求体包含允许的因子类型和证明；拒绝无效、过期、重放或超过尝试次数的证明；原子消费 |
| `POST` | `/api/auth/mfa/cancel` | 放弃待处理登录并使事务失效 | 待 MFA 事务凭据；`204` | 使事务句柄失效并清除未完成挑战；事务已过期时保持幂等 |
| `POST` | `/api/auth/password-reset-requests` | 发起邮箱密码恢复请求 | 公开；通用 `202` | 请求使用 `email`；无论用户是否存在都返回相同响应；按 IP 和规范化邮箱限流 |
| `POST` | `/api/auth/password-resets` | 使用重置凭据创建密码重置结果 | 公开，需单次重置凭据；`204` | 拒绝无效/过期/重放凭据；成功后撤销现有会话 |
| `POST` | `/api/auth/unlock-requests` | 向已验证邮箱发送解锁挑战 | 公开；通用 `202` | 请求使用 `email`；按用户/IP 限流；无论邮箱或锁定状态如何均返回相同响应 |
| `POST` | `/api/auth/unlocks` | 使用单次解锁凭据创建解锁结果 | 公开，需单次解锁凭据；`204` | 原子消费；限制尝试并审计；不得透露用户或锁定状态 |
| `POST` | `/api/auth/token/refresh` | 为 bearer-token 客户端轮换 refresh credential | Refresh credential；令牌响应 `200` | 无效/重用凭据拒绝；重用时撤销整个令牌族；禁止缓存 |
| `GET` | `/api/auth/login-providers` | 列出登录页可用的外部 OAuth 2.0/OIDC Provider | OAuth2 Client 已启用时公开；`200` | 关闭时不暴露该功能路由；不包含本地登录方式或 MFA 因子；只返回安全标识和展示字段，不返回密钥或内部端点配置 |
| `POST` | `/api/auth/token/exchange` | 用外部登录回调签发的一次性兑换码换取本地令牌 | 公开，需兑换码和 `code_verifier`；令牌响应 `200`，需 MFA 或先注册因子时与邮箱登录相同的 `202` 响应 | 代码短时效、只存校验值、绑定登录事务且只能消费一次；`code_verifier` 与发起时的 `code_challenge` 不符时返回通用 `401` 并作废该代码；不得绕过 4.3 的外部登录 MFA 要求 |
| `POST` | `/api/auth/token/revocation` | 用 refresh token 注销对应会话 | 公开，请求体提交 refresh token；`204` | 撤销该凭据所属会话及凭据族；凭据无效、过期或已撤销时同样返回 `204`，不泄露凭据状态；限流 |

### 5.3 资料、联系方式、凭据与会话

| 方法 | 路径 | 用途 | 访问与成功响应 | 重要错误/规则 |
|---|---|---|---|---|
| `GET` | `/api/users/me` | 读取当前用户资料和联系方式状态 | 已认证；`200` | 返回本人邮箱/手机号、验证状态、待替换联系方式的掩码值、`displayName` 和本人权限列表；仅返回自助操作和界面权限判断所需字段 |
| `PATCH` | `/api/users/me` | 更新个人资料 | 已认证；`200` | 首版只允许修改 `displayName`；拒绝邮箱、手机号、状态、角色等其他字段（联系方式通过变更请求接口修改）；校验长度和字符 |
| `POST` | `/api/users/me/reauthentication` | 重新认证以刷新近期认证时间 | 已认证；提交密码时 `204`，提交 `registrationId` 时 `202` 返回授权跳转地址 | 请求体包含 `password` 或已绑定身份的 `registrationId`（同时提交 `continue`）之一；OIDC 重新认证事务绑定当前 `sid`，回调完成后按 `continue` 跳回客户端；无本地密码却提交密码时返回 `409`；仅更新当前会话的最近认证时间，不满足近期 MFA；密码校验按 4.1 复用登录认证组件；不受 MFA 开关影响 |
| `PUT` | `/api/users/me/password` | 设置或替换当前用户密码 | 已认证；`204` | 已有密码时校验当前密码；无本地密码时按 4.1 要求近期认证；撤销其他会话 |
| `POST` | `/api/users/me/email-change-requests` | 发起设置或更换账户邮箱 | 已认证且近期认证；`202` 返回 `requestId` 和过期时间 | 请求体包含新邮箱；向新邮箱发送验证码；新邮箱确认前保留原已验证邮箱有效；向原已验证邮箱发送变更通知；新请求使旧请求失效；限流 |
| `POST` | `/api/users/me/email-change-requests/{requestId}/resend` | 重新发送邮箱验证码 | 已认证；`202` | 仅发送至该请求的新邮箱；新验证码使旧验证码失效；限制重发 |
| `POST` | `/api/users/me/email-change-requests/{requestId}/confirm` | 确认新邮箱 | 已认证；`204` | 验证码放在请求体；设置过期时间、尝试上限并单次消费；确认时原子校验新邮箱唯一性并替换邮箱 |
| `POST` | `/api/users/me/phone-change-requests` | 发起设置或更换账户手机号 | 已认证且近期认证；`202` 返回 `requestId` 和过期时间 | 请求体包含新号码；校验目标号码并发送验证码；新号码确认前保留原已验证号码有效；向原已验证联系方式发送变更通知；新请求使旧请求失效；按用户/目标/IP 限流 |
| `POST` | `/api/users/me/phone-change-requests/{requestId}/resend` | 重新发送手机验证码 | 已认证；`202` | 仅发送至该请求的新号码；新验证码使旧验证码失效；限制重发 |
| `POST` | `/api/users/me/phone-change-requests/{requestId}/confirm` | 确认新手机号 | 已认证；`204` | 验证码放在请求体；设置过期时间、尝试上限并单次消费；原子替换号码 |
| `GET` | `/api/users/me/sessions` | 列出活动会话 | 已认证；分页 `200` | 返回安全的设备摘要、创建/最近使用/过期时间和当前会话标记；IP 掩码显示 |
| `DELETE` | `/api/users/me/sessions/{sessionId}` | 撤销属于当前用户的单个会话，包括当前会话 | 已认证；`204` | `sessionId` 可为保留值 `current`，表示当前会话；校验归属；撤销当前会话即完成注销；重复撤销保持幂等，不得泄露其他用户的会话是否存在 |
| `DELETE` | `/api/users/me/sessions` | 撤销当前用户全部会话 | 已认证；`204` | 执行近期认证/MFA 策略；明确当前会话也会被撤销 |
| `GET` | `/api/users/me/auth-logs` | 列出当前用户自己的认证历史 | 已认证；分页 `200` | 只提供用户自助查询视图；包含可用时的粗略地点/设备摘要，不含其他用户或特权运维信息；遵循保留期限和分页上限 |
| `GET` | `/api/users/me/audit-logs` | 列出与当前用户账户相关的操作审计记录 | 已认证；分页 `200` | 仅包含当前用户作为操作者或受影响用户且获准查看的账户相关记录；仅返回安全投影；排除其他用户数据和特权调查详情 |

### 5.4 MFA、钱包、恢复码与 Passkey

| 方法 | 路径 | 用途 | 访问与成功响应 | 重要错误/规则 |
|---|---|---|---|---|
| `GET` | `/api/users/me/mfa/factors` | 列出已注册因子类型和状态 | 完整认证；`200` | 只返回安全的因子 ID/类型/状态/标签/时间戳和掩码目标；不得返回 TOTP 密钥、恢复码校验值、无必要的原始钱包地址或 WebAuthn 凭据内部信息 |
| `POST` | `/api/users/me/mfa/totp/enrollment` | 创建待确认 TOTP 注册 | 完整认证且近期认证，或受限会话；已有因子时需近期 MFA；`200` | 配置密钥/URI 仅在注册时返回一次，响应禁止缓存；待确认流程需过期 |
| `POST` | `/api/users/me/mfa/totp/enrollment/confirm` | 校验设置码并激活 TOTP | 完整认证或受限会话；`201` 返回因子摘要 | 必须存在待确认设置；拒绝验证码重放；原子激活 TOTP，若无恢复码则同时创建；恢复码明文只返回一次且禁止缓存 |
| `PATCH` | `/api/users/me/mfa/factors/{factorId}` | 修改因子标签 | 完整认证；`200` | 只允许修改 `label`；校验长度和字符；记录操作审计 |
| `DELETE` | `/api/users/me/mfa/factors/{factorId}` | 移除单个已注册因子（TOTP、钱包、Passkey 等） | 完整认证且近期 MFA；`204` | 使用应用因子 ID，不暴露原始凭据材料；按策略禁止移除最后一个必需因子并保留可用的恢复/登录方式；通知并审计 |
| `POST` | `/api/users/me/mfa/recovery-codes` | 签发新的恢复码集合 | 已有活动恢复码时需完整认证且近期 MFA；否则需近期认证，或为受限会话；`200` | 原子使旧码失效；明文只返回一次且禁止缓存 |
| `DELETE` | `/api/users/me/mfa/recovery-codes` | 撤销当前恢复码集合 | 完整认证且近期 MFA；`204` | 活动因子策略要求恢复码时不得移除 |
| `POST` | `/api/users/me/mfa/wallet/enrollment` | 开始注册钱包因子 | 完整认证且近期认证，或受限会话；已有因子时需近期 MFA；`200` 返回签名挑战 | 挑战单次、短时效，并绑定账户、origin/域名、操作和配置的链/网络 |
| `POST` | `/api/users/me/mfa/wallet/enrollment/confirm` | 验证钱包签名并注册钱包 | 完整认证或受限会话；`201` 返回因子摘要 | 校验挑战、签名、签名对应地址（EOA 恢复地址或 EIP-1271 校验）、域名/origin、过期时间、链/网络和重放；证明成功前不激活；若无恢复码则同时创建，明文只返回一次且禁止缓存 |
| `POST` | `/api/users/me/mfa/step-up/challenge` | 为敏感操作开始新的因子挑战 | 完整认证；需经邮件/短信发送时 `202`，直接返回挑战载荷时 `200`；均返回不透明挑战句柄 | 请求指定允许的业务操作和因子类型；绑定用户/会话/操作及过期时间；按因子渠道限流 |
| `POST` | `/api/users/me/mfa/step-up/verify` | 验证待处理 step-up 操作的因子证明 | 完整认证；验证成功 `204` | 绑定发起会话和操作；拒绝过期/重放证明或用于其他操作的证明 |

待登录时，`POST /api/auth/mfa/challenge` 请求选定因子的挑战，`POST /api/auth/mfa/verify` 提交因子类型和证明。共用登录验证流程覆盖 TOTP、恢复码、已验证邮箱/手机 OTP（若启用，且受第 2 节第 7 条限制）、钱包签名和 WebAuthn 断言。证明绑定待登录事务，不能用于注册因子、step-up 或其他操作。恢复码的使用必须审计并限流。已认证后的 step-up 使用独立的 `/api/users/me/mfa/step-up/challenge` 和 `/api/users/me/mfa/step-up/verify`，不使用待登录接口。

待登录挑战请求示例：

```json
POST /api/auth/mfa/challenge
{
  "factorType": "WALLET_SIGNATURE",
  "walletAddress": "0x1234...abcd",
  "chainId": "eip155:1"
}
```

MFA 事务凭据通过 `Authorization: MFA` Header 传递，不放入请求体。钱包挑战请求包含目标钱包地址和配置的链/网络；响应包含不透明 `challengeId`、需要签名的完整域名绑定消息和过期时间。邮箱/手机挑战只返回通用发送状态和重试时间。TOTP 和恢复码不需要先发送挑战。

```json
POST /api/auth/mfa/verify
{
  "factorType": "WALLET_SIGNATURE",
  "challengeId": "one-time-challenge-id",
  "proof": { "signature": "base64-or-hex-signature" }
}
```

TOTP 或恢复码的 `proof` 放提交的验证码/恢复码，而不是签名。使用稳定的因子类型值，例如 `TOTP`、`RECOVERY_CODE`、`EMAIL_OTP`、`PHONE_OTP`、`WALLET_SIGNATURE` 和 `WEBAUTHN`；服务端只接受已启用且策略允许的类型。包含验证码或签名的请求必须使用 TLS，不得记录日志；可能被缓存的相关响应应设为 no-store。服务端忽略客户端声称的因子成功状态或保证等级。绝不索取或接收钱包助记词或种子短语。

Step-up 请求体指定允许的业务操作和符合条件的因子类型（钱包验证时也可包含钱包地址/链）。服务端返回不透明挑战句柄及因子所需的挑战载荷；验证绑定已认证会话和目标操作。不得将客户端任意提供的操作字符串用作授权策略。验证成功后更新会话的最近 MFA 时间；目标操作被策略标记为高风险时，另记录绑定该操作的短时效一次性 step-up 授权，由目标操作消费（见 4.4）。默认情况下，恢复码不得用于高风险 step-up；例外必须在因子策略中明确记录并审计。

WebAuthn/passkey 注册和断言使用 Spring Security 支持的端点与浏览器流程。在标准 Servlet 支持下，应将以下端点记录为框架管理的协议操作：注册选项 `POST /webauthn/register/options`、注册凭据 `POST /webauthn/register`、认证选项 `POST /webauthn/authenticate/options`、断言 `POST /login/webauthn`。注册要求已认证且近期认证（或处于受限会话），已有因子时还需近期 MFA。在待登录流程中用作 MFA 因子时，认证选项和断言必须绑定到该事务。Spring Security 默认将 ceremony 选项保存在 `HttpSession`，`/login/webauthn` 成功后会建立完整认证，并且不会自动执行近期认证规则。因此，用于 bearer 客户端或待 MFA 事务时，必须自定义选项存储（绑定事务句柄）和成功处理（只记录因子证明，不建立完整会话或签发令牌），并为注册端点追加近期认证/近期 MFA 授权规则；本服务只服务 bearer 客户端，因此 Passkey 只能在完成上述定制并通过测试后提供；未完成时 Passkey 延后到后续版本，不得以默认会话行为上线。按环境配置 RP ID（Web 前端与 API 共用的可注册域名）、允许的 origin（Web 前端 origin，以及原生 App 的 origin，如 Android 的 `android:apk-key-hash:…`；iOS 需在 RP ID 域名上声明关联域）、用户验证策略和凭据持久性。只有框架验证完整 WebAuthn ceremony 后才视为因子通过。如果框架版本或定制改变这些端点，应在生成的 OpenAPI 文档中公布实际路径和请求结构。参见 [Spring Security Passkeys 文档](https://docs.spring.io/spring-security/reference/servlet/authentication/passkeys.html)。

因子发现、挑战请求/验证和取消待登录都必须通过 `Authorization: MFA` Header 携带待 MFA 凭据。取消使用 `/api/auth/mfa/cancel`。因子证明无效时返回通用 `401`，待登录事务/挑战过期返回 `410 Gone`，因子状态转换冲突返回 `409`，超出次数限制返回带重试时间的 `429`；响应不得泄露用户/因子是否存在。待 MFA 状态不能访问资料、会话、因子管理、管理接口或普通应用资源。注销当前会话使用 `DELETE /api/users/me/sessions/current`，要求完整认证或受限会话，不能代替取消待处理事务。对应 Spring Security 因子权限只能在因子证明完成协议验证后授予。

### 5.5 OAuth2/OIDC 登录、身份和 Provider 管理

| 方法 | 路径 | 用途 | 访问与成功响应 | 重要错误/规则 |
|---|---|---|---|---|
| `GET` | `/oauth2/authorization/{registrationId}` | 发起已配置的 OAuth2/OIDC 登录 | OAuth2 Client 已启用时公开；客户端以顶层页面导航（原生 App 用系统浏览器）访问；框架重定向 | Registration 必须启用；登录时查询参数 `continue`（已登记的跳回目标 ID）和 `code_challenge`（S256）必填，绑定和重新认证时由服务端返回的授权地址携带事务标识；见 4.3 |
| `GET` | `/login/oauth2/code/{registrationId}` | 接收授权响应 | OAuth2 Client 已启用时由框架管理 | 校验 state/nonce/协议响应；不得另写冲突回调 Controller |
| `GET` | `/api/users/me/external-identities` | 列出当前用户已绑定的外部身份 | OAuth2 Client 已启用且已认证；`200` | 返回身份 ID、registration ID 和安全展示信息，不返回 access/refresh token 或原始 subject |
| `POST` | `/api/users/me/external-identity-binding-requests` | 开始将外部身份绑定到当前账户 | OAuth2 Client 已启用、已认证且通过适用的 step-up 策略；`202` 返回 `requestId`、授权跳转地址和过期时间 | 请求体包含 `registrationId` 和 `continue`；创建绑定到当前用户和会话的短时效绑定请求；客户端以顶层页面导航（原生 App 用系统浏览器）访问返回的地址，不通过 XHR 跟随重定向；新请求使该用户的旧请求失效；不得仅按邮箱自动绑定 |
| `POST` | `/api/users/me/external-identity-binding-requests/{requestId}/confirm` | 确认回调完成的待确认绑定 | OAuth2 Client 已启用且已认证；`201` 返回已绑定身份摘要 | 请求体包含回调交给客户端的一次性绑定结果码；当前用户必须是发起该请求的用户，否则返回通用 `404` 并作废该请求；结果码短时效、只存校验值并原子消费；确认时原子校验 `(issuer, subject)` 未被其他账户绑定，冲突返回 `409`；通知并审计 |
| `DELETE` | `/api/users/me/external-identities/{identityId}` | 解绑外部身份 | OAuth2 Client 已启用、已认证且通过适用的 step-up 策略；`204` | 保留另一种可用登录方式；按策略审计和通知 |

同一用户能否绑定同一 Provider 的多个身份由产品决定，解绑始终按身份 ID 进行。绑定事务与登录共用框架回调 `/login/oauth2/code/{registrationId}`，由自定义的授权请求解析器和成功处理器根据 state 关联的事务区分登录、绑定和重新认证，不得另写冲突的回调 Controller。为防止绑定 CSRF（攻击者把为自己账户发起的授权地址发给受害者，使受害者的外部身份被绑到攻击者账户），绑定回调不直接生效，只把 Provider 身份记入待确认绑定，并把一次性绑定结果码交给 `continue` 指定的客户端；客户端以当前登录用户的 bearer token 调用确认接口，服务端核对确认人即发起人。受害者的客户端以受害者身份确认时与发起人不符，绑定被拒绝。

登录、绑定和重新认证的回调完成后，后端只能重定向到 `continue` 对应的已登记地址：Web 前端页面或已声明的 App Link/Universal Link；不得接受任意 return URL。一次性兑换码或绑定结果码放在 URL 片段中；登录时客户端携带 `code_verifier` 调用 `/api/auth/token/exchange` 兑换，绑定时调用确认接口；不得把 access/refresh token 放入 URL。失败时只在片段中返回通用错误码。已绑定身份列表是绑定的最终状态来源。

Provider 注册管理是可选高级功能，仅在 OAuth2 Client 已启用且选择动态管理时提供。除非运维确实需要运行时修改，否则优先由部署配置管理。若启用，所有接口要求专门的 OAuth 配置权限，并按敏感操作策略要求 step-up/MFA；密钥只写不读，变更必须审计。

| 方法 | 路径 | 用途 | 访问与成功响应 | 重要错误/规则 |
|---|---|---|---|---|
| `GET` | `/api/oauth2-clients` | 列出受管理的 Provider 注册 | OAuth 配置权限；分页 `200` | 密钥仅以“已配置/未配置”表示 |
| `GET` | `/api/oauth2-clients/{registrationId}` | 查看单个受管理的 Provider 注册 | OAuth 配置权限；`200` | 返回安全的管理视图；不得返回 client secret |
| `GET` | `/api/oauth2-client-templates` | 列出安全配置模板 | OAuth 配置权限；`200` | 模板仅为配置参考，不代表 Provider 元数据已验证 |
| `POST` | `/api/oauth2-clients` | 创建 Provider 注册 | OAuth 配置权限；`201` | 校验协议、scope、认证方法、HTTPS 和严格主机白名单；记录审计 |
| `PUT` | `/api/oauth2-clients/{registrationId}` | 替换 Provider 配置 | OAuth 配置权限；`200` | 明确密钥保留语义；不得变更已有注册的 issuer 或 Provider；设置 `enabled: false` 时执行与删除相同的最后登录方式检查；校验并审计 |
| `DELETE` | `/api/oauth2-clients/{registrationId}` | 删除 Provider 配置 | OAuth 配置权限；`204` | 禁用通过 `PUT` 设置 `enabled: false` 完成；存在已绑定身份且其中有用户没有其他可用登录方式时返回 `409`；记录审计 |
| `POST` | `/api/oauth2-clients/{registrationId}/connectivity-tests` | 执行受限配置诊断 | OAuth 配置权限；`200` | 不发送 client secret；限制超时、不跟随重定向；要求主机白名单和出站控制；连通性不代表登录成功 |

#### OAuth Client 配置表示

创建和替换请求使用经过校验的 JSON 对象。Provider 专属字段只有在符合已文档化协议模型时才允许：

```json
{
  "registrationId": "google-workspace",
  "providerType": "oidc",
  "clientName": "Company login",
  "loginButtonLabel": "Continue with Company",
  "loginButtonIcon": "company",
  "clientId": "public-client-identifier",
  "clientSecret": "write-only-secret",
  "clientAuthenticationMethod": "client_secret_basic",
  "scopes": ["openid", "profile", "email"],
  "authorizationUri": "https://idp.example.test/authorize",
  "tokenUri": "https://idp.example.test/token",
  "userInfoUri": "https://idp.example.test/userinfo",
  "issuerUri": "https://idp.example.test",
  "jwkSetUri": "https://idp.example.test/jwks",
  "authorizationParameters": {"prompt": "select_account"},
  "subjectAttributeName": "sub",
  "displayNameAttributeName": "name",
  "emailAttributeName": "email",
  "enabled": true
}
```

对于不适用 issuer/JWK Set URI 的 OAuth 2.0 Provider，可不提供 `issuerUri` 和 `jwkSetUri`；需提供 user-info 配置和稳定 subject 映射。OIDC 必须提供 issuer 和协议要求的 scope/元数据。客户端认证方法必须来自明确的允许列表。额外授权参数数量和长度需受限，并禁止覆盖协议保留字段，例如 `state`、`nonce`、`redirect_uri`、`client_id`、`response_type` 和 `code_challenge`。

`registrationId` 必须遵循明确、稳定的格式，并在 Provider 注册间唯一；删除后不得复用于不同的 issuer 或 Provider，以免新 Provider 下相同的 subject 登录旧用户账户。Redirect URI 由服务端按 `{baseUrl}/login/oauth2/code/{registrationId}` 生成，在响应中只读返回；请求中提交的值与生成值不一致时拒绝。

创建操作返回 `201 Created`、`Location` Header 和安全的 Provider 配置表示。列表/详情/更新响应可包含 registration/provider 标识、客户端名称、按钮展示信息、client ID、启用状态、scope、协议端点、用户属性映射及 `clientSecretConfigured`；绝不返回 client secret。更新时，只有在明确启用并记录行为后，省略或留空密钥才可保留现有密钥；清除密钥需单独明确操作或字段。不得记录包含密钥的请求体。

`GET /api/auth/login-providers` 是供登录页发现可用登录选项的公开接口，仅在标准响应 envelope 中返回已启用的外部 OAuth 2.0/OIDC Provider，例如 `data` 为 `[{"registrationId":"google-workspace","providerType":"oidc","clientName":"Company login","loginButtonLabel":"Continue with Company","loginButtonIcon":"company"}]`。这些展示字段与管理配置表示中的对应安全字段使用相同名称。它不列出本地登录方式或 MFA 因子。启用动态管理时，Provider 注册配置由独立的 `/api/oauth2-clients` 接口管理。不得返回 client ID、secret、token URI、user-info URI、JWK URI、scope 或授权参数。

`POST /api/oauth2-clients/{registrationId}/connectivity-tests` 返回 registration ID、端点是否有响应、测试时间和各端点结果/状态。不得发送客户端凭据、跟随重定向或声称连通性证明了凭据、scope、身份映射或完整登录流程有效。不得返回 Provider 响应正文或含敏感查询参数的 URL。

OAuth Client 配置变更通过 `GET /api/audit-logs` 使用相同的安全操作审计视图返回，必要时按资源类型/ID 筛选。不得创建独立的 OAuth Client 审计日志。`GET /api/oauth2-client-templates` 只返回非敏感初始值；启用前运维人员仍需审核模板配置。

OAuth 登录回调签发短时效、单次使用兑换码。`POST /api/auth/token/exchange` 核对 `code_verifier` 后消费该码并返回本地令牌响应；兑换码绑定已完成的 Provider 登录和发起时的 `code_challenge`，仅保存校验值，不得记录日志；除回调交接时放在 URL 片段中外，不得出现在 URL 中。

动态端点配置可能导致 SSRF 风险。主机白名单、DNS/IP 校验、阻止重定向、超时和网络出站控制必须组合使用；单独的 URL 白名单不足以形成完整网络边界。若 Provider 配置静态管理，则省略这些管理接口。

### 5.6 管理资源与公钥发现

| 方法 | 路径 | 用途 | 访问与成功响应 | 重要错误/规则 |
|---|---|---|---|---|
| `GET` | `/api/users` | 按查询条件和状态搜索用户 | 用户管理权限；分页 `200` | 限制分页大小；稳定排序；尽量减少 PII；普通用户不得枚举此集合 |
| `GET` | `/api/users/{userId}` | 查看管理员用用户信息投影 | 用户管理权限；`200` | 只返回支持/管理工作需要的字段，包括当前角色；`/api/users/me` 保留为自助接口 |
| `DELETE` | `/api/users/{userId}` | 软删除账户 | 用户管理权限；按敏感操作策略执行 step-up/MFA；`204` | 原子撤销全部会话和 refresh 凭据；禁止删除最后一个有效管理员；要求原因并记录操作审计；通知用户；邮箱和外部身份的释放规则见第 11 节第 6 条 |
| `GET` | `/api/users/{userId}/sessions` | 查看指定用户的活动会话 | 用户管理权限；分页 `200` | 字段与本人会话列表一致，IP 掩码显示；查询本身记录操作审计 |
| `PATCH` | `/api/users/{userId}/status` | 禁用或重新启用账户 | 用户管理权限；按敏感操作策略执行 step-up/MFA；`204` | 与登录失败锁定分开处理；停用时原子撤销该用户全部会话和 refresh 凭据；审计操作者/原因；禁止禁用最后一个有效管理员 |
| `PUT` | `/api/users/{userId}/roles` | 替换用户角色集合 | 角色管理权限；按敏感操作策略执行 step-up/MFA；`204` | 不得修改自己的角色；授予的角色所含权限必须是操作者自身已持有权限的子集；不得移除最后一个有效管理员；只接受已定义的角色值；变更按 4.4 在刷新时生效，需立即生效时撤销受影响会话；记录操作者、原因和变更前后角色的操作审计 |
| `DELETE` | `/api/users/{userId}/sessions` | 撤销指定用户全部会话（安全事件响应） | 用户管理权限；按敏感操作策略执行 step-up/MFA；`204` | 原子撤销会话和 refresh 凭据；要求原因并记录操作审计；通知用户 |
| `GET` | `/api/users/{userId}/mfa/factors` | 查看指定用户的已注册因子 | 用户管理权限；`200` | 字段与本人因子列表一致，不返回任何秘密；查询本身记录操作审计 |
| `DELETE` | `/api/users/{userId}/mfa/factors` | 在支持恢复流程中清除用户全部因子 | 用户管理权限；近期 MFA；`204` | 仅用于已记录的支持恢复流程（如因子丢失、强制 MFA 宽限期过期）；要求原因和身份核验记录；同时作废恢复码并撤销该用户全部会话；被强制 MFA 的用户重新获得注册宽限期；通过已验证渠道通知用户；记录操作审计 |
| `DELETE` | `/api/users/{userId}/authentication-lock` | 管理员清除用户临时登录锁定 | 用户管理权限；按敏感操作策略执行 step-up/MFA；`204` | 要求原因并记录操作审计；不得清除账户停用状态 |
| `GET` | `/api/roles` | 列出部署配置定义的角色及其权限 | 角色管理权限；`200` | 只读；角色由部署配置定义，不提供创建或修改接口 |
| `GET` | `/api/mfa-settings` | 查看部署所提供的 MFA 运行状态和策略 | MFA 管理能力已部署且具备 MFA 配置权限；`200` | 只返回运行开关、允许的因子及策略摘要，不返回用户因子秘密 |
| `PATCH` | `/api/mfa-settings` | 修改 MFA 配置并启用或停用 MFA | MFA 管理能力已部署且具备 MFA 配置权限；`204` | 校验因子/step-up 策略；停用须满足第 2 节第 6 条，否则返回 `409`；要求管理员近期重新认证（MFA 已启用时须完成 MFA step-up），审计变更并通知运维 |
| `GET` | `/api/auth-logs` | 搜索跨用户认证事件，包括无法解析用户的失败尝试 | 专门认证日志权限；分页 `200` | 可按时间、结果、因子/Provider、关联 ID 或已知用户 ID 筛选；不要求 user ID；不暴露原始标识符或秘密输入 |
| `GET` | `/api/audit-logs` | 搜索跨用户和管理操作的操作审计事件 | 专门审计读取权限；分页 `200` | 可按时间、操作、操作者、目标和结果筛选；审计日志查询本身也需审计；限制导出量 |
| `GET` | `/.well-known/jwks.json` | 发布 JWT 校验所需的公钥 | 公开；标准 JWKS 文档 | JWT 基础能力的一部分，始终启用；仅发布公钥；轮换时支持新旧密钥重叠 |

管理操作沿用其他 API 的资源路径；权限由明确授权决定，不由路径名称决定。`/api/users` 和 `/api/users/{userId}` 要求用户管理权限，`/api/users/me` 及其子资源用于用户自助；用户 ID 不得使用保留字面值 `me`。`/api/auth-logs` 和 `/api/audit-logs` 要求各自专属的读取权限，提供跨用户查询。认证事件查询与操作审计查询分开，因为认证失败可能无法解析用户，而操作审计需标识操作者及受影响的目标。`/api/users/me/auth-logs` 提供当前用户自己的认证历史；`/api/users/me/audit-logs` 只提供用户获准查看的本人账户相关操作审计安全投影。OAuth Client 配置变更属于操作审计，并显示在 `/api/audit-logs` 中；不得引入第三类日志模型或重复记录。

### 5.7 授权、限流与错误矩阵

| 请求状态 | 允许的操作 |
|---|---|
| 匿名 | 注册、注册邮箱验证与重发、登录发起、密码恢复与重置、认证解锁请求与完成、公开 Provider 列表、OAuth2/OIDC 登录发起与回调、凭 refresh credential 刷新或注销、凭一次性代码兑换联邦登录凭据、公开公钥发现。首版不提供无密码 Passkey 登录；若后续启用，需单独定义需求 |
| 待 MFA | 因子/挑战发现、请求挑战、验证挑战、绑定待处理事务的 WebAuthn 断言、取消待登录 |
| 受限会话（强制 MFA 注册宽限期） | 仅限 TOTP 和钱包的 `enrollment` 与 `enrollment/confirm`、WebAuthn 注册端点、`POST /api/users/me/mfa/recovery-codes`、`DELETE /api/users/me/sessions/current`；不得访问其他资源 |
| 已认证 | 本人资料、联系方式、凭据、会话、认证事件、本人账户相关操作审计记录、因子管理和 WebAuthn 注册、已绑定身份；敏感变更适用 step-up 策略 |
| 管理员 | 仅访问明确授予管理权限所覆盖的资源；高影响操作按部署策略要求近期 MFA 并记录审计 |

每个凭据/挑战接口都需定义按 IP 及账户或目标地址的限制。可能泄露账户/Provider 身份是否存在的接口应采用通用结果。请求格式或校验失败返回 `400`（不使用 `422`），缺少/无效认证返回 `401`，已认证但权限不足返回 `403`，资源不存在或按策略隐藏时返回 `404`，状态冲突返回 `409`，待登录事务或挑战过期返回 `410`，限流返回带 `Retry-After` 的 `429`，fail-closed 策略下安全基础设施不可用返回 `503`。不得把框架生成的安全 `4xx` 转换成 `500`。

OpenAPI 必须记录请求/响应 schema、状态码、安全方案、限流行为、分页/排序字段以及哪些端点由框架管理。CI 必须检查契约与实现是否偏离。

## 6. 核心数据概念

- `User`：规范化登录标识、`displayName`、凭据状态、账户状态、联系方式验证状态、角色和生命周期时间。
- `ExternalIdentity`：Provider registration/issuer 与不可变 subject；`(issuer, subject)` 唯一。无 issuer 的 OAuth 2.0 Provider 以不可复用的 `registrationId` 代替 issuer。
- `UserCredential`：密码哈希及凭据元数据；不得序列化包含秘密的字段。
- `Role`：命名的权限集合，用户可持有多个角色。权限至少包括用户管理、角色管理、MFA 配置、OAuth 配置、认证日志读取和审计日志读取；首版角色及其权限只由部署配置定义，不提供运行时定义或修改角色的接口；授权检查只依据权限，不依据角色名。系统内置一个不可删除的引导角色，包含全部管理权限，用于初始管理员引导。“有效管理员”指持有角色管理权限且账户已启用的用户。
- `MfaFactor`：因子类型、注册/验证状态、加密秘密或凭据引用、密钥版本和时间。
- `MfaPolicy`：部署允许使用的因子、运行时启用状态、敏感操作 step-up 规则和配置版本；不得保存因子秘密。
- `RecoveryCode`：单向校验值、用户/因子关联和消费时间；校验值唯一。
- `PendingAuthentication`：不完整登录的短时效状态，绑定用户、主因子、已完成及可用后续因子和尝试次数；每一步验证原子推进状态，全部必需因子完成后单次消费。
- `Session` / `RefreshCredential`：凭据校验值、凭据族、过期时间、轮换/撤销元数据和认证上下文。
- `AuthLog`：事件/结果、发生时间、适用的因子/Provider、请求关联 ID、可选的解析后 `userId` 和隐私安全的标识符/网络元数据。用于关联匿名失败尝试的标识符应为受控的掩码值或带密钥摘要，不保存原始邮箱/手机号；未知账户尝试必须能在没有这些 ID 的情况下表示。
- `AuditLog`：操作、发生时间、操作者类型/ID；人为操作时记录 `actorUserId`；另含目标类型/ID、结果、请求关联 ID 和安全变更元数据。系统操作标识系统操作者。

认证事件和操作审计事件属于不同逻辑 schema 和数据集，不要强行合并为充满空字段的宽表。若使用关系型存储，采用不同表；其他存储则采用隔离的集合/流/索引。分别定义保留期限、访问控制和查询索引。OAuth Provider 配置审计属于操作审计数据，不是认证事件，也不构成另一套规范审计模型。

存储操作必须确保唯一性和状态转换原子性，避免账户创建、挑战/恢复码消费、刷新轮换及身份绑定/解绑的并发竞争。实现机制由设计阶段选择，但并发请求不得创建重复身份或重复消费一次性凭据。

## 7. 安全与隐私要求

- 密码使用配置的自适应密码编码器；不得记录或返回明文凭据。
- OTP 熵较低，须以带密钥校验值保存；恢复码须同时具备足够熵并以单向校验值保存。原子执行过期、单次消费、尝试上限和限流。
- TOTP 共享密钥需加密保存、受访问控制、带密钥版本，且不得进入审计/日志。
- 使用 `SecureRandom`；适用时以恒定时间比较秘密派生值。
- 防止登录和恢复流程枚举账户。查询前规范化标识符，并按 IP 和不可逆标识符键限流。
- 外部身份绑定要求稳定 Provider subject 和明确的已验证用户策略；不得仅按邮箱自动合并。
- 测试 OAuth2/OIDC state、nonce、issuer、audience、回调、令牌存储、注销、刷新和重定向白名单。
- API 不设置也不读取 Cookie，不创建 `HttpSession`：认证凭据只来自 `Authorization` Header 或请求体，因此不启用 CSRF 令牌机制；OAuth 流程的发起方由 `code_verifier` 或发起人的 bearer token 识别（见 4.3、5.5），不依赖浏览器 Cookie。CORS 不是授权机制。
- 仅在受控代理会清除客户端自带转发头的情况下信任 forwarded headers；适用时阻止绕过代理的公网访问。
- 生产密钥由密钥管理器/环境变量注入；示例配置只放占位值。启动时校验必需配置。
- JWT 私钥不得发布到 JWKS 或客户端；密钥轮换使用 `kid` 区分密钥，并在验证端保留足够的新旧公钥重叠期。不得因轮换而提前使仍有效的令牌全部失效，除非执行了明确的紧急撤销流程。
- 日志掩码邮箱/手机号；除运维确有需要外避免记录原始 IP；绝不记录密码、OTP、授权码、token、client secret 或恢复码。
- 发布前定义 PII 保留、账户删除/导出、备份保留及审计数据访问策略。

## 8. 开发与编码规范

### 框架与依赖管理

- 目标为 Java 25 LTS（或开始实施时选择的最新 LTS）、Spring Boot 4.1.1 和所选 Boot 版本管理的 Spring Security。Boot 4 使用 Jackson 3 API（`tools.jackson`；注解仍位于 `com.fasterxml.jackson.annotation`）。
- 开始实施时依据官方兼容信息核实框架、库和 Provider SDK 版本。不得猜测版本，也不得无证据覆盖受管版本。
- 持久化引擎、schema/迁移工具、缓存、分布式状态机制、构建插件和部署平台均由实施阶段决定。记录所选方案及其满足本规范的依据；本文不限定数据库、ORM、缓存、迁移工具或代码架构。

### 代码与 API 质量

- Spring 管理组件使用构造器注入。相关配置通过经过校验的配置类型集中绑定；密钥不得进入源码版本控制。
- HTTP 契约与内部持久化表示解耦。在边界校验不可信输入，使用明确 DTO，不暴露内部记录或含秘密状态。
- 根据产品选择包/模块边界和架构风格；保持职责清晰、内聚、可测试和依赖方向可审查。没有实际需要时不引入层、抽象或通用框架。
- Java record 适合作为不可变数据载体。若使用 Lombok，仅用于合适的非 record 类；避免为持久化对象或含秘密对象生成 equality/string 方法。
- 绝对时间使用 UTC 的 `Instant`，时间字段以 `...At` 结尾。持久化/公开状态和操作码使用显式 enum 或有文档的稳定值。
- 完成的阶段不得留下占位/TODO 实现。Provider 集成必须明确标注支持状态和运维行为。

### 错误处理、隐私与 API 文档

- 在边界校验请求体和查询/路径参数。使用稳定机器可读错误码及通用意外错误消息；不得暴露堆栈、凭据或实现异常文本。
- 保持正确 HTTP 状态语义，包括框架生成的安全失败响应。安全错误响应中包含关联 ID。
- OpenAPI 记录选定的成功/错误 envelope、分页、认证模式、限流、授权及框架管理协议端点。
- 按适当级别记录预期客户端错误，但不包含敏感输入。绝不记录密码、OTP、因子秘密、access/refresh token、OAuth code/client secret、钱包种子短语或完整 PII。

### 验证

- 使用所选 Boot 版本支持的当前 JUnit 版本，以及适用的断言、mock 和 Spring Security 测试工具。根据所选存储和部署技术选择测试基础设施。
- 尽量让领域/安全行为测试独立于外部服务。集成测试需覆盖持久性保证、一次性消费原子性、过期、刷新轮换、密钥处理、适用时的 schema 演进以及所需的多实例行为。
- 测试 OAuth 回调失败（state、nonce、issuer、audience、签名、redirect）、账户绑定冲突、Provider 失败和本地注销语义，以及有活动因子或被强制 MFA 的用户经外部登录和 `token/exchange` 时仍须完成 MFA；测试 `code_verifier` 缺失或不符时兑换失败、`continue` 不在登记列表时拒绝发起、他人确认绑定请求时被拒绝，以及整个 OAuth 流程不创建 `HttpSession`；不要求每次构建都连接真实 Provider。
- 测试每种 MFA 因子的成功和拒绝路径，包括钱包挑战重放、恢复码单次使用/尝试上限、WebAuthn 注册/断言策略及因子权限转换。
- 测试密码长度（含超过 72 字节的多字节密码）、注册与登录两侧的 Unicode 规范化、阻止列表和历史密码哈希兼容；测试邮箱登录过滤器对未知账户、错误密码、停用/锁定账户返回一致响应且耗时不可区分，以及锁定计数、到期、锁定通知、用户解锁和管理员解锁。
- 测试注册邮箱验证：待验证账户不能登录；确认验证须提交正确的注册密码；对待验证邮箱重复注册会使旧验证凭据失效；过期未验证账户被清除并释放邮箱。
- 测试 refresh token 并发刷新：宽限窗口内的重复提交不撤销凭据族，窗口外重用则撤销。
- 测试新地点/设备通知只使用解析后的最少必要摘要；User-Agent/IP 地理信息不能单独导致登录拒绝，日志中不得保存完整 User-Agent 或精确位置。
- 测试 MFA 管理 API 的授权、配置审计、启停状态及停用保护：存在强制 MFA 策略或用户已激活因子时必须拒绝停用，运行时停用后不得通过登录流程或因子 API 绕过策略。
- CI 执行文档化的构建/验证命令、实施方案选定的格式/静态检查、依赖/安全检查和密钥扫描。报告实际结果及未运行的检查。

## 9. 质量属性与验收标准

- **正确性：** 只有配置的因子策略满足后才能签发会话/访问令牌；挑战、钱包证明和恢复码在并发情况下均只能使用一次。
- **安全性：** 授权在服务端执行；OAuth 身份不能仅凭邮箱跨账号绑定；生产密钥/配置错误应快速失败；必需安全状态不可用时 fail closed；认证日志和操作日志具有独立的访问/保留策略。
- **可用性：** 存储和共享状态健康情况可见；重试有界；Provider 失败返回稳定错误；运维人员有恢复说明。
- **隐私：** 审计/日志/API 不暴露秘密并尽量减少 PII；数据导出/删除/保留策略明确。
- **兼容性：** API 契约及错误码有文档，OpenAPI 与实现一致。
- **可维护性：** 职责内聚、契约有文档、不重复实现 OAuth 协议、构建可复现。
- **验证：** 所选构建和 CI 检查通过；集成测试覆盖所选存储/共享状态实现；不得静默使用实质不同的测试基础设施替代目标环境。

## 10. 交付阶段

1. **基础能力：** Boot 4.1.1、依赖与配置、所选存储/共享状态集成、JWT 签发/校验/JWKS、API/错误契约、健康指标、测试流水线、CI 和编码规范。
2. **本地身份：** 注册、密码登录、账户锁定、按标识符/IP 限流、邮箱验证、密码恢复/修改、会话/refresh-token 策略。
3. **可选 MFA：** 启用时实现分阶段登录、Security 7 因子权限集成、TOTP、WebAuthn/passkey、钱包签名、恢复码、按操作配置 step-up、重放/竞争测试；验证关闭开关时不会留下半启用流程或降级强制 MFA 策略。
4. **可选 OAuth2/OIDC Client：** 启用时实现 Provider 注册、框架管理登录/回调、用户映射、安全本地开通、绑定/解绑、本地会话/令牌交接、集成 Provider 测试；关闭时不暴露 Provider 能力。
5. **OAuth2 Client 使用：** 仅为已确认需求增加委托 Provider API 调用或服务 Client Credentials；定义 consent、authorized-client 生命周期和 scope。
6. **加固与发布：** 反滥用、分布式状态故障测试、数据生命周期、密钥轮换、部署手册、OpenAPI 对齐和发布评审。只有产品选择动态 Provider 管理时才增加对应能力。

## 11. 实施前待决策事项

1. 服务只为自有一方应用签发令牌，还是成为标准 OAuth2/OIDC Authorization Server？若是后者，需扩展范围并在功能实现前评估 Spring Authorization Server。
2. 首版允许哪些因子组合，各组合可宣称何种保证等级？明确钱包协议/链及恢复码的恢复/支持策略。
3. 社交登录是否采用即时开通（JIT）？账户创建和绑定信任哪些声明？重复邮箱如何处理？
4. 服务将代表用户访问哪些外部 Provider API？这决定 authorized-client 保留、consent、scope 和 refresh-token 策略。
5. 哪种存储与共享状态技术、拓扑和故障目标能满足持久性、原子性、过期、可用性及一致性要求？
6. 目标部署适用哪些限流、保留周期、用户数据导出/删除义务和审计保留期限？账户软删除后原邮箱能否重新注册、已绑定的 `(issuer, subject)` 是否释放？
7. 个人数据导出目前只有功能要求，尚未定义请求、状态查询和安全下载 API；确定是否提供自助 API 及其近期认证、过期和审计规则。自助 API 不提供用户停用或删除功能。

## 12. 官方框架参考

- [Spring Boot 4.1.1 OAuth2 配置](https://docs.spring.io/spring-boot/reference/security/oauth2.html)
- [Spring Boot 4.1.1 Security 自动配置](https://docs.spring.io/spring-boot/reference/web/spring-security.html)
- [Spring Security 7 MFA](https://docs.spring.io/spring-security/reference/servlet/authentication/mfa.html)
- [Spring Security 7 Passkeys](https://docs.spring.io/spring-security/reference/servlet/authentication/passkeys.html)
- [Spring Security OAuth2 概览](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Spring Security Servlet 认证架构](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html)
- [Spring Security 密码存储与 PasswordEncoder](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [NIST SP 800-63B：认证器管理与密码要求](https://pages.nist.gov/800-63-4/sp800-63b.html)
