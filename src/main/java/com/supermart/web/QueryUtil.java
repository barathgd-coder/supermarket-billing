package com.supermart.web;

import org.springframework.web.util.UriComponentsBuilder;

/** Builds the "k=v&" prefix used by the pagination links so filters survive page changes. */
public final class QueryUtil {
    private QueryUtil() { }

    public static String of(String... keyValues) {
        UriComponentsBuilder b = UriComponentsBuilder.newInstance();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String v = keyValues[i + 1];
            if (v != null && !v.isBlank()) b.queryParam(keyValues[i], v);
        }
        String q = b.build().encode().getQuery();
        return q == null ? "" : q + "&";
    }

    public static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
