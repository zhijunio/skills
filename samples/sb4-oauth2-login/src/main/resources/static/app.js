let accessToken = null;
const message = document.querySelector("#message");
function showAccount() {
  document.querySelector("#login-card").classList.add("hidden");
  document.querySelector("#account-card").classList.remove("hidden");
  loadProfile();
}
async function loadProviders() {
  const response = await fetch("/api/auth/providers");
  const data = await response.json();
  const host = document.querySelector("#providers");
  data.providers.forEach(provider => {
    const button = document.createElement("button");
    button.className = "provider"; button.type = "button"; button.textContent = provider.label;
    if (provider.icon) {
      const icon = document.createElement("span"); icon.className = "provider-icon"; icon.textContent = provider.icon;
      button.prepend(icon);
    }
    button.addEventListener("click", () => { window.location.href = `/oauth2/authorization/${provider.id}`; });
    host.append(button);
  });
}
async function loadProfile() {
  const response = await fetch("/api/me", {headers:{Authorization:`Bearer ${accessToken}`}});
  if (!response.ok) { document.querySelector("#profile").textContent = "身份信息读取失败，请重新登录。"; return; }
  document.querySelector("#profile").textContent = JSON.stringify(await response.json(), null, 2);
}
document.querySelector("#login-form").addEventListener("submit", async event => {
  event.preventDefault(); message.textContent = "";
  const form = new FormData(event.currentTarget);
  try {
    const response = await fetch("/api/auth/login", {method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({username:form.get("username"),password:form.get("password")})});
    if (!response.ok) throw new Error("用户名或密码错误");
    accessToken = (await response.json()).accessToken; showAccount();
  } catch (error) { message.textContent = error.message || "登录失败，请检查服务连接。"; }
});
document.querySelector("#logout").addEventListener("click", () => { accessToken = null; document.querySelector("#account-card").classList.add("hidden"); document.querySelector("#login-card").classList.remove("hidden"); document.querySelector("#password").value = ""; });
async function exchangeCallbackCode() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");
  if (!code) return;
  history.replaceState(null, "", "/");
  try {
    const response = await fetch("/api/auth/oauth2/exchange", {method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({code})});
    if (!response.ok) throw new Error("登录凭证已失效，请重新登录。");
    accessToken = (await response.json()).accessToken; showAccount();
  } catch (error) { message.textContent = error.message; }
}
const query = new URLSearchParams(window.location.search);
if (query.has("oauthError")) {
  history.replaceState(null, "", "/");
  message.textContent = "第三方登录失败，请检查提供方配置后重试。";
}
loadProviders().catch(() => { message.textContent = "无法读取登录提供方配置。"; });
exchangeCallbackCode();
