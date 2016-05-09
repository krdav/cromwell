package cromwell

import akka.testkit._
import wdl4s.values.WdlString
import cromwell.util.SampleWdl

import scala.language.postfixOps

class PostfixQuantifierWorkflowSpec extends CromwellTestkitSpec {
  "A task which contains a parameter with a zero-or-more postfix quantifier" should {
    "accept an array of size 3" ignore {
      runWdlAndAssertOutputs(
        sampleWdl = SampleWdl.ZeroOrMorePostfixQuantifierWorkflowWithArrayInput,
        EventFilter.info(pattern = s"starting calls: postfix.hello", occurrences = 1),
        expectedOutputs = Map("postfix.hello.greeting" -> WdlString("hello alice,bob,charles"))
      )
    }
    "accept an array of size 1" ignore {
      runWdlAndAssertOutputs(
        sampleWdl = SampleWdl.ZeroOrMorePostfixQuantifierWorkflowWithOneElementArrayInput,
        EventFilter.info(pattern = s"starting calls: postfix.hello", occurrences = 1),
        expectedOutputs = Map("postfix.hello.greeting" -> WdlString("hello alice"))
      )
    }
    "accept an array of size 0" ignore {
      runWdlAndAssertOutputs(
        sampleWdl = SampleWdl.ZeroOrMorePostfixQuantifierWorkflowWithZeroElementArrayInput,
        EventFilter.info(pattern = s"starting calls: postfix.hello", occurrences = 1),
        expectedOutputs = Map("postfix.hello.greeting" -> WdlString("hello"))
      )
    }
  }

  "A task which contains a parameter with a one-or-more postfix quantifier" should {
    "accept an array for the value" ignore {
      runWdlAndAssertOutputs(
        sampleWdl = SampleWdl.OneOrMorePostfixQuantifierWorkflowWithArrayInput,
        EventFilter.info(pattern = s"starting calls: postfix.hello", occurrences = 1),
        expectedOutputs = Map("postfix.hello.greeting" -> WdlString("hello alice,bob,charles"))
      )
    }
    "accept a scalar for the value" ignore {
      runWdlAndAssertOutputs(
        sampleWdl = SampleWdl.OneOrMorePostfixQuantifierWorkflowWithScalarInput,
        EventFilter.info(pattern = s"starting calls: postfix.hello", occurrences = 1),
        expectedOutputs = Map("postfix.hello.greeting" -> WdlString("hello alice"))
      )
    }
  }
}
