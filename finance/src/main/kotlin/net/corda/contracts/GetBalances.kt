package net.corda.contracts

import net.corda.core.contracts.Amount
import net.corda.core.contracts.FungibleAsset
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.builder
import net.corda.schemas.CashSchemaV1
import java.util.*
import kotlin.collections.LinkedHashMap

fun CordaRPCOps.getCashBalance(currency: Currency): Amount<Currency> {
    val sum = builder { CashSchemaV1.PersistentCashState::pennies.sum(groupByColumns = listOf(CashSchemaV1.PersistentCashState::currency)) }
    val sumCriteria = QueryCriteria.VaultCustomQueryCriteria(sum)

    val ccyIndex = builder { CashSchemaV1.PersistentCashState::currency.equal(currency.currencyCode) }
    val ccyCriteria = QueryCriteria.VaultCustomQueryCriteria(ccyIndex)

    val results = this.vaultQueryByCriteria(sumCriteria.and(ccyCriteria), FungibleAsset::class.java)
    if (results.otherResults.isEmpty()) {
        return Amount(0L, currency)
    } else {
        require(results.otherResults.size == 2)
        require(results.otherResults[1] == currency.currencyCode)
        @Suppress("UNCHECKED_CAST")
        val quantity = results.otherResults[0] as Long
        return Amount(quantity, currency)
    }
}

fun ServiceHub.getCashBalance(currency: Currency): Amount<Currency> {
    val sum = builder { CashSchemaV1.PersistentCashState::pennies.sum(groupByColumns = listOf(CashSchemaV1.PersistentCashState::currency)) }
    val sumCriteria = QueryCriteria.VaultCustomQueryCriteria(sum)

    val ccyIndex = builder { CashSchemaV1.PersistentCashState::currency.equal(currency.currencyCode) }
    val ccyCriteria = QueryCriteria.VaultCustomQueryCriteria(ccyIndex)

    val results = this.vaultQueryService.queryBy<FungibleAsset<*>>(sumCriteria.and(ccyCriteria))
    if (results.otherResults.isEmpty()) {
        return Amount(0L, currency)
    } else {
        require(results.otherResults.size == 2)
        require(results.otherResults[1] == currency.currencyCode)
        @Suppress("UNCHECKED_CAST")
        val quantity = results.otherResults[0] as Long
        return Amount(quantity, currency)
    }
}

fun ServiceHub.getCashBalances(): Map<Currency, Amount<Currency>> {
    val sum = builder {
        CashSchemaV1.PersistentCashState::pennies.sum(groupByColumns = listOf(CashSchemaV1.PersistentCashState::currency),
                orderBy = Sort.Direction.DESC)
    }

    val sums = this.vaultQueryService.queryBy<FungibleAsset<*>>(QueryCriteria.VaultCustomQueryCriteria(sum)).otherResults
    val balances = LinkedHashMap<Currency, Amount<Currency>>()
    for (index in 0..sums.size - 1 step 2) {
        val ccy = Currency.getInstance(sums[index + 1] as String)
        balances[ccy] = Amount(sums[index] as Long, ccy)
    }
    return balances
}

fun CordaRPCOps.getCashBalances(): Map<Currency, Amount<Currency>> {
    val sum = builder {
        CashSchemaV1.PersistentCashState::pennies.sum(groupByColumns = listOf(CashSchemaV1.PersistentCashState::currency),
                orderBy = Sort.Direction.DESC)
    }

    val sums = this.vaultQueryBy<FungibleAsset<*>>(QueryCriteria.VaultCustomQueryCriteria(sum)).otherResults
    val balances = LinkedHashMap<Currency, Amount<Currency>>()
    for (index in 0..sums.size - 1 step 2) {
        val ccy = Currency.getInstance(sums[index + 1] as String)
        balances[ccy] = Amount(sums[index] as Long, ccy)
    }
    return balances
}

