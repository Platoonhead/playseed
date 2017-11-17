package models

case class P3User(firstName: String,
                  lastName: String,
                  email: String,
                  password: String)

case class Phone(homePhone: String,
                 cellPhone: String)

case class P3UserProfile(email: String,
                         firstName: String,
                         lastName: String,
                         birth: Long,
                         gender: String,
                         phone: Phone,
                         city: String,
                         postal: String,
                         receiveEmail: Boolean,
                         receiveSms: Boolean)
