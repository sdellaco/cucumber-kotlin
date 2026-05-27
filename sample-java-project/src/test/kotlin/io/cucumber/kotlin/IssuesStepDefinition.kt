package io.cucumber.kotlin

import io.cucumber.java.en.And
import io.cucumber.java.en.Then

class IssuesStepDefinition {
    @Then("test")
    fun testStep() {
    }

    @And("^the product id is (\\d+)$")
    fun testStepProductId(number: Int) {
    }

    @And("I have {int} cuke(s) in my stomach")
    fun testStepCukes(i: Int) {
    }

    @And("I have {int} cucumber(s) in my belly/stomach")
    fun testStepCucumbers(i: Int) {
    }

    @And("^Product(?: with id (\\d+))? is valid")
    fun testStepProductValid(number: Int?) {
    }
}
