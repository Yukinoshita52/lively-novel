interface DownloadLink {
  download: string
  href: string
  click: () => void
  remove: () => void
}

interface DownloadEnvironment {
  createElement: (tagName: 'a') => DownloadLink
  appendChild: (node: DownloadLink) => void
  createObjectURL: (blob: Blob) => string
  revokeObjectURL: (url: string) => void
}

function createBrowserDownloadEnvironment(): DownloadEnvironment {
  return {
    createElement: (tagName) => document.createElement(tagName),
    appendChild: (node) => document.body.appendChild(node as HTMLAnchorElement),
    createObjectURL: (blob) => URL.createObjectURL(blob),
    revokeObjectURL: (url) => URL.revokeObjectURL(url),
  }
}

export function downloadBlob(
  blob: Blob,
  filename: string,
  environment: DownloadEnvironment = createBrowserDownloadEnvironment(),
) {
  const url = environment.createObjectURL(blob)
  const link = environment.createElement('a')

  try {
    link.href = url
    link.download = filename
    environment.appendChild(link)
    link.click()
  } finally {
    link.remove()
    environment.revokeObjectURL(url)
  }
}
