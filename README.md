# TFC Public Panel Launcher (Android)

**PPLaunch** is a small client-side "app" meant for use on TFC public panels such as touchscreens and tablets. It enables these devices to power up before the TFC servers without the need for a human to refresh the browser once the TFC servers are online.

The panel will open, then every five seconds it will try to connect to the TFC servers. Upon successful connection it will reload the page automatically. If you see a 401 page from TFC, then there is no assigned public panel for the IP address of the device running this program.

**DISCLAIMER** - This is not an official TFC Product. If something does not work **DO NOT** contact TFC Support, they will not be able to help you!

## First launch
Upon first launch, the TFC Instance will not be set. Open the settings page (with the gear) and set your instance.

## Other Versions
- [Desktop](https://github.com/brianwlynch/pplaunch) - Windows / Ubuntu
- iOS (iPAD) - *Coming Soon*

## Changelog
[CHANGELOG](CHANGELOG.md)

## Debug mode
In the settings menu you can enable debug mode. Debug mode is checked to see if it is enabled every five seconds. Developer tools will open!
The panel will continue to operate as normally. Use the `console` tab in dev tools to see some logs. If the panel does connect to TFC, it **will not** redirect you until you disable debug mode.

---
<a href="https://www.star-history.com/?repos=brianwlynch%2Fpplaunch&type=date&logscale=&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=brianwlynch/pplaunch&type=date&theme=dark&logscale&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=brianwlynch/pplaunch&type=date&logscale&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=brianwlynch/pplaunch-android&type=date&logscale&legend=top-left" />
 </picture>
</a>