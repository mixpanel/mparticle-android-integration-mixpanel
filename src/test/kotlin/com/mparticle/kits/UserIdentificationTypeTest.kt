package com.mparticle.kits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserIdentificationTypeTest {

    @Test
    fun `fromValue returns CUSTOMER_ID for CustomerId string`() {
        assertEquals(UserIdentificationType.CUSTOMER_ID, UserIdentificationType.fromValue("CustomerId"))
    }

    @Test
    fun `fromValue returns MPID for MPID string`() {
        assertEquals(UserIdentificationType.MPID, UserIdentificationType.fromValue("MPID"))
    }

    @Test
    fun `fromValue returns OTHER for Other string`() {
        assertEquals(UserIdentificationType.OTHER, UserIdentificationType.fromValue("Other"))
    }

    @Test
    fun `fromValue returns OTHER_2 for Other2 string`() {
        assertEquals(UserIdentificationType.OTHER_2, UserIdentificationType.fromValue("Other2"))
    }

    @Test
    fun `fromValue returns OTHER_3 for Other3 string`() {
        assertEquals(UserIdentificationType.OTHER_3, UserIdentificationType.fromValue("Other3"))
    }

    @Test
    fun `fromValue returns OTHER_4 for Other4 string`() {
        assertEquals(UserIdentificationType.OTHER_4, UserIdentificationType.fromValue("Other4"))
    }

    @Test
    fun `fromValue returns null for unknown string`() {
        assertNull(UserIdentificationType.fromValue("Unknown"))
    }

    @Test
    fun `fromValue returns null for null input`() {
        assertNull(UserIdentificationType.fromValue(null))
    }

    @Test
    fun `fromValue returns null for empty string`() {
        assertNull(UserIdentificationType.fromValue(""))
    }

    @Test
    fun `value property returns correct string for each type`() {
        assertEquals("CustomerId", UserIdentificationType.CUSTOMER_ID.value)
        assertEquals("MPID", UserIdentificationType.MPID.value)
        assertEquals("Other", UserIdentificationType.OTHER.value)
        assertEquals("Other2", UserIdentificationType.OTHER_2.value)
        assertEquals("Other3", UserIdentificationType.OTHER_3.value)
        assertEquals("Other4", UserIdentificationType.OTHER_4.value)
    }
}
