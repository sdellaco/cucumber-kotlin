package io.cucumber.kotlin

import io.cucumber.datatable.DataTable
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions

class StepDefinitions {
    enum class AgeGroup {
        Child, Adult, Retiree
    }

    @ParameterType("(an? )?(child|adult|retiree)")
    fun ageGroup(article: String, s: String): AgeGroup {
        return AgeGroup.entries.first { it.name.equals(s, ignoreCase = true) }
    }

    enum class EmploymentStatus(val key: String) {
        Employee(key = "employee"),
        Unemployed(key = "unemployed"),
        SelfEmployed(key = "self-employed"),
    }

    @ParameterType(value = "employee|unemployed|self-employed", name = "employment-status")
    fun employmentStatus(s: String): EmploymentStatus {
        return EmploymentStatus.entries.first { it.key.equals(s, ignoreCase = true) }
    }

    @Given("I have {long} cukes in my belly")
    fun assertMyBelly(n: Long) {
        Assertions.assertEquals(42L, n)
    }

    @Then("I really have {int} cukes in my belly")
    fun assertReallyHave(n: Int) {
        Assertions.assertEquals(42, n)
    }

    @When("I use a datatable:")
    fun assertDatatable(table: DataTable) {
        Assertions.assertEquals(2, table.asMap().size)
    }

    @Given("^using a regex (\\w{1,3}\\d+)$")
    fun assertRegex(regex: String) {
        Assertions.assertTrue(Regex("\\w{1,3}\\d+").matches(regex))
    }

    @Given("I am {ageGroup} and {employment-status}")
    fun assertGender(ageGroup: AgeGroup, employmentStatus: EmploymentStatus) {
        Assertions.assertEquals(ageGroup, AgeGroup.Adult)
        Assertions.assertEquals(employmentStatus, EmploymentStatus.SelfEmployed)
    }
}