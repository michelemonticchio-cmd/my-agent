package com.monticchio.myagent.dto;

import java.time.Instant;

public class AccountDtos {

    public record AccountInfo(String username, Instant createdAt) {}
}
