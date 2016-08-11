package com.r3corda.core.contracts

import com.r3corda.core.crypto.Party
import com.r3corda.core.crypto.SecureHash
import java.security.PublicKey

// The dummy contract doesn't do anything useful. It exists for testing purposes.

val DUMMY_PROGRAM_ID = DummyContract()

class DummyContract : Contract {

    interface State : ContractState {
        val magicNumber: Int
    }

    data class SingleOwnerState(override val magicNumber: Int = 0, override val owner: PublicKey) : OwnableState, State {
        override val contract = DUMMY_PROGRAM_ID
        override val participants: List<PublicKey>
            get() = listOf(owner)

        override fun withNewOwner(newOwner: PublicKey) = Pair(Commands.Move(), copy(owner = newOwner))
    }

    /**
     * Alternative state with multiple owners. This exists primarily to provide a dummy state with multiple
     * participants, and could in theory be merged with [SingleOwnerState] by putting the additional participants
     * in a different field, however this is a good example of a contract with multiple states.
     */
    data class MultiOwnerState(override val magicNumber: Int = 0,
                               val owners: List<PublicKey>) : ContractState, State {
        override val contract = DUMMY_PROGRAM_ID
        override val participants: List<PublicKey>
            get() = owners
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Move : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: TransactionForContract) {
        // Always accepts.
    }

    // The "empty contract"
    override val legalContractReference: SecureHash = SecureHash.sha256("")

    companion object {
        @JvmStatic
        fun generateInitial(owner: PartyAndReference, magicNumber: Int, notary: Party): TransactionBuilder {
            val state = SingleOwnerState(magicNumber, owner.party.owningKey)
            return TransactionType.General.Builder(notary = notary).withItems(state, Command(Commands.Create(), owner.party.owningKey))
        }

        fun move(prior: StateAndRef<DummyContract.SingleOwnerState>, newOwner: PublicKey): TransactionBuilder {
            val priorState = prior.state.data
            val (cmd, state) = priorState.withNewOwner(newOwner)
            return TransactionType.General.Builder(notary = prior.state.notary).withItems(
                    /* INPUT   */ prior,
                    /* COMMAND */ Command(cmd, priorState.owner),
                    /* OUTPUT  */ state
            )
        }
    }
}