package com.notifyhub.template;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.exception.InvalidRequestException;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateRenderer {

    // {{ name }}  : letters, digits, underscore; must not start with a digit; spaces allowed inside
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]{0,49})\\s*\\}\\}");

    private TemplateRenderer() {
    }

    public record RenderedMessage(String subject, String body) {
    }

    /** All variable names used in the given texts, sorted. */
    public static Set<String> extractVariables(String... texts) {
        Set<String> names = new TreeSet<>();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            Matcher m = PLACEHOLDER.matcher(text);
            while (m.find()) {
                names.add(m.group(1));
            }
        }
        return names;
    }

    /** Rejects broken placeholders like "{{name" or "{{ }}" when a template is saved. */
    public static void validateSyntax(String text) {
        if (text == null) {
            return;
        }
        String leftover = PLACEHOLDER.matcher(text).replaceAll("");
        if (leftover.contains("{{") || leftover.contains("}}")) {
            throw new InvalidRequestException(
                    "Malformed placeholder. Use {{variableName}} with letters, digits or underscore");
        }
    }

    public static RenderedMessage render(Channel channel, String subject, String body,
                                         Map<String, String> variables) {
        Map<String, String> vars = (variables == null) ? Map.of() : variables;

        Set<String> missing = new TreeSet<>();
        for (String name : extractVariables(subject, body)) {
            if (vars.get(name) == null) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("Missing template variables: " + missing);
        }

        boolean escapeHtml = (channel == Channel.EMAIL);
        String renderedSubject = (subject == null) ? null
                : substitute(subject, vars, false).replaceAll("[\\r\\n]+", " ");
        String renderedBody = substitute(body, vars, escapeHtml);
        return new RenderedMessage(renderedSubject, renderedBody);
    }

    private static String substitute(String text, Map<String, String> vars, boolean escapeHtml) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = vars.get(m.group(1));
            if (escapeHtml) {
                value = HtmlUtils.htmlEscape(value);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}