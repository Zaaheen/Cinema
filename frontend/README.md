# React + JavaScript/JSX + Vite

## CSRF refresh/logout

Before refresh and logout, the client calls `GET /api/v1/auth/csrf` with credentials, reads the
non-HttpOnly `XSRF-TOKEN` cookie, and sends it as `X-XSRF-TOKEN` on the subsequent credentialed
request. This double-submit flow requires the frontend JavaScript to be able to read that cookie.
With separate cross-site hosts (for example, `app.example.com` and an unrelated API host), browsers
do not expose an API host-only cookie to the frontend origin even when `SameSite=None; Secure` is
used. Deploy behind a same-origin proxy or configure an appropriate shared cookie domain before
enabling that topology; CORS credentials alone do not make the cookie readable.

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Mở rộng cấu hình Oxlint

Với frontend JavaScript/JSX, có thể bật thêm các rule React và Oxc trong `.oxlintrc.json` nếu dự án cần kiểm tra chặt hơn:

```json
{
  "$schema": "./node_modules/oxlint/configuration_schema.json",
  "plugins": ["react", "oxc"],
  "rules": {
    "react/rules-of-hooks": "error",
    "react/only-export-components": ["warn", { "allowConstantExport": true }]
  }
}
```

See the [Oxlint rules documentation](https://oxc.rs/docs/guide/usage/linter/rules) for the full list of rules and categories.
