## HAC Lib (NISD)

NISD-HAC-API, HACHelper or HAC Lib (name pending) is a JVM library written in Kotlin with the purpose of providing tools to students to make their own grading utility programs, separate from Powerschool's official (admin-only) tools.

It directly scrapes HTML data from HAC using JSoup (included as a transitive dependency if you use Jitpack, if using the local .jar from release please includ JSoup yourself).

### Include with Jitpack (https://jitpack.io/#Corrinedev/NISD-HAC-API)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

// build.gradle.kts
dependencies {
    // This will automatically include JSoup for the API if you don't already have it as an implementation
    implementation("com.github.Corrinedev:NISD-HAC-API:1.0.0") //Use latest version from release
}
```

### Features
| Method | Description                                                       |
|---|-------------------------------------------------------------------|
| `returnCurrentGrades()` | Returns grades and class names for current period                 |
| `returnQuarterGrade(quarter)` | Returns grades and class names for a specific quarter             |
| `returnCurrentAssignmentsDf()` | Returns all assignments for current period as structured data     |
| `returnQuarterAssignmentsDf(quarter)` | Returns all assignments for a specific quarter as structured data |
| `returnCurrentAssignmentsHtml()` | Alias for `returnCurrentAssignmentsDf()`                          |
| `returnQuarterAssignmentsHtml(quarter?)` | Returns assignments with Formative/Summative filtering            |
| `returnWeightedGpa()` | Returns weighted GPA as a float                                   |
| `returnCollegeGpa()` | Converts weighted GPA to 4.0 scale                                |
| `returnFullRank()` | Returns full rank string (e.g. "5 out of 300")                    |
| `returnRank()` | Returns rank as an integer                                        |
| `returnFullAddress()` | Returns full address string                                       |
| `returnAddress()` | Returns address stripped of leading non-digit characters          |
| `getUsername()` | Returns the current username                                      |
| `reset()` | Clears cookies and re-authenticates                               |
| `parseHacData(doc)` | Parses HAC HTML into structured class/assignment data             |
