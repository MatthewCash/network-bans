package com.matthewcash.network;

import java.util.Date;
import java.util.UUID;

public class Ban {
    public UUID uuid;
    public String reason;
    public Date unbanTime;

    public Ban(UUID uuid, String reason, Date unbanTime) {
        this.uuid = uuid;
        this.reason = reason;
        this.unbanTime = unbanTime;
    }
}