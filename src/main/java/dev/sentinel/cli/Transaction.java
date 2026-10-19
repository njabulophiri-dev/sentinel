package dev.sentinel.cli;

import java.math.BigDecimal;

public record Transaction(String id, String merchant, String category, BigDecimal amount){}