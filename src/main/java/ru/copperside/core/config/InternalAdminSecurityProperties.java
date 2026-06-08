package ru.copperside.core.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Конфигурация межсервисной аутентикации {@code /internal/**}.
 *
 * <p>Модель «один ключ = идентичность caller'а»: {@link #acceptedCallers} — map
 * {@code имя-caller'а -> его ключ}. Фильтр валидирует входящий ключ против этой map и пишет имя
 * совпавшего caller'а в лог. {@link #apiKey} — legacy общий ключ; оставлен для dual-accept на время
 * миграции и подлежит удалению после cutover.
 */
@Validated
@ConfigurationProperties(prefix = "merchants-core.internal-admin")
public record InternalAdminSecurityProperties(
        String apiKey,
        Map<String, String> acceptedCallers,
        @NotBlank String headerName
) {
    public InternalAdminSecurityProperties {
        acceptedCallers = acceptedCallers == null ? Map.of() : Map.copyOf(acceptedCallers);
    }

    /**
     * Фильтр включается, как только сконфигурирован хоть один непустой ключ (legacy или per-caller).
     * Пустые значения (плейсхолдер без секрета) держат фильтр выключенным — локальный/тестовый старт
     * без инфраструктуры.
     */
    public boolean enabled() {
        return hasLegacyKey() || hasAcceptedCaller();
    }

    public boolean hasLegacyKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private boolean hasAcceptedCaller() {
        return acceptedCallers.values().stream().anyMatch(v -> v != null && !v.isBlank());
    }
}
