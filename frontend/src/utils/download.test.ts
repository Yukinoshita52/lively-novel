import { downloadBlob } from './download.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

const createdLinks: Array<{
  download: string
  href: string
  clickCount: number
  removed: boolean
}> = []
const appendedNodes: unknown[] = []
const revokedUrls: string[] = []

downloadBlob(new Blob(['yaml'], { type: 'text/yaml' }), 'screenplay.yaml', {
  createElement(tagName) {
    assert(tagName === 'a', '下载工具应创建链接元素')
    const link = {
      download: '',
      href: '',
      clickCount: 0,
      removed: false,
      click() {
        this.clickCount += 1
      },
      remove() {
        this.removed = true
      },
    }
    createdLinks.push(link)
    return link
  },
  appendChild(node) {
    appendedNodes.push(node)
  },
  createObjectURL(blob) {
    assert(blob.size === 4, '下载工具应使用传入的 Blob 创建对象 URL')
    return 'blob:screenplay-yaml'
  },
  revokeObjectURL(url) {
    revokedUrls.push(url)
  },
})

assert(createdLinks.length === 1, '下载工具应创建一个临时链接')
assert(createdLinks[0].download === 'screenplay.yaml', '下载工具应设置下载文件名')
assert(createdLinks[0].href === 'blob:screenplay-yaml', '下载工具应把对象 URL 写入链接')
assert(createdLinks[0].clickCount === 1, '下载工具应触发一次链接点击')
assert(createdLinks[0].removed, '下载工具应移除临时链接')
assert(appendedNodes.length === 1, '下载工具应把临时链接挂到 document.body')
assert(revokedUrls.join(',') === 'blob:screenplay-yaml', '下载工具应释放对象 URL')
