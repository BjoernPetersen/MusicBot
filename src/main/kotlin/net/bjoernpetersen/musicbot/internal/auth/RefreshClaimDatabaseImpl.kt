package net.bjoernpetersen.musicbot.internal.auth

import java.sql.Connection
import javax.inject.Inject
import net.bjoernpetersen.musicbot.api.auth.Crypto
import net.bjoernpetersen.musicbot.api.config.ByteArraySerializer

internal class RefreshClaimDatabaseImpl @Inject private constructor(
    connection: Connection
) : RefreshClaimDatabase {
    init {
        connection.createStatement().use { statement ->
            statement.execute(
                """CREATE TABLE IF NOT EXISTS refresh_claim(
                    user_id TEXT PRIMARY KEY UNIQUE NOT NULL,
                    claim TEXT NOT NULL)""".trimMargin()
            )
        }
    }

    private val getClaim = connection.prepareStatement(
        "SELECT claim FROM refresh_claim WHERE user_id=?"
    )
    private val createClaim = connection.prepareStatement(
        "INSERT OR ABORT INTO refresh_claim(user_id, claim) VALUES(?, ?)"
    )
    private val deleteClaim = connection.prepareStatement(
        "DELETE FROM refresh_claim WHERE user_id=?"
    )

    override fun getClaim(userId: String): String {
        synchronized(getClaim) {
            getClaim.clearParameters()
            getClaim.setString(1, userId)
            getClaim.executeQuery().use { resultSet ->
                if (!resultSet.next()) return createClaim(userId)
                return resultSet.getString("claim")
            }
        }
    }

    private fun createClaim(userId: String): String {
        val bytes = Crypto.createRandomBytes()
        val encoded = ByteArraySerializer.serialize(bytes)
        synchronized(createClaim) {
            createClaim.clearParameters()
            createClaim.setString(1, userId)
            createClaim.setString(2, encoded)
            createClaim.executeUpdate()
        }
        return encoded
    }

    override fun invalidateClaim(userId: String) {
        synchronized(deleteClaim) {
            deleteClaim.clearParameters()
            deleteClaim.setString(1, userId)
            deleteClaim.executeUpdate()
        }
    }
}
