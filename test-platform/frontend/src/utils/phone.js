export function sanitizePhoneInput(value) {
  return String(value ?? '').replace(/\D/g, '').slice(0, 11)
}

export function isValidPhone(value) {
  return /^\d{11}$/.test(String(value ?? ''))
}

export function normalizePhone(value) {
  return sanitizePhoneInput(value)
}
