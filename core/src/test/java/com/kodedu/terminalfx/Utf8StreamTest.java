package com.kodedu.terminalfx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("decoding output a byte at a time")
class Utf8StreamTest {

    @Test
    @DisplayName("keeps a character that arrived in pieces whole")
    void splitCharacters() {
        Utf8Stream stream = new Utf8Stream();
        byte[] bytes = "aığ日😀z".getBytes(StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder();
        for (byte value : bytes) {
            out.append(stream.decode(new byte[] { value }, 0, 1));
        }
        assertEquals("aığ日😀z", out.toString());
    }

    @Test
    @DisplayName("decodes a whole chunk in one go")
    void wholeChunk() {
        Utf8Stream stream = new Utf8Stream();
        byte[] bytes = "günaydın 日本語".getBytes(StandardCharsets.UTF_8);
        assertEquals("günaydın 日本語", stream.decode(bytes, 0, bytes.length));
    }

    @Test
    @DisplayName("replaces a byte that is not part of any character, and carries on")
    void rubbishByte() {
        Utf8Stream stream = new Utf8Stream();
        byte[] bytes = { 'a', (byte) 0xC3, (byte) 0x28, 'b' };
        String text = stream.decode(bytes, 0, bytes.length);
        assertEquals('a', text.charAt(0));
        assertEquals('b', text.charAt(text.length() - 1));
    }

    @Test
    @DisplayName("decodes only the slice it was given")
    void offsetAndLength() {
        Utf8Stream stream = new Utf8Stream();
        byte[] bytes = "xxhello worldxx".getBytes(StandardCharsets.UTF_8);
        assertEquals("hello world", stream.decode(bytes, 2, 11));
    }
}
