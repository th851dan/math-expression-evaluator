package pdorobisz.evaluator.utils

import pdorobisz.evaluator.errors.{InvalidIdentifier, EvaluatorError, LeftParenthesisNotMatched, RightParenthesisNotMatched}
import pdorobisz.evaluator.tokens._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scalaz.{Failure, Success, Validation}

/**
 * Reverse Polish Notation converter (shunting-yard algorithm).
 */
object RPNConverter {

  private val pattern = """\G(\d+|[\(\)*/+-]|\s+)""".r

  /**
   * Converts expression in infix notation to Reverse Polish Notation expression.
   *
   * @param expression expression in infix notation
   * @return sequence of tokens representing expression in Reverse Polish Notation or error
   */
  def convert(expression: String): Validation[EvaluatorError, Seq[TokenPosition]] = {
    val stack = mutable.Stack[TokenPosition]()
    val output = mutable.ArrayBuffer[TokenPosition]()
    var endPosition = 0
    var previous = ""

    (pattern findAllIn expression).matchData foreach { m => {
      val position: Int = m.start
      (m.matched, previous) match {
        case Operator(operator) =>
          while (operatorOnStackHasHigherPrecedence(operator, stack)) output += stack.pop()
          stack.push(TokenPosition(position, operator))
        case ("(", _) =>
          stack.push(TokenPosition(position, LeftParenthesis))
        case (")", _) =>
          addOperatorsToOutput(stack, output)
          if (stack.isEmpty) return Failure(RightParenthesisNotMatched(position))
          stack.pop()
        case (value, _) if isNotEmpty(value) =>
          output += TokenPosition(position, Value(value.toInt))
        case _ => None
      }
      previous = m.matched
      endPosition = m.end
    }
    }

    if (endPosition != expression.length) return Failure(InvalidIdentifier(endPosition))
    addOperatorsToOutput(stack, output)
    if (stack.nonEmpty) return Failure(LeftParenthesisNotMatched(stack.head.position))
    Success(output)
  }

  private def operatorOnStackHasHigherPrecedence(operator: Operator, stack: mutable.Stack[TokenPosition]): Boolean =
    stack.headOption.exists(_.token.isInstanceOf[Operator]) &&
      operator.precedence <= stack.top.token.asInstanceOf[Operator].precedence

  private def addOperatorsToOutput(stack: mutable.Stack[TokenPosition], output: ArrayBuffer[TokenPosition]) {
    while (stack.headOption.exists(_.token != LeftParenthesis)) output += stack.pop()
  }

  private def isNotEmpty(s: String): Boolean = !s.trim.isEmpty

}
