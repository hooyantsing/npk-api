package xyz.hooy.npkapi.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static String hyphenToPascalCase(String input, String splitRegex) {
        String[] words = input.split(splitRegex);
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (capitalizeNext) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
                capitalizeNext = false;
            } else {
                result.append(word.toLowerCase());
            }
        }
        return result.toString();
    }
}
