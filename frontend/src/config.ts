const runtimeConfig = window.__APP_CONFIG__ ?? {}

type ConfigOptions = {
  allowEmpty?: boolean
}

export const getConfigValue = (
  key: keyof AppConfig,
  options: ConfigOptions = {},
): string | undefined => {
  const runtimeValue = runtimeConfig[key]

  if (runtimeValue !== undefined && (options.allowEmpty || runtimeValue !== '')) {
    return runtimeValue
  }

  const buildTimeValue = import.meta.env[key]

  return typeof buildTimeValue === 'string' &&
    (options.allowEmpty || buildTimeValue !== '')
    ? buildTimeValue
    : undefined
}
