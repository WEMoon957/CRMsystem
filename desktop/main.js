/**
 * SaaS CRM 桌面客户端主进程
 * 职责：拉起本地后端（内置 JRE + crm.jar）、等待健康检查、加载应用窗口；
 * 配置（公司共享 MySQL 连接等）存于 userData/config.json，首次启动进入配置向导。
 */
const { app, BrowserWindow, ipcMain, shell } = require('electron')
const path = require('path')
const fs = require('fs')
const http = require('http')
const net = require('net')
const crypto = require('crypto')
const { spawn, spawnSync } = require('child_process')

// ---------- 路径 ----------
const userDataDir = app.getPath('userData')
const configPath = path.join(userDataDir, 'config.json')
const logDir = path.join(userDataDir, 'logs')
const backendLogPath = path.join(logDir, 'backend.log')
const defaultUploadDir = path.join(userDataDir, 'uploads')

// 打包后资源在 resources/ 下；开发模式（npm start）回退到仓库内产物与本机 Java
let javaExe = path.join(process.resourcesPath, 'jre', 'bin', 'java.exe')
let backendJar = path.join(process.resourcesPath, 'backend', 'crm.jar')
let frontendDir = path.join(process.resourcesPath, 'frontend')
if (!app.isPackaged) {
  javaExe = 'java'
  backendJar = path.join(__dirname, '..', 'backend', 'target', 'crm.jar')
  frontendDir = path.join(__dirname, '..', 'frontend', 'dist')
}

// ---------- 运行状态 ----------
let backendProcess = null
let mainWindow = null
let setupWindow = null
let loadingWindow = null
let errorWindow = null
let lastError = { message: '', logTail: '' }
const logRing = []

// ---------- 配置读写 ----------
function readConfig() {
  try {
    return JSON.parse(fs.readFileSync(configPath, 'utf-8'))
  } catch {
    return null
  }
}

function writeConfig(cfg) {
  const tmp = configPath + '.tmp'
  fs.writeFileSync(tmp, JSON.stringify(cfg, null, 2), 'utf-8')
  fs.renameSync(tmp, configPath)
}

function isConfigComplete(cfg) {
  return !!(cfg && cfg.mysql && cfg.mysql.host && cfg.mysql.port && cfg.mysql.database && cfg.mysql.user)
}

// ---------- 日志 ----------
function appendLog(line) {
  logRing.push(line)
  if (logRing.length > 200) logRing.shift()
}

// ---------- 窗口 ----------
function closeWindow(win) {
  if (win && !win.isDestroyed()) win.close()
}

function showSetupWindow() {
  closeWindow(loadingWindow); closeWindow(errorWindow); closeWindow(mainWindow)
  setupWindow = new BrowserWindow({
    width: 640,
    height: 720,
    resizable: false,
    autoHideMenuBar: true,
    title: 'SaaS CRM - 初始配置',
    webPreferences: { preload: path.join(__dirname, 'preload.js') }
  })
  setupWindow.loadFile('setup.html')
}

function showLoadingWindow() {
  closeWindow(setupWindow); closeWindow(errorWindow); closeWindow(mainWindow)
  loadingWindow = new BrowserWindow({
    width: 420,
    height: 300,
    resizable: false,
    frame: false,
    webPreferences: { preload: path.join(__dirname, 'preload.js') }
  })
  loadingWindow.loadFile('loading.html')
}

function showErrorWindow(message) {
  lastError = { message, logTail: logRing.slice(-50).join('') }
  closeWindow(setupWindow); closeWindow(loadingWindow); closeWindow(mainWindow)
  errorWindow = new BrowserWindow({
    width: 720,
    height: 560,
    autoHideMenuBar: true,
    title: 'SaaS CRM - 启动失败',
    webPreferences: { preload: path.join(__dirname, 'preload.js') }
  })
  errorWindow.loadFile('error.html')
}

function showMainWindow(port) {
  closeWindow(setupWindow); closeWindow(loadingWindow); closeWindow(errorWindow)
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1080,
    minHeight: 700,
    autoHideMenuBar: true,
    title: 'SaaS CRM',
    webPreferences: { preload: path.join(__dirname, 'preload.js') }
  })
  // 外部链接交给系统浏览器，不在应用内跳转
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http://127.0.0.1')) return { action: 'allow' }
    shell.openExternal(url)
    return { action: 'deny' }
  })
  mainWindow.loadURL(`http://127.0.0.1:${port}/`)
}

// ---------- 后端生命周期 ----------
function getFreePort() {
  return new Promise((resolve, reject) => {
    const srv = net.createServer()
    srv.unref()
    srv.on('error', reject)
    srv.listen(0, '127.0.0.1', () => {
      const port = srv.address().port
      srv.close(() => resolve(port))
    })
  })
}

function buildEnv(cfg, port) {
  const mysql = cfg.mysql
  const env = {
    ...process.env,
    SPRING_PROFILES_ACTIVE: 'prod',
    SERVER_PORT: String(port),
    MYSQL_URL: `jdbc:mysql://${mysql.host}:${mysql.port}/${mysql.database}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`,
    MYSQL_USER: mysql.user,
    MYSQL_PASSWORD: mysql.password || '',
    JWT_SECRET: cfg.jwtSecret,
    UPLOAD_DIR: cfg.uploadDir || defaultUploadDir,
    WEB_STATIC_DIR: frontendDir,
    FRONTEND_ORIGIN: `http://127.0.0.1:${port}`,
    TZ: 'Asia/Shanghai'
  }
  // 首次初始化主账号（仅空库生效）
  if (cfg.seed && cfg.seed.enabled) {
    env.SEED_ENABLED = 'true'
    env.SEED_ADMIN_USERNAME = cfg.seed.adminUsername || 'admin'
    env.SEED_ADMIN_PASSWORD = cfg.seed.adminPassword || 'Admin@123'
  }
  return env
}

function spawnBackend(cfg, port) {
  fs.mkdirSync(logDir, { recursive: true })
  fs.mkdirSync(cfg.uploadDir || defaultUploadDir, { recursive: true })
  const logStream = fs.createWriteStream(backendLogPath, { flags: 'a' })

  const child = spawn(javaExe, ['-XX:MaxRAMPercentage=75', '-jar', backendJar], {
    env: buildEnv(cfg, port),
    windowsHide: true
  })

  child.stdout.on('data', (d) => {
    const text = d.toString()
    appendLog(text)
    logStream.write(text)
  })
  child.stderr.on('data', (d) => {
    const text = d.toString()
    appendLog(text)
    logStream.write(text)
  })
  child.on('exit', (code) => {
    appendLog(`\n[desktop] backend exited with code ${code}\n`)
    logStream.end()
    if (backendProcess === child) backendProcess = null
  })
  return child
}

function waitForHealth(port, timeoutMs, isDead) {
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + timeoutMs
    const retry = () => {
      if (isDead()) return reject(new Error('后端进程已退出'))
      if (Date.now() > deadline) return reject(new Error('后端健康检查超时'))
      setTimeout(tick, 500)
    }
    const tick = () => {
      if (isDead()) return reject(new Error('后端进程已退出'))
      const req = http.get({ host: '127.0.0.1', port, path: '/healthz', timeout: 2000 }, (res) => {
        res.resume()
        if (res.statusCode === 200) resolve()
        else retry()
      })
      req.on('error', retry)
      req.on('timeout', () => { req.destroy(); retry() })
    }
    tick()
  })
}

async function boot(cfg) {
  showLoadingWindow()
  const port = await getFreePort()
  backendProcess = spawnBackend(cfg, port)
  const child = backendProcess
  try {
    // 首次运行 Flyway 迁移可能较慢，给到 90 秒
    await waitForHealth(port, 90_000, () => child.exitCode !== null)
  } catch (e) {
    killBackend()
    showErrorWindow(`后端启动失败：${e.message}。请检查 MySQL 连接配置（详见日志）。`)
    return
  }
  // 种子初始化成功后立即关闭，避免配置中长期留存管理员口令
  if (cfg.seed && cfg.seed.enabled) {
    cfg.seed.enabled = false
    delete cfg.seed.adminPassword
    writeConfig(cfg)
  }
  showMainWindow(port)
}

function killBackend() {
  if (backendProcess && backendProcess.pid) {
    if (process.platform === 'win32') {
      spawnSync('taskkill', ['/pid', String(backendProcess.pid), '/T', '/F'])
    } else {
      backendProcess.kill('SIGTERM')
    }
    backendProcess = null
  }
}

// ---------- IPC ----------
ipcMain.handle('config:save', async (_event, cfg) => {
  if (!cfg.mysql || !cfg.mysql.host || !cfg.mysql.database || !cfg.mysql.user) {
    return { ok: false, error: '请完整填写 MySQL 主机、库名与用户名' }
  }
  cfg.mysql.port = Number(cfg.mysql.port) || 3306
  if (!cfg.jwtSecret) {
    cfg.jwtSecret = crypto.randomBytes(32).toString('hex')
  }
  writeConfig(cfg)
  await boot(cfg)
  return { ok: true }
})

ipcMain.handle('app:reconfigure', () => {
  killBackend()
  showSetupWindow()
})

ipcMain.handle('app:info', () => ({
  version: app.getVersion(),
  logDir,
  configPath
}))

ipcMain.handle('error:detail', () => lastError)

ipcMain.handle('logs:open', () => {
  fs.mkdirSync(logDir, { recursive: true })
  shell.openPath(logDir)
})

// ---------- 应用生命周期 ----------
const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      if (mainWindow.isMinimized()) mainWindow.restore()
      mainWindow.focus()
    }
  })

  app.whenReady().then(async () => {
    fs.mkdirSync(userDataDir, { recursive: true })
    const cfg = readConfig()
    if (isConfigComplete(cfg)) {
      await boot(cfg)
    } else {
      showSetupWindow()
    }
  })

  app.on('window-all-closed', () => {
    app.quit()
  })

  app.on('will-quit', () => {
    killBackend()
  })
}
