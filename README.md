# network-bans

This is the punishment/ban plugin I use on my personal Minecraft server.

IP-bans are handled by sending HTTP requests to a remote [nftables-ban](https://github.com/MatthewCash/nftables-ban) instance sitting on the network's firewall.

## Example Configuration

```toml
[database]
username = "network-bans"
password = "network-bans"
url = "jdbc:mysql://127.0.0.1:3306"

[ipban]
api_url = "http://127.0.0.1:9378"
auth_token = "3b395ed3b325251570061c786b7fd5b78e0be9569e032f93546920327e631d82"

```

## Build Instructions

```bash
# build jar with gradle
gradle build
```
