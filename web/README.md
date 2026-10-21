# Padam for any TV browser

Run Padam on a computer at home, then open it in the web browser of any smart TV on the same Wi-Fi
(Samsung, LG, Sony, Fire TV, or anything with a browser). You browse with the remote's arrow keys,
OK and Back. No ads, no tracking, no accounts.

It shows the same home screen as the Android TV app: a rotating Featured banner, then In theatres,
Streaming on your apps, Top 10 Tamil films, Top rated, Coming soon, genres and Classics, plus My List and Recently viewed, which every TV in the home shares
(saved in `padam-data.json` next to the server). It also has Tamil-only search and a details page with cast, crew,
where the film is streaming, and official trailers and songs that play full screen.

## Run it

Needs Node.js 18 or newer on the computer (https://nodejs.org, the LTS version). No other installs.

1. Download Padam: https://github.com/javajaga-usa/Padam/archive/refs/heads/main.zip and unzip it.
   Open the `web` folder inside.
2. Double-click **Start Padam**:
   - Windows: `Start Padam.bat`
   - Mac: `Start Padam.command` (the first time, right-click it and choose Open, because it isn't
     from the App Store)
   - Linux: run `./Start\ Padam.command` in a terminal
3. The first time, it asks for a free TMDB key (get one at https://www.themoviedb.org/settings/api)
   and whether to start Padam automatically whenever you log in. Say yes and you never need to
   start it again.
4. On the TV's browser, type **`padam.local`** in the address bar and bookmark it.
   If the browser runs a web search instead of opening the page, type it as `http://padam.local`.

Double-clicking Start Padam while it is already running just shows the address. To stop starting at
login, run `node launcher.js --autostart off` in the `web` folder (`--autostart on` turns it back on).
When it starts at login, messages go to `padam.log` in the same folder.

You can still start it by hand with `node server.js` and a `.env` file copied from `.env.example`.

The key stays on your computer. The TV only talks to your computer, to TMDB for poster images,
and to YouTube for trailers.

To try the screens without a key, run `PADAM_MOCK=1 node server.js` (sample titles, no posters).

If the TV can't open it, allow Node.js through the computer's firewall on private networks
(the first run on Windows and macOS asks), and keep the computer awake while you watch.

### No port number

Padam uses port 80, the browser's default, so the address has no `:8080` on the end.

- **Windows and macOS:** works as is.
- **Linux:** allow Node to use port 80 once with
  `sudo setcap 'cap_net_bind_service=+ep' "$(command -v node)"`.

If port 80 is unavailable (another program uses it, or Linux without the step above), Padam falls back
to 8080 and says so when it starts.

### If `padam.local` doesn't open

The server announces the name `padam.local` on your home network (mDNS, the same way printers and
Apple devices find each other). Android TV and Google TV generally understand it; support in Samsung,
LG and other TV browsers varies by model. If yours doesn't:

1. Use the IP address the server prints when it starts (for example `http://192.168.1.20`).
2. To stop that address changing, reserve it for the computer in your Wi-Fi router's settings
   (usually called "DHCP reservation" or "static lease"). Many routers also let you give it a
   name there, such as `padam`, which then works on every TV.

To use a different name, set `PADAM_NAME=movies` in `.env` and open `movies.local`.

## Remote keys

| Key | Does |
| --- | --- |
| Arrows | Move between films and buttons |
| OK / Enter | Open a film, play a trailer |
| Back | Close the player or go back a screen |

On the Featured banner, Left and Right step through its films and OK opens the one shown.

A pointer remote (LG Magic Remote) or a mouse also works.

## Settings on the TV

Open **Settings** in the top bar to choose where YouTube videos play:

- **Padam** (default): full screen inside Padam.
- **YouTube**: opens the video on youtube.com in the same tab. Choose this if you have YouTube Premium,
  because Premium removes ads only on YouTube itself.

The choice is saved in that TV's browser.

## Server settings (`.env`)

| Name | Default | Meaning |
| --- | --- | --- |
| `TMDB_API_KEY` | (required) | TMDB v3 key or v4 read token |
| `YOUTUBE_API_KEY` | (optional) | Turns on "Free on YouTube" for films rights holders put on YouTube |
| `TMDB_REGION` | `IN` | Country used for "streaming on" |
| `TMDB_LANGUAGE` | `en-IN` | Language for titles and synopses |
| `PORT` | `80` | Port the TV connects to |
| `PADAM_NAME` | `padam` | Name to type on the TV, without `.local` |

## Files

```
server.js          serves the app and proxies TMDB, with a 6-hour in-memory cache
mdns.js            announces padam.local on the home network
public/index.html  page shell and the trailer player
public/app.js      screens and remote-control navigation (plain ES5 for older TV browsers)
public/style.css   10-foot layout that scales with the TV's screen size
```

This product uses the TMDB API but is not endorsed or certified by TMDB.
