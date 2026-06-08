# Vault ACL-политика merchants-core (применяется downstream-пайплайном tkbpay/vault-config).
# KV v1 mount `pay`, раскладка pay/{env}/{app}-{secret} (без сегмента data/).
# Свои секреты: pay/test/merchants-core-db-password
path "pay/test/merchants-core-*" {
  capabilities = ["read"]
}

# callee: ключ-идентичность caller'а payadmin-bff (валидация X-Internal-Admin-Key).
path "pay/test/payadmin-bff-internal-key" {
  capabilities = ["read"]
}

# transitional (dual-accept) — старый общий admin-key. Убрать после cutover на per-caller.
path "pay/test/sbp-router-admin-key" {
  capabilities = ["read"]
}
