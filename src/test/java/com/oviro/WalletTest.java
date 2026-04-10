package com.oviro;

import com.oviro.exception.InsufficientBalanceException;
import com.oviro.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests Wallet – règles métier")
class WalletTest {

    @Test
    @DisplayName("Crédit augmente le solde")
    void credit_increases_balance() {
        Wallet w = Wallet.builder().build();
        w.credit(BigDecimal.valueOf(5000));
        assertThat(w.getBalance()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Débit diminue le solde")
    void debit_decreases_balance() {
        Wallet w = Wallet.builder().build();
        w.credit(BigDecimal.valueOf(10000));
        w.debit(BigDecimal.valueOf(3000));
        assertThat(w.getBalance()).isEqualByComparingTo("7000");
    }

    @Test
    @DisplayName("Débit impossible si solde insuffisant")
    void debit_throws_when_insufficient() {
        Wallet w = Wallet.builder().build();
        w.credit(BigDecimal.valueOf(1000));
        assertThatThrownBy(() -> w.debit(BigDecimal.valueOf(5000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solde insuffisant");
    }

    @Test
    @DisplayName("Crédit avec montant négatif interdit")
    void credit_negative_throws() {
        Wallet w = Wallet.builder().build();
        assertThatThrownBy(() -> w.credit(BigDecimal.valueOf(-100)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
