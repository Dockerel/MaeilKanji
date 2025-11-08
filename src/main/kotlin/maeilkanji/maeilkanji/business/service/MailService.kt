package maeilkanji.maeilkanji.business.service

interface MailService {

    fun send(email: String, content: String)

}
