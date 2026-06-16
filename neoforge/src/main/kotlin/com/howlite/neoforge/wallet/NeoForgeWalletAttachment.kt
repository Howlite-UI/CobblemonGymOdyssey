package com.howlite.neoforge.wallet

import com.howlite.CobblemonGymOdyssey
import com.howlite.wallet.WalletData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * Déclare et enregistre le type d'attachement NeoForge pour le wallet du joueur.
 *
 * Suit le même pattern que [com.howlite.neoforge.data.NeoForgeAttachments].
 * Le wallet persiste sur disque et survit à la mort du joueur.
 */
object NeoForgeWalletAttachment {

    private val WALLET_CODEC: Codec<WalletData> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.LONG.fieldOf("balance_ccc").orElse(0L).forGetter { it.balanceCCC },
            Codec.BOOL.fieldOf("auto_collect").orElse(true).forGetter { it.autoCollect },
            Codec.BOOL.fieldOf("hud_enabled").orElse(false).forGetter { it.hudEnabled }
        ).apply(instance) { balance, auto, hud ->
            WalletData().also { w ->
                w.balanceCCC = balance
                w.autoCollect = auto
                w.hudEnabled = hud
            }
        }
    }

    private val REGISTRY: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CobblemonGymOdyssey.MOD_ID)

    val PLAYER_WALLET: Supplier<AttachmentType<WalletData>> =
        REGISTRY.register("player_wallet", Supplier {
            AttachmentType.builder(::WalletData)
                .serialize(WALLET_CODEC)
                .copyOnDeath()
                .build()
        })

    fun register(eventBus: IEventBus) {
        REGISTRY.register(eventBus)
    }
}
