package com.moveo.aicryptoadvisor.client;

/**
 * Provider-agnostic meme, as returned by a meme source (the live Reddit feed or the static
 * curated pool) and held in the meme cache. {@code id} carries a source-specific prefix
 * ({@code reddit-}, {@code meme-}) so it stays unambiguous as a voteable {@code itemRef}.
 */
public record Meme(String id, String imageUrl, String caption) {
}
