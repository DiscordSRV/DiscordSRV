package github.scarsz.discordsrv.linking;

import github.scarsz.discordsrv.linking.store.CodeStore;
import github.scarsz.discordsrv.linking.store.DiscordAccountStore;
import github.scarsz.discordsrv.linking.store.MinecraftAccountStore;

public interface AccountSystem extends DiscordAccountStore, MinecraftAccountStore, CodeStore {}
