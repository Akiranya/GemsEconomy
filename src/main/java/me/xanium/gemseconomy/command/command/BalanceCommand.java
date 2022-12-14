package me.xanium.gemseconomy.command.command;

import cloud.commandframework.Command;
import me.lucko.helper.utils.annotation.NonnullByDefault;
import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.GemsMessages;
import me.xanium.gemseconomy.account.Account;
import me.xanium.gemseconomy.command.GemsCommand;
import me.xanium.gemseconomy.command.GemsCommands;
import me.xanium.gemseconomy.command.argument.AccountArgument;
import me.xanium.gemseconomy.currency.Currency;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class BalanceCommand extends GemsCommand {

    public BalanceCommand(GemsEconomy plugin, GemsCommands manager) {
        super(plugin, manager);
    }

    @Override
    public void register() {
        Command<CommandSender> balance = manager
                .commandBuilder("balance", "bal", "money")
                .permission("gemseconomy.command.balance")
                .argument(AccountArgument.optional("account"))
                .handler(context -> {
                    CommandSender sender = context.getSender();
                    Optional<Account> account = context.getOptional("account");
                    if (sender instanceof Player player) {
                        if (account.isEmpty()) { // Player did not specify account, so view the account of their own
                            Account ownAccount = GemsEconomy.getInstance().getAccountManager().getAccount(player);
                            if (ownAccount == null) { // Double check in case the player's account is not loaded yet
                                GemsEconomy.lang().sendComponent(sender, "err_account_missing");
                                return;
                            }
                            sendBalance(player, ownAccount);
                        } else if (account.get().getNickname().equalsIgnoreCase(sender.getName())) { // Player specified his own name
                            sendBalance(player, account.get());
                        } else if (sender.hasPermission("gemseconomy.command.balance.other")) { // Player specified an account, so view other's account
                            sendBalance(player, account.get());
                        } else {
                            GemsEconomy.lang().sendComponent(sender, "err_no_permission", "permission", "gemseconomy.command.balance.other");
                        }
                    } else { // It is a ConsoleSender (usually)
                        if (account.isEmpty()) { // Console must specify an account
                            GemsEconomy.lang().sendComponent(sender, "err_player_is_null");
                        } else {
                            sendBalance(sender, account.get());
                        }
                    }
                })
                .build();

        manager.register(List.of(balance));
    }

    @NonnullByDefault
    private void sendBalance(CommandSender sender, Account account) {
        int currencies = GemsEconomy.getInstance().getCurrencyManager().getCurrencies().size();
        if (currencies == 0) {
            GemsEconomy.lang().sendComponent(sender, "err_no_default_currency");
        } else if (currencies == 1) {
            Currency currency = GemsEconomy.getInstance().getCurrencyManager().getDefaultCurrency();
            if (currency == null) {
                GemsEconomy.lang().sendComponent(sender, "err_balance_none", "account", account.getNickname());
                return;
            }
            double balance = account.getBalance(currency);
            Component balanceMessage = GemsEconomy.lang()
                    .component(sender, "msg_balance_current")
                    .replaceText(GemsMessages.ACCOUNT_REPLACEMENT.apply(account.getNickname()))
                    .replaceText(GemsMessages.AMOUNT_REPLACEMENT.apply(currency, balance));
            GemsEconomy.lang().sendComponent(sender, balanceMessage);
        } else {
            GemsEconomy.lang().sendComponent(sender, "msg_balance_multiple", "account", account.getNickname());
            for (Currency currency : GemsEconomy.getInstance().getCurrencyManager().getCurrencies()) {
                if (sender.hasPermission("gemseconomy.currency.balance." + currency.getSingular())) {
                    double balance = account.getBalance(currency);
                    GemsEconomy.lang().sendComponent(sender, GemsEconomy.lang()
                            .component(sender, "msg_balance_list")
                            .replaceText(GemsMessages.AMOUNT_REPLACEMENT.apply(currency, balance))
                    );
                }
            }
        }
    }

}
