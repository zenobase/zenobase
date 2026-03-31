package com.zenobase.tasks.fitbark;

import org.joda.time.DateTime;

public record Dog(String id, String name, DateTime created, DateTime modified) {}
