package org.campusfix.model;

import org.campusfix.model.domain.User;

public interface ModelChange {
    User actor();

    record ReportChanged(User actor, Kind kind, long reportId) implements ModelChange {
        public enum Kind {
            SUBMITTED, EDITED, CANCELLED, WORK_STARTED, RESOLVED
        }
    }

    record CategoryChanged(User actor, Kind kind) implements ModelChange {
        public enum Kind {
            ADDED, RENAMED, ACTIVATED, DEACTIVATED
        }
    }
}
