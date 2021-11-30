package an.example.randomchat.domain.auth

data class SigninResponse(
    val token : String,
    val refreshToken : String,
    val nickname : String
)