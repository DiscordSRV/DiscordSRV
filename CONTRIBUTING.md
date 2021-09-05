# Reporting an issue
Do ***not*** report a problem without going through the following checklist first.

> Did you reload the plugin?

If you did, we won't help/support you. DiscordSRV doesn't like to be reloaded.
It doesn't matter if you reloaded it through PlugMan. It still breaks it. Restart your server completely.

> Are you using the most up-to-date version?

If you aren't, we can't help you. It's as simple as that. Update your plugin.

> Have you turned on debug mode?

This is referring to the `Debug` config option. Before reporting an issue, add `all` to `Debug`,
`/discord reload`, then re-do what is causing your problem. Without debug information in your log, we can't
diagnose what's wrong and won't be able to help as good as we'd normally be able to.

> Have you attached the output of `/discord debug`?

This is our way of getting all the required information to assist you effectively and to help diagnose errors.
Without this, we can't help.

# Translating
If you'd like to help DiscordSRV by translating it to a new language, you'll need to fork the repo and make
edits to your fork's `develop` branch, specifically copy + pasting the English config, messages, synchronization, alerts and voice files and
translating everything except the names of values. Also, add the language and it's associated 639-1 code to
`src/main/java/github/scarsz/discordsrv/util/LangUtil.java` and translate the messages there.

# Submitting a pull request
Submit changes targeting the `develop` branch. PR's to `master` will be denied. Try your best to follow the
existing coding practices. For config options, follow the way options are named in a CamelCase fashion.
Use lombok `@Getter`'s/`@Setter`'s where applicable.
