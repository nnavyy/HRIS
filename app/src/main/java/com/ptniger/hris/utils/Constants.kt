package com.ptniger.hris.utils

object Constants {
    object Role {
        const val HR = "hr"
        const val FINANCE = "finance"
        const val MANAGER = "manager"
        const val SUPER_ADMIN = "super_admin"
        const val EMPLOYEE = "employee"
    }

    object LeaveType {
        const val ANNUAL = "annual"
        const val SICK = "sick"
        const val PERMISSION = "permission"
    }

    object LeaveStatus {
        const val PENDING = "pending"
        const val APPROVED = "approved"
        const val REJECTED = "rejected"
    }

    object AttendanceStatus {
        const val PRESENT = "present"
        const val LATE = "late"
        const val ABSENT = "absent"
        const val LEAVE = "leave"
        const val HOLIDAY = "holiday"
        
        // Geofencing & Validation Status
        const val VALID = "valid"
        const val INVALID_LOCATION = "invalid_location"
        const val NEED_REVIEW = "need_review"
        const val REJECTED = "rejected"
        const val APPROVED_MANUALLY = "approved_manually"
    }

    object AttendanceType {
        const val CLOCK_IN = "clock_in"
        const val CLOCK_OUT = "clock_out"
    }

    object PayrollStatus {
        const val DRAFT = "draft"
        const val PENDING_APPROVAL = "pending_approval"
        const val APPROVED = "approved"
        const val REJECTED = "rejected"
        const val FINALIZED = "finalized"
        const val PAID = "paid"
    }

    object EmploymentStatus {
        const val ACTIVE = "active"
        const val PROBATION = "probation"
        const val RESIGNED = "resigned"
    }

    object Collections {
        const val USERS = "users"
        const val EMPLOYEES = "employees"
        const val ATTENDANCE = "attendance"
        const val LEAVE_REQUESTS = "leave_requests"
        const val KPI_CONFIGS = "kpi_configs"
        const val KPI_SCORES = "kpi_scores"
        const val PAYROLLS = "payrolls"
        const val NOTIFICATIONS = "notifications"
        const val AUDIT_LOGS = "audit_logs"
        const val AUTOMATION_RULES = "automation_rules"
        const val OFFICE_LOCATIONS = "office_locations"
    }
}
