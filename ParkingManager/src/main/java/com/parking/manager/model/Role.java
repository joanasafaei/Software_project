package com.parking.manager.model;

/**
 * سطوح دسترسی در سامانه
 */
public enum Role {
    OWNER,      // مالک – دسترسی کامل
    ADMIN,      // ادمین – تقریباً همه دسترسی‌ها جز حذف کاربران
    OPERATOR    // اپراتور – فقط ثبت ورود/خروج و مشاهده لیست
}