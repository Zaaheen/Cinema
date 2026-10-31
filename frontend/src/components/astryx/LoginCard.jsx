// Adapted from the Astryx CLI `login-card` template.
import { FilmIcon } from '@heroicons/react/24/outline'
import { Link } from 'react-router-dom'
import { Button } from '@astryxdesign/core/Button'
import { Card } from '@astryxdesign/core/Card'
import { Center } from '@astryxdesign/core/Center'
import { Icon } from '@astryxdesign/core/Icon'
import { VStack } from '@astryxdesign/core/Layout'
import { Text, Heading } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'

export function LoginCard({
  username,
  password,
  error,
  isSubmitting,
  onUsernameChange,
  onPasswordChange,
  onSubmit,
}) {
  return (
    <Center axis="both" padding={6} style={{ minHeight: '100%' }}>
      <VStack gap={4} hAlign="center" style={{ width: '100%', maxWidth: 400 }}>
        <VStack gap={2} hAlign="center">
          <Icon icon={FilmIcon} size="lg" />
          <Text type="body" weight="bold" size="lg">Cinema SE03</Text>
        </VStack>

        <Card padding={8} width="100%">
          <form onSubmit={onSubmit}>
            <VStack gap={4} hAlign="stretch">
              <VStack gap={1} hAlign="center">
                <Heading level={2}>Chào mừng trở lại</Heading>
                <Text type="body" color="secondary" size="sm">
                  Đăng nhập để tiếp tục đặt vé.
                </Text>
              </VStack>

              <VStack gap={2}>
                <TextInput
                  label="Tên đăng nhập"
                  value={username}
                  onChange={onUsernameChange}
                  placeholder="Nhập tên đăng nhập"
                  autoComplete="username"
                  htmlName="username"
                  isRequired
                  size="lg"
                />
                <TextInput
                  label="Mật khẩu"
                  type="password"
                  value={password}
                  onChange={onPasswordChange}
                  placeholder="Nhập mật khẩu"
                  autoComplete="current-password"
                  htmlName="password"
                  isRequired
                  size="lg"
                  status={error ? { type: 'error', message: error } : undefined}
                />
              </VStack>

              <Button
                label="Đăng nhập"
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isSubmitting}
              />

              <Text type="supporting" color="secondary" justify="center">
                Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
              </Text>
            </VStack>
          </form>
        </Card>
      </VStack>
    </Center>
  )
}
