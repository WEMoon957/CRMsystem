/**
 * 预加载脚本：以 contextBridge 暴露最小 API 给渲染进程（配置页/错误页/主界面）。
 * 渲染进程无 Node 能力，保证安全边界。
 */
const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('desktop', {
  /** 保存初始配置并启动后端（配置页） */
  saveConfig: (cfg) => ipcRenderer.invoke('config:save', cfg),
  /** 返回配置向导（错误页） */
  reconfigure: () => ipcRenderer.invoke('app:reconfigure'),
  /** 打开后端日志目录 */
  openLogs: () => ipcRenderer.invoke('logs:open'),
  /** 应用版本与路径信息 */
  info: () => ipcRenderer.invoke('app:info'),
  /** 启动失败详情（错误页） */
  errorDetail: () => ipcRenderer.invoke('error:detail')
})
