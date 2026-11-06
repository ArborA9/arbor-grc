import { defineConfig, type ProxyOptions } from 'vite'
import react from '@vitejs/plugin-react'

const apiTarget = process.env.ARBOR_VITE_API_PROXY ?? 'http://127.0.0.1:8080'
const devPort = Number(process.env.ARBOR_VITE_PORT ?? 5173)

function proxyToBackend(): ProxyOptions {
  return {
    target: apiTarget,
    changeOrigin: true,
    configure: (proxy) => {
      // Cloudflare Origin would fail Spring CORS (localhost-only). Strip so proxied
      // /api calls look local. Dev-only — production serves the SPA from Spring.
      proxy.on('proxyReq', (proxyReq) => {
        proxyReq.removeHeader('origin')
      })
    },
  }
}

// The dev server proxies /api to the Spring Boot backend (default :8080).
// In production the React build is served as static files by Spring Boot (single JAR).
export default defineConfig({
  plugins: [react()],
  server: {
    port: Number.isFinite(devPort) ? devPort : 5173,
    strictPort: Boolean(process.env.ARBOR_VITE_PORT),
    host: true, // listen on IPv4 + IPv6 so localhost / 127.0.0.1 both work
    allowedHosts: true,
    proxy: {
      // API + SSO / OIDC handshake paths (browser may hit Vite in some flows)
      '/api': proxyToBackend(),
      '/sso': proxyToBackend(),
      '/oauth2': proxyToBackend(),
      '/login': proxyToBackend(),
    },
  },
})
