package com.matthewcash.network;

import java.util.Date;
import java.util.UUID;

public record Ban(UUID uuid, String reason, Date unbanTime) {
}
