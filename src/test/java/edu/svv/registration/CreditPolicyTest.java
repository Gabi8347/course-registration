package edu.svv.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FR-2 CreditPolicy: ECTS limit by GPA")
class CreditPolicyTest {

    @ParameterizedTest(name = "EP: GPA {0} -> {1} ECTS")
    @CsvSource({
            "1.00, 30",  // class [0.00, 2.00)
            "2.50, 37",  // class [2.00, 3.00)
            "3.25, 45",  // class [3.00, 3.50)
            "3.75, 50"   // class [3.50, 4.00]  (v1.1 honour band)
    })
    @DisplayName("EP: one representative per valid GPA class")
    void equivalenceClasses(double gpa, int expected) {
        assertEquals(expected, CreditPolicy.maxEcts(gpa));
    }

    @ParameterizedTest(name = "BVA: GPA {0} -> {1} ECTS")
    @CsvSource({
            "0.00, 30", "0.01, 30",
            "1.99, 30", "2.00, 37", "2.01, 37",
            "2.99, 37", "3.00, 45", "3.01, 45",
            "3.49, 45", "3.50, 50", "3.51, 50",  // v1.1 honour boundary
            "3.99, 50", "4.00, 50"
    })
    @DisplayName("BVA: below, on and above every GPA boundary")
    void boundaryValues(double gpa, int expected) {
        assertEquals(expected, CreditPolicy.maxEcts(gpa));
    }

    @ParameterizedTest(name = "Negative/BVA: GPA {0} is rejected")
    @ValueSource(doubles = {-0.01, 4.01, -1.0, 10.0, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("Negative: GPA outside [0.00, 4.00] or NaN throws")
    void invalidGpaThrows(double gpa) {
        assertThrows(IllegalArgumentException.class, () -> CreditPolicy.maxEcts(gpa));
    }

    @Test
    @DisplayName("Limits are ordered: low < standard < high < honour")
    void limitsAreMonotonic() {
        int low = CreditPolicy.maxEcts(0.0);
        int standard = CreditPolicy.maxEcts(2.0);
        int high = CreditPolicy.maxEcts(3.0);
        int honour = CreditPolicy.maxEcts(3.5);
        assertTrue(low < standard && standard < high && high < honour);
    }
}
