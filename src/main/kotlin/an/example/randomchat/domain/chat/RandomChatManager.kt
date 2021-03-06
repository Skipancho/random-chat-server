package an.example.randomchat.domain.chat

import an.example.randomchat.domain.chat.room.RandomChatRoom
import an.example.randomchat.domain.chat.room.RandomChatRoomManager
import an.example.randomchat.domain.chat.session.ChatMessage
import an.example.randomchat.domain.chat.session.RandomChatMessageHandler
import an.example.randomchat.domain.chat.session.RandomChatSession
import an.example.randomchat.domain.chat.session.RandomChatSessionManager
import an.example.randomchat.domain.user.User
import an.example.randomchat.domain.user.UserRepository
import org.springframework.stereotype.Component

@Component
class RandomChatManager(
    private val userRepository: UserRepository,
    private val randomChatSessionManager: RandomChatSessionManager,
    private val randomChatRoomManager: RandomChatRoomManager,
    private val randomChatMessageHandler: RandomChatMessageHandler
) {
    fun start(userId : Long, session: RandomChatSession){
        val user = userRepository.findById(userId)

        when{
            user == null -> {session.close()}
            randomChatSessionManager.getSession(user) != null ->{
                session.close()
            }
            else ->{ startSession(user, session)}
        }
    }

    private fun startSession(user: User, session: RandomChatSession){
        randomChatSessionManager.addSession(user,session)

        val roomCreated = randomChatRoomManager
            .createRoomOrWaitAnotherUser(user)

        if (roomCreated == null){
            val message = ChatMessage(NOTICE, WAITING_MESSAGE)
            session.sendMessage(message)
        }else{
            sendWelcomeMessage(roomCreated)
        }
    }

    private fun sendWelcomeMessage(room: RandomChatRoom){
        room.users.forEach { user ->
            val message = ChatMessage(NOTICE, WELCOME_MESSAGE)

            randomChatSessionManager.getSession(user)
                ?.sendMessage(message)
        }
    }

    fun sendMessage(sender : User , message : String){
        randomChatMessageHandler.onMessage(sender,message)
    }

    fun closeSession(userId: Long){
        val abandoner = userRepository.findById(userId)

        abandoner?.let {
            randomChatSessionManager.removeSession(abandoner)
            userRepository.deleteUser(abandoner)

            val roomAbandoned = randomChatRoomManager
                .removeUserFromRoom(abandoner)

            roomAbandoned?.let {
                sendQuitMessage(roomAbandoned,abandoner)
            }
        }
    }

    private fun sendQuitMessage(room: RandomChatRoom, abandoner : User){
        val nickName = abandoner.nickName
        val quitMessage = ChatMessage(NOTICE,"${nickName}?????? ???????????????.")

        room.users
            .forEach{ other ->
                randomChatSessionManager
                    .getSession(other)
                    ?.sendMessage(quitMessage)
            }
    }

    companion object{
        const val NOTICE = "??????"
        const val WELCOME_MESSAGE = "???????????? ?????????????????????."
        const val WAITING_MESSAGE = "?????? ?????? ???????????? ????????????. ????????? ??????????????????."
    }
}