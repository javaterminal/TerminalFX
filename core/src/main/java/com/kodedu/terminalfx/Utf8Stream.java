package com.kodedu.terminalfx;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Bytes to text, a chunk at a time, without cutting a character in half.
 *
 * <p>A terminal's output arrives in whatever sizes the network chose, and the last three bytes of
 * a read are quite often the first three of a four-byte character. Decoding each read on its own
 * turns those into replacement characters — a terminal that drops a letter from "ğ" every few
 * kilobytes and nobody can say why. What is left over here is kept and prepended to the next
 * read.
 *
 * <p>Not thread safe: one of these belongs to one stream, and one stream is read by one thread.
 */
final class Utf8Stream {

    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

    /** At most three bytes: the longest prefix of a UTF-8 sequence that is not yet a character. */
    private final ByteBuffer leftover = ByteBuffer.allocate(8);

    String decode(byte[] bytes, int offset, int length) {
        ByteBuffer input;
        if (leftover.position() > 0) {
            input = ByteBuffer.allocate(leftover.position() + length);
            leftover.flip();
            input.put(leftover);
            leftover.clear();
            input.put(bytes, offset, length);
            input.flip();
        } else {
            input = ByteBuffer.wrap(bytes, offset, length);
        }
        CharBuffer out = CharBuffer.allocate(input.remaining() + 1);
        StringBuilder text = new StringBuilder(input.remaining());
        while (true) {
            CoderResult result = decoder.decode(input, out, false);
            out.flip();
            text.append(out);
            out.clear();
            if (result.isUnderflow()) {
                break;
            }
            if (result.isOverflow()) {
                continue;
            }
            // Malformed input is replaced by the decoder's own action; nothing else is expected.
            break;
        }
        if (input.hasRemaining()) {
            leftover.put(input);
        }
        return text.toString();
    }
}
