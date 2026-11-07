const ROLE_HOME = {
  ADMIN: '/admin',
  EMPLOYEE: '/employee',
  MEMBER: '/member',
}

const ROLE_LABEL = {
  ADMIN: 'Quản trị viên',
  EMPLOYEE: 'Nhân viên',
  MEMBER: 'Thành viên',
}

export function roleHome(role) {
  return ROLE_HOME[role] ?? '/account'
}

export function roleLabel(role) {
  return ROLE_LABEL[role] ?? role
}
