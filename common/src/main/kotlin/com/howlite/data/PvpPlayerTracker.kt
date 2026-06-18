package com.howlite.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.io.File
import java.util.UUID

object PvpPlayerTracker {

    data class PlayerProfile(val uuid: String, val name: String)

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val _players = mutableSetOf<PlayerProfile>()

    val registeredPlayers: Set<PlayerProfile>
        get() = synchronized(_players) { _players.toSet() }

    private fun getFile(server: MinecraftServer): File {
        val path = server.getWorldPath(LevelResource.ROOT).resolve("cobblemongymodyssey_players.json")
        return path.toFile()
    }

    fun load(server: MinecraftServer) {
        synchronized(_players) {
            _players.clear()
            try {
                val file = getFile(server)
                if (file.exists()) {
                    val content = file.readText()
                    val type = object : TypeToken<Set<PlayerProfile>>() {}.type
                    val loaded: Set<PlayerProfile>? = gson.fromJson(content, type)
                    if (loaded != null) {
                        _players.addAll(loaded)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun save(server: MinecraftServer) {
        synchronized(_players) {
            try {
                val file = getFile(server)
                val json = gson.toJson(_players)
                file.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registerPlayer(server: MinecraftServer, uuid: UUID, name: String) {
        synchronized(_players) {
            val profile = PlayerProfile(uuid.toString(), name)
            // Si le profil n'existe pas encore ou que le nom a changé pour cet UUID
            val existing = _players.find { it.uuid == profile.uuid }
            if (existing == null) {
                _players.add(profile)
                save(server)
            } else if (existing.name != profile.name) {
                _players.remove(existing)
                _players.add(profile)
                save(server)
            }
        }
    }
}
