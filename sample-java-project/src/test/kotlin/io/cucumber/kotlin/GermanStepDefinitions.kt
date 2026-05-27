package io.cucumber.kotlin

import io.cucumber.java.ParameterType
import io.cucumber.java.de.Angenommen
import io.cucumber.java.de.Dann
import org.junit.jupiter.api.Assertions

class GermanStepDefinitions {
    @ParameterType(name = "stringList", value = "(\"([^\"]+)\"(\\s*(([,]?\\s*)|(and\\s?))\"[^\"]+\")*)")
    fun stringList(strings: String): List<String?> {
        return "\\s*(,|and)?\\s*\"(?<group>[^\"]+)\"".toRegex(RegexOption.MULTILINE)
            .findAll(strings).map { it.groups[2]?.value }.toList()
    }

    @Angenommen("ich habe {long} Gurken in meinem Bauch")
    fun givenCucumbers(n: Long) {
        Assertions.assertEquals(42L, n)
    }

    @Dann("ich habe wirklich {int} Gurken in meinem Bauch")
    fun givenCucumbersReally(n: Int) {
        Assertions.assertEquals(42, n)
    }

    @Dann("ich habe wirklich {stringList} Gurken in meinem Bauch")
    fun givenCucumbersStringList(strings: List<String?>) {
        Assertions.assertEquals(listOf("foo", "bar"), strings)
    }
}
