import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = (file) => readFile(new URL(file, import.meta.url), 'utf8')

test('member profile keeps /account canonical and redirects the legacy URL', async () => {
  const routes = await source('./AppRoutes.jsx')
  const login = await source('../pages/LoginPage.jsx')

  assert.match(routes, /path="account" element=\{<AccountPage\s*\/>\}/)
  assert.match(routes, /path="account\/profile" element=\{<Navigate to="\/account" replace\s*\/>\}/)
  assert.match(login, /role === 'MEMBER'\) return '\/account'/)
})

test('site chrome exposes guest actions and canonical member account links', async () => {
  const chrome = await source('../components/CinemaSiteChrome.jsx')

  assert.match(chrome, /to="\/login"[^>]*>Đăng nhập/)
  assert.match(chrome, /to="\/register"[^>]*>Đăng ký/)
  assert.match(chrome, /to="\/member"[\s\S]*?>Không gian thành viên/)
  assert.match(chrome, /to="\/account"[\s\S]*?>Hồ sơ của tôi/)
  assert.doesNotMatch(chrome, /account\/profile/)
})
