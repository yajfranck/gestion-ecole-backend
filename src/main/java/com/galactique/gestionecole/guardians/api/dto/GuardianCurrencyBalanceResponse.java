package com.galactique.gestionecole.guardians.api.dto;

import java.math.BigDecimal;

import com.galactique.gestionecole.guardians.application.GuardianChildPortalApplicationService.CurrencyBalance;

public record GuardianCurrencyBalanceResponse(String currency, BigDecimal amountBilled,
	BigDecimal amountPaid, BigDecimal balance) {
	public static GuardianCurrencyBalanceResponse from(CurrencyBalance balance) {
		return new GuardianCurrencyBalanceResponse(balance.currency(), balance.amountBilled(),
			balance.amountPaid(), balance.balance());
	}
}
