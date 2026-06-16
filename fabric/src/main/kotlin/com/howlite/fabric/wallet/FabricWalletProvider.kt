package com.howlite.fabric.wallet

import com.howlite.wallet.WalletData
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import org.ladysnake.cca.api.v3.component.Component

/**
 * Composant Cardinal Components (Fabric) stockant le [WalletData] d'un joueur.
 *
 * Chaque instance est attachée à un joueur via [FabricWalletComponents].
 * Les données sont automatiquement sérialisées/désérialisées par CCA.
 */
class FabricWalletProvider : Component {

    val wallet = WalletData()

    override fun writeToNbt(tag: CompoundTag, registryLookup: HolderLookup.Provider) {
        val walletTag = wallet.toNbt()
        tag.put("wallet", walletTag)
    }

    override fun readFromNbt(tag: CompoundTag, registryLookup: HolderLookup.Provider) {
        if (tag.contains("wallet")) {
            wallet.fromNbt(tag.getCompound("wallet"))
        }
    }
}
