package com.notifyhub.notifyhub.template;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.template.TemplateRenderer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRendererTest {

    @Test
    void replacesVariables() {
        var out = TemplateRenderer.render(Channel.SMS, null,
                "Hi {{name}}, your OTP is {{otp}}", Map.of("name", "Ravi", "otp", "4821"));
        assertEquals("Hi Ravi, your OTP is 4821", out.body());
        assertNull(out.subject());
    }

    @Test
    void allowsSpacesInsideBraces() {
        var out = TemplateRenderer.render(Channel.SMS, null, "Hi {{ name }}", Map.of("name", "Ravi"));
        assertEquals("Hi Ravi", out.body());
    }

    @Test
    void missingVariableIsRejected() {
        var ex = assertThrows(InvalidRequestException.class, () ->
                TemplateRenderer.render(Channel.SMS, null, "OTP {{otp}}", Map.of("name", "Ravi")));
        assertTrue(ex.getMessage().contains("otp"));
    }

    @Test
    void nullValueCountsAsMissing() {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", null);
        assertThrows(InvalidRequestException.class, () ->
                TemplateRenderer.render(Channel.SMS, null, "Hi {{name}}", vars));
    }

    @Test
    void extraVariablesAreIgnored() {
        var out = TemplateRenderer.render(Channel.SMS, null, "Hi {{name}}",
                Map.of("name", "Ravi", "unused", "x"));
        assertEquals("Hi Ravi", out.body());
    }

    @Test
    void emailBodyEscapesHtmlButSubjectDoesNot() {
        var out = TemplateRenderer.render(Channel.EMAIL, "Hello {{name}}", "<p>Hi {{name}}</p>",
                Map.of("name", "<b>Ravi</b>"));
        assertEquals("Hello <b>Ravi</b>", out.subject());
        assertEquals("<p>Hi &lt;b&gt;Ravi&lt;/b&gt;</p>", out.body());
    }

    @Test
    void smsDoesNotEscape() {
        var out = TemplateRenderer.render(Channel.SMS, null, "{{x}}", Map.of("x", "a&b"));
        assertEquals("a&b", out.body());
    }

    @Test
    void valuesAreNeverReExpanded() {
        var out = TemplateRenderer.render(Channel.SMS, null, "Hi {{name}}",
                Map.of("name", "{{otp}}", "otp", "1234"));
        assertEquals("Hi {{otp}}", out.body());
    }

    @Test
    void dollarAndBackslashInValuesAreSafe() {
        var out = TemplateRenderer.render(Channel.SMS, null, "Cost {{p}}", Map.of("p", "$5 \\ off"));
        assertEquals("Cost $5 \\ off", out.body());
    }

    @Test
    void newlinesAreStrippedFromSubject() {
        var out = TemplateRenderer.render(Channel.EMAIL, "Hi {{name}}", "body",
                Map.of("name", "Ravi\r\nBcc: evil@x.com"));
        assertEquals("Hi Ravi Bcc: evil@x.com", out.subject());
    }

    @Test
    void malformedPlaceholdersAreRejected() {
        assertThrows(InvalidRequestException.class, () -> TemplateRenderer.validateSyntax("Hi {{name"));
        assertThrows(InvalidRequestException.class, () -> TemplateRenderer.validateSyntax("Hi {{ }}"));
        assertThrows(InvalidRequestException.class, () -> TemplateRenderer.validateSyntax("Hi {{na-me}}"));
        assertDoesNotThrow(() -> TemplateRenderer.validateSyntax("Hi {{ name }} and {{code}}"));
    }

    @Test
    void extractVariablesIsSortedAndDeduplicated() {
        List<String> names = new ArrayList<>(
                TemplateRenderer.extractVariables("Hi {{b}} {{a}}", "{{a}} {{c}}"));
        assertEquals(List.of("a", "b", "c"), names);
    }
}