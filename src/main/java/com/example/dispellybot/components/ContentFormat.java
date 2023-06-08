package com.example.dispellybot.components;

public enum ContentFormat {
  TEXT_PLAIN("text/plain"),
  TEXT_HTML("text/html"),
  TEXT_XML("text/xml"),
  APPLICATION_JSON("application/json"),
  APPLICATION_PDF("application/pdf");

  private final String value;

  ContentFormat(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
