package org.campusfix.model.domain;

public record Issue(long id, long reporterId, long categoryId, String title,
                    String description, String location, Status status, String resolutionNote) {
    public enum Status {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CANCELLED
    }

    public record Summary(Issue issue, String reporterName, String categoryName) {
    }

    public static Issue open(long reporterId, long categoryId, String title,
                             String description, String location) {
        return open(0, reporterId, categoryId, title, description, location);
    }

    public static Issue open(long id, long reporterId, long categoryId, String title,
                             String description, String location) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Select a category.");
        }
        return new Issue(id, reporterId, categoryId,
                requireText(title, "Title", 120),
                requireText(description, "Description", 2000),
                requireText(location, "Location", 120),
                Status.OPEN, null);
    }

    public static String requireResolutionNote(String note) {
        return requireText(note, "Resolution note", 2000);
    }

    private static String requireText(String text, String field, int maxLength) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(String.format("%s is required.", field));
        }
        String value = text.strip();
        if (value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(
                    String.format("%s must be at most %d characters.", field, maxLength));
        }
        return value;
    }
}
