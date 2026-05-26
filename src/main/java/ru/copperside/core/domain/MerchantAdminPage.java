package ru.copperside.core.domain;

import java.util.List;

public record MerchantAdminPage(List<MerchantAdminLine> lines, long total) {
}
