import play.api.data.validation._

val result = Constraints.emailAddress

result("test@sample.com")