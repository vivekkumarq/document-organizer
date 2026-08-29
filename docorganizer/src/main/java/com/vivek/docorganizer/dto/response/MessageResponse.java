package com.vivek.docorganizer.dto.response;

/** Simple {"message": "..."} envelope for endpoints with nothing else to say. */
public record MessageResponse(String message) { }
