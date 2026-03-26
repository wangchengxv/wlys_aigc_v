package com.example.aigc.dto;

import java.util.List;

public record PagedResult<T>(List<T> list, long total) {
}
