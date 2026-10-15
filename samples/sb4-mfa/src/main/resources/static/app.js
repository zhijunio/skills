const $ = (selector) => document.querySelector(selector)

const state = {
  csrf: null,
  csrfHeader: 'X-CSRF-TOKEN',
  session: null,
  walletEnrollmentMessage: null,
  walletLoginMessage: null,
}

const messages = {
  'Invalid TOTP code': 'TOTP 验证码无效。',
  'Invalid, expired, or reused TOTP code': 'TOTP 验证码无效、已过期或已使用。',
  'TOTP code was already used': '该 TOTP 验证码已使用。',
  'Confirm TOTP enrollment first': '请先确认 TOTP 绑定。',
  'Enroll TOTP first': '请先绑定 TOTP 验证器。',
  'Email challenge is missing, expired, or locked': '邮件验证码不存在、已过期或尝试次数过多。',
  'Invalid email code': '邮件验证码无效。',
  'Email code was already used': '该邮件验证码已使用。',
  'Enroll a wallet first': '请先绑定钱包。',
  'Wallet challenge is missing or expired': '钱包签名挑战不存在或已过期，请重新获取。',
  'Invalid wallet signature': '钱包签名无效。',
  'Expected a 65-byte EIP-191 signature': '签名格式不正确，请使用钱包重新签名。',
  'Invalid EIP-191 signature': '钱包签名无效。',
  'Expected an Ethereum address': '请输入有效的以太坊地址。',
  'Wallet signature does not match the challenged address': '签名地址与挑战地址不一致。',
  'Wallet challenge was already used': '该钱包挑战已使用，请重新获取。',
  'Enroll a recovery phrase first': '请先生成本应用的备用助记词。',
  'Invalid or expired recovery phrase': '助记词无效、已过期或尝试次数过多。',
  'Recovery phrase was already used': '该助记词已使用，请改用其他验证方式。',
}

function localize(message) {
  if (messages[message]) return messages[message]
  if (message?.startsWith('Request failed (')) return `请求失败（${message.match(/\d+/)?.[0] || ''}）。`
  if (message && /[\u3400-\u9fff]/.test(message)) return message
  return '操作未成功，请检查输入或重试。'
}

async function refreshCsrf() {
  const response = await fetch('/api/csrf', { credentials: 'same-origin' })
  const body = await response.json()
  state.csrf = body.token
  state.csrfHeader = body.headerName
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (options.method && options.method !== 'GET' && state.csrf) headers.set(state.csrfHeader, state.csrf)

  const response = await fetch(path, { ...options, headers, credentials: 'same-origin' })
  const contentType = response.headers.get('content-type') || ''
  const body = contentType.includes('json') ? await response.json() : await response.text()
  if (!response.ok) {
    const message = body?.detail || body?.error || body?.message || `Request failed (${response.status})`
    const error = new Error(localize(message))
    error.status = response.status
    throw error
  }
  return body
}

function showResult(selector, message, kind = 'success') {
  const element = $(selector)
  element.textContent = message
  element.dataset.kind = kind
}

function toast(message) {
  const element = $('#toast')
  element.textContent = message
  element.classList.add('visible')
  window.setTimeout(() => element.classList.remove('visible'), 3200)
}

async function refreshSessionView() {
  try {
    const response = await fetch('/api/session', { credentials: 'same-origin' })
    const session = await response.json()
    state.session = session
    const authenticated = session.authenticated
    const mfaComplete = session.mfaComplete
    $('#login-panel').hidden = authenticated
    $('#workspace').hidden = !authenticated
    $('#logout-button').hidden = !authenticated
    $('#mfa-pending').hidden = !authenticated || mfaComplete
    $('#protected-panel').hidden = !mfaComplete
    document.querySelectorAll('[data-factor-setup]').forEach((element) => {
      element.hidden = !mfaComplete
    })
    $('#session-status').textContent = !authenticated ? '尚未登录' : mfaComplete ? '两步验证已完成' : '等待第二步验证'
    $('#factor-description').textContent = mfaComplete
      ? '本会话已通过两步验证，可访问受保护资源或管理验证方式。'
      : '请选择已绑定的验证方式。首次登录可用示例邮件验证码完成验证。'
    return session
  } catch {
    $('#session-status').textContent = '无法连接服务'
    return false
  }
}

async function submitJson(path, form, output) {
  const data = Object.fromEntries(new FormData(form).entries())
  try {
    await request(path, { method: 'POST', body: JSON.stringify(data) })
    form.reset()
    await refreshCsrf()
    const session = await refreshSessionView()
    const complete = session?.mfaComplete
    showResult(output, complete ? '两步验证成功，当前会话已开放受保护资源。' : '操作成功。若这是绑定确认，请再验证一次新生成的验证码。')
    toast(complete ? '两步验证成功' : '验证信息已更新')
  } catch (error) {
    showResult(output, error.message, 'error')
  }
}

function bytesToHex(value) {
  return `0x${Array.from(new TextEncoder().encode(value), (byte) => byte.toString(16).padStart(2, '0')).join('')}`
}

async function walletSign(message) {
  if (!window.ethereum) throw new Error('未检测到浏览器钱包，请在已安装钱包扩展的浏览器中打开页面。')
  const [address] = await window.ethereum.request({ method: 'eth_requestAccounts' })
  const signature = await window.ethereum.request({ method: 'personal_sign', params: [bytesToHex(message), address] })
  return signature
}

function decodeBase64Url(value) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  const binary = atob(padded)
  return Uint8Array.from(binary, (character) => character.charCodeAt(0))
}

function encodeBase64Url(value) {
  const bytes = new Uint8Array(value)
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

function credentialResponse(credential) {
  return {
    id: credential.id,
    rawId: encodeBase64Url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: encodeBase64Url(credential.response.clientDataJSON),
      authenticatorData: encodeBase64Url(credential.response.authenticatorData),
      signature: encodeBase64Url(credential.response.signature),
      userHandle: credential.response.userHandle ? encodeBase64Url(credential.response.userHandle) : null,
    },
    clientExtensionResults: credential.getClientExtensionResults(),
    authenticatorAttachment: credential.authenticatorAttachment,
  }
}

function setBusy(button, busy, text) {
  button.disabled = busy
  if (busy) {
    button.dataset.label = button.textContent
    button.textContent = text
  } else if (button.dataset.label) {
    button.textContent = button.dataset.label
    delete button.dataset.label
  }
}

$('#login-form').addEventListener('submit', async (event) => {
  event.preventDefault()
  const form = event.currentTarget
  const data = new URLSearchParams(new FormData(form))
  try {
    const response = await fetch('/login', {
      method: 'POST',
      body: data,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', [state.csrfHeader]: state.csrf },
      credentials: 'same-origin',
    })
    if (!response.ok || response.url.includes('error')) throw new Error('登录失败，请检查用户名和密码。')
    await refreshCsrf()
    const session = await refreshSessionView()
    if (!session?.authenticated) throw new Error('登录失败，请检查用户名和密码。')
    toast(session.mfaComplete ? '两步验证已完成' : '第一步验证成功，请完成第二步。')
  } catch (error) {
    toast(localize(error.message))
  }
})

$('#logout-button').addEventListener('click', async () => {
  try {
    await request('/logout', { method: 'POST' })
    await refreshCsrf()
    await refreshSessionView()
    toast('已退出登录')
  } catch (error) {
    toast(localize(error.message))
  }
})

$('#totp-enroll').addEventListener('click', async () => {
  try {
    const result = await request('/api/mfa/totp/enroll', { method: 'POST', body: '{}' })
    showResult('#totp-result', `请在验证器应用中添加此密钥：${result.secret}\n配置链接：${result.otpauthUri}`, 'info')
  } catch (error) {
    showResult('#totp-result', localize(error.message), 'error')
  }
})

$('#totp-confirm-form').addEventListener('submit', (event) => {
  event.preventDefault()
  submitJson('/api/mfa/totp/confirm', event.currentTarget, '#totp-result')
})

$('#totp-verify-form').addEventListener('submit', (event) => {
  event.preventDefault()
  submitJson('/api/mfa/totp/verify', event.currentTarget, '#totp-result')
})

$('#email-request').addEventListener('click', async () => {
  try {
    const result = await request('/api/mfa/email/request', { method: 'POST', body: '{}' })
    showResult('#email-result', `已发送到示例邮箱 demo@example.test。验证码请查看应用日志。`)
  } catch (error) {
    showResult('#email-result', localize(error.message), 'error')
  }
})

$('#email-verify-form').addEventListener('submit', (event) => {
  event.preventDefault()
  submitJson('/api/mfa/email/verify', event.currentTarget, '#email-result')
})

$('#wallet-enroll').addEventListener('click', async () => {
  try {
    const result = await request('/api/mfa/wallet/enroll/challenge', {
      method: 'POST', body: JSON.stringify({ address: $('#wallet-address').value.trim() }),
    })
    state.walletEnrollmentMessage = result.message
    $('#wallet-enroll-sign').disabled = false
    showResult('#wallet-result', '绑定挑战已生成，请在钱包中签署。')
  } catch (error) {
    showResult('#wallet-result', localize(error.message), 'error')
  }
})

$('#wallet-enroll-sign').addEventListener('click', async () => {
  try {
    const signature = await walletSign(state.walletEnrollmentMessage)
    await request('/api/mfa/wallet/enroll/verify', { method: 'POST', body: JSON.stringify({ signature }) })
    state.walletEnrollmentMessage = null
    $('#wallet-enroll-sign').disabled = true
    showResult('#wallet-result', '钱包绑定成功。请获取新的挑战以完成验证。')
  } catch (error) {
    showResult('#wallet-result', localize(error.message), 'error')
  }
})

$('#wallet-challenge').addEventListener('click', async () => {
  try {
    const result = await request('/api/mfa/wallet/challenge', { method: 'POST', body: '{}' })
    state.walletLoginMessage = result.message
    $('#wallet-sign').disabled = false
    showResult('#wallet-result', '登录挑战已生成，请在钱包中签署。')
  } catch (error) {
    showResult('#wallet-result', localize(error.message), 'error')
  }
})

$('#wallet-sign').addEventListener('click', async () => {
  try {
    const signature = await walletSign(state.walletLoginMessage)
    await request('/api/mfa/wallet/verify', { method: 'POST', body: JSON.stringify({ signature }) })
    state.walletLoginMessage = null
    $('#wallet-sign').disabled = true
    showResult('#wallet-result', '钱包因子验证成功，已加入当前会话。')
    await refreshCsrf()
    const session = await refreshSessionView()
    if (session?.mfaComplete) toast('两步验证成功，受保护资源已开放。')
  } catch (error) {
    showResult('#wallet-result', localize(error.message), 'error')
  }
})

$('#recovery-phrase-enroll').addEventListener('click', async () => {
  try {
    const result = await request('/api/mfa/recovery-phrase/enroll', { method: 'POST', body: '{}' })
    showResult('#recovery-phrase-result', `请立即离线保存；此助记词只显示这一次：\n\n${result.phrase}`, 'info')
    toast('备用助记词已生成，请立即离线保存')
  } catch (error) {
    showResult('#recovery-phrase-result', localize(error.message), 'error')
  }
})

$('#recovery-phrase-form').addEventListener('submit', (event) => {
  event.preventDefault()
  submitJson('/api/mfa/recovery-phrase', event.currentTarget, '#recovery-phrase-result')
})

$('#check-profile').addEventListener('click', async () => {
  try {
    const result = await request('/api/users/me')
    showResult('#access-result', `个人信息：${result.username}。查询个人信息不要求再次验证 MFA。`)
  } catch (error) {
    showResult('#access-result', localize(error.message), 'error')
  }
})

$('#check-security').addEventListener('click', async () => {
  try {
    const result = await request('/api/account/security')
    showResult('#access-result', `安全设置访问成功：${result.username} · 最近完成 MFA：${result.mfaVerifiedAt}`)
  } catch (error) {
    const message = error.status === 403
      ? '此接口要求最近 5 分钟内完成 MFA。请在上方重新验证一种因子后重试。'
      : localize(error.message)
    showResult('#access-result', message, 'error')
  }
})

$('#passkey-register').addEventListener('click', async () => {
  if (!window.PublicKeyCredential || !navigator.credentials) {
    showResult('#access-result', '当前浏览器不支持 WebAuthn 通行密钥。', 'error')
    return
  }
  const button = $('#passkey-register')
  try {
    setBusy(button, true, '正在注册…')
    const options = await request('/webauthn/register/options', { method: 'POST' })
    options.challenge = decodeBase64Url(options.challenge)
    options.user.id = decodeBase64Url(options.user.id)
    options.excludeCredentials = (options.excludeCredentials || []).map((item) => ({ ...item, id: decodeBase64Url(item.id) }))
    const credential = await navigator.credentials.create({ publicKey: options })
    const payload = {
      publicKey: {
        credential: {
          id: credential.id,
          rawId: encodeBase64Url(credential.rawId),
          type: credential.type,
          response: {
            attestationObject: encodeBase64Url(credential.response.attestationObject),
            clientDataJSON: encodeBase64Url(credential.response.clientDataJSON),
            transports: credential.response.getTransports?.() || [],
          },
          clientExtensionResults: credential.getClientExtensionResults(),
          authenticatorAttachment: credential.authenticatorAttachment,
        },
        label: '我的通行密钥',
      },
    }
    await request('/webauthn/register', { method: 'POST', body: JSON.stringify(payload) })
    showResult('#access-result', '通行密钥注册成功。')
    toast('通行密钥注册成功')
  } catch (error) {
    showResult('#access-result', localize(error.message), 'error')
  } finally {
    setBusy(button, false)
  }
})

$('#passkey-login').addEventListener('click', async () => {
  if (!window.PublicKeyCredential || !navigator.credentials) {
    toast('当前浏览器不支持 WebAuthn 通行密钥。')
    return
  }
  const button = $('#passkey-login')
  try {
    setBusy(button, true, '正在验证…')
    const options = await request('/webauthn/authenticate/options', { method: 'POST' })
    options.challenge = decodeBase64Url(options.challenge)
    options.allowCredentials = (options.allowCredentials || []).map((item) => ({ ...item, id: decodeBase64Url(item.id) }))
    const credential = await navigator.credentials.get({ publicKey: options })
    await request('/login/webauthn', { method: 'POST', body: JSON.stringify(credentialResponse(credential)) })
    await refreshCsrf()
    const session = await refreshSessionView()
    toast(session?.mfaComplete ? '两步验证已完成' : '通行密钥验证成功，请完成第二步。')
  } catch (error) {
    toast(localize(error.message))
  } finally {
    setBusy(button, false)
  }
})

await refreshCsrf()
await refreshSessionView()
