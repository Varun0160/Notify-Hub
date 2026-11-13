package com.notifyhub.notification;

import com.notifyhub.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageCursorTest {

    @Test
    void encodeThenDecodeGivesTheSameId() {
        assertEquals(12345L, PageCursor.decode(PageCursor.encode(12345L)));
    }

    @Test
    void noCursorMeansFirstPage() {
        assertNull(PageCursor.decode(null));
        assertNull(PageCursor.decode("  "));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(InvalidRequestException.class, () -> PageCursor.decode("!!!not-base64!!!"));
    }

    @Test
    void validBase64ThatIsNotANumberIsRejected() {
        String text = Base64.getUrlEncoder().withoutPadding().encodeToString("abc".getBytes());
        assertThrows(InvalidRequestException.class, () -> PageCursor.decode(text));
    }

    @Test
    void zeroAndNegativeIdsAreRejected() {
        assertThrows(InvalidRequestException.class, () -> PageCursor.decode(PageCursor.encode(0)));
        assertThrows(InvalidRequestException.class, () -> PageCursor.decode(PageCursor.encode(-5)));
    }
}