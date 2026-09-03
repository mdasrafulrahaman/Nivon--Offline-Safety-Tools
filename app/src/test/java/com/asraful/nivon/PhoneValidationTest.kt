package com.asraful.nivon

import com.asraful.nivon.data.isValidIndianPhone
import com.asraful.nivon.data.normalizeIndianPhone
import org.junit.Assert.*
import org.junit.Test

class PhoneValidationTest {
    @Test fun normalizesTenDigitIndianMobile() = assertEquals("+919876543210", normalizeIndianPhone("9876543210"))
    @Test fun acceptsValidIndianMobile() = assertTrue(isValidIndianPhone("+91 9876543210"))
    @Test fun rejectsInvalidNumber() = assertFalse(isValidIndianPhone("12345"))
}
