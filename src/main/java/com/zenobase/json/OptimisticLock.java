package com.zenobase.json;

public record OptimisticLock(long seqNo, long primaryTerm) {}
