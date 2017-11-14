package models

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class EventRepositoryTest extends Specification {

  "Event Repository" should {

    val eventRepository =  new ModelsTest[EventRepository]

    "store a new event" in {
      val event = Event(id = 1, profileID = 1, eventType = "eventType", remoteIp = None, new DateTime(System.currentTimeMillis))

      val storeResult = eventRepository.result(eventRepository.repository.store(event))

      storeResult must equalTo(true)
    }
  }
}
