import com.cdv.hac.api.Account
import com.cdv.hac.api.Assignment
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.ifEmpty
import kotlin.time.Clock

fun main() = runBlocking {
    val user = "s796569"
    val pass = "brow5560"

    val acc = Account.createAndLogin(user, pass)

    val classes = acc.getClasses(1)

    val nowDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val importantAssignments = mutableListOf<Assignment>()
    for (klass in classes) {
        val filterAfterNow = klass.assignments.filter { a ->
            a.dropped
        }.ifEmpty { continue }
        importantAssignments.addAll(filterAfterNow)
    }

    importantAssignments.sortBy { it.dateDue }

    for ((name, dateDue, dateAssigned, category, score, _, _, _, _, avgScore) in importantAssignments) {
        println("[$name]")
        println("{---Score: $score")
        println("{---Class AVG: $avgScore")
        println("{---Due: $dateDue")
        println("{---Category: ${category.name}")
    }

    classes.forEach { println(it) }

    println(acc.returnEstimatedQuarterGPA(1))
}