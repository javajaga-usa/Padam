// Double-click launcher for Padam. Asks for the TMDB key on first run, can start Padam at login,
// and won't start a second copy if one is already running.
//   node launcher.js                  start (interactive)
//   node launcher.js --background     start without questions (used at login)
//   node launcher.js --autostart on   start Padam whenever you log in
//   node launcher.js --autostart off  stop doing that
'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');
const http = require('http');
const readline = require('readline');
const { execFileSync } = require('child_process');

const DIR = __dirname;
const ENV = path.join(DIR, '.env');
const args = process.argv.slice(2);
const background = args.includes('--background');

// One line reader for the whole run, queueing lines so answers typed or pasted ahead aren't lost.
let rl = null;
const lines = [];
const waiting = [];
function ask(question) {
  if (!rl) {
    rl = readline.createInterface({ input: process.stdin });
    rl.on('line', (l) => (waiting.length ? waiting.shift()(l.trim()) : lines.push(l.trim())));
    rl.on('close', () => { while (waiting.length) waiting.shift()(''); });
  }
  process.stdout.write(question);
  return lines.length ? Promise.resolve(lines.shift()) : new Promise((resolve) => waiting.push(resolve));
}
function doneAsking() {
  if (rl) rl.close();
  rl = null;
}

function readEnv() {
  return fs.existsSync(ENV) ? fs.readFileSync(ENV, 'utf8') : '';
}

function setEnv(name, value) {
  const lines = readEnv().split(/\r?\n/).filter((l) => l && !l.startsWith(name + '='));
  lines.unshift(name + '=' + value);
  fs.writeFileSync(ENV, lines.join(os.EOL) + os.EOL);
}

function envValue(name) {
  const m = readEnv().match(new RegExp('^\\s*' + name + '\\s*=\\s*(.*)$', 'm'));
  return m ? m[1].trim().replace(/^["']|["']$/g, '') : '';
}

// Resolves with the port of a Padam server already running on this computer, or null.
function runningPort() {
  const tryPort = (port) => new Promise((resolve) => {
    const req = http.get({ host: '127.0.0.1', port, path: '/api/config', timeout: 800 }, (res) => {
      res.resume();
      resolve(res.statusCode === 200 ? port : null);
    });
    req.on('error', () => resolve(null));
    req.on('timeout', () => { req.destroy(); resolve(null); });
  });
  return tryPort(80).then((p) => p || tryPort(8080));
}

// Waits a few seconds for the login item to bring Padam up.
async function waitForStart() {
  for (let i = 0; i < 10; i++) {
    if (await runningPort()) return true;
    await new Promise((r) => setTimeout(r, 500));
  }
  return false;
}

// ---- Start at login ----------------------------------------------------------

function autostart(on) {
  const node = process.execPath;
  const script = path.join(DIR, 'launcher.js');
  const log = path.join(DIR, 'padam.log');
  const home = os.homedir();

  if (process.platform === 'win32') {
    const file = path.join(process.env.APPDATA, 'Microsoft', 'Windows', 'Start Menu', 'Programs', 'Startup', 'Padam.vbs');
    if (!on) { if (fs.existsSync(file)) fs.unlinkSync(file); return 'Padam will no longer start when you sign in.'; }
    // A .vbs runs node with no window, so nothing pops up at sign-in.
    fs.writeFileSync(file, [
      'Set sh = CreateObject("WScript.Shell")',
      'q = Chr(34)',
      'sh.CurrentDirectory = "' + DIR + '"',
      'sh.Run "cmd /c " & q & q & "' + node + '" & q & " " & q & "' + script + '" & q & " --background >> " & q & "' + log + '" & q & " 2>&1" & q, 0, False',
      '',
    ].join('\r\n'));
    return 'Padam will start on its own each time you sign in to Windows.';
  }

  if (process.platform === 'darwin') {
    const file = path.join(home, 'Library', 'LaunchAgents', 'local.padam.plist');
    const unload = () => { try { execFileSync('launchctl', ['unload', file], { stdio: 'ignore' }); } catch (_) { /* not loaded */ } };
    if (!on) { unload(); if (fs.existsSync(file)) fs.unlinkSync(file); return 'Padam will no longer start when you log in.'; }
    const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;');
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>Label</key><string>local.padam</string>
  <key>ProgramArguments</key><array><string>${esc(node)}</string><string>${esc(script)}</string><string>--background</string></array>
  <key>WorkingDirectory</key><string>${esc(DIR)}</string>
  <key>RunAtLoad</key><true/>
  <key>KeepAlive</key><true/>
  <key>StandardOutPath</key><string>${esc(log)}</string>
  <key>StandardErrorPath</key><string>${esc(log)}</string>
</dict></plist>
`);
    unload();
    execFileSync('launchctl', ['load', file]);
    return 'Padam will start on its own each time you log in to this Mac.';
  }

  const file = path.join(home, '.config', 'systemd', 'user', 'padam.service');
  const ctl = (...a) => execFileSync('systemctl', ['--user', ...a], { stdio: 'ignore' });
  if (!on) {
    try { ctl('disable', '--now', 'padam.service'); } catch (_) { /* not installed */ }
    if (fs.existsSync(file)) fs.unlinkSync(file);
    return 'Padam will no longer start when you log in.';
  }
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `[Unit]
Description=Padam for TV browsers
After=network-online.target

[Service]
WorkingDirectory=${DIR}
ExecStart="${node}" "${script}" --background
Restart=on-failure

[Install]
WantedBy=default.target
`);
  ctl('daemon-reload');
  ctl('enable', '--now', 'padam.service');
  return 'Padam will start on its own each time you log in.';
}

// ---- Main -------------------------------------------------------------------

async function main() {
  const flag = args.indexOf('--autostart');
  if (flag !== -1) {
    console.log(autostart(args[flag + 1] !== 'off'));
    return;
  }

  const port = await runningPort();
  if (port) {
    console.log('Padam is already running. On your TV browser, open:  http://padam.local' + (port === 80 ? '' : ':' + port));
    if (!background) await ask('\nPress Enter to close this window.');
    return;
  }

  if (!envValue('TMDB_API_KEY') && !process.env.TMDB_API_KEY && process.env.PADAM_MOCK !== '1') {
    if (background) {
      console.log('No TMDB key yet. Double-click the Start Padam file once to set it up.');
      process.exit(1);
    }
    console.log('Welcome to Padam.\n');
    console.log('Padam needs a free TMDB key for film lists and posters.');
    console.log('Get one at https://www.themoviedb.org/settings/api (sign in, then "API Key").\n');
    let key = '';
    while (key.length < 32) {
      key = await ask('Paste your TMDB API key and press Enter: ');
      if (key.length < 32) console.log('That looks too short. TMDB keys are at least 32 characters.');
    }
    setEnv('TMDB_API_KEY', key);
    console.log('Saved.\n');
    const yes = /^y/i.test(await ask('Start Padam automatically whenever you log in to this computer? (y/n): '));
    if (yes) {
      try {
        console.log(autostart(true));
        // The login item just started its own copy, so this window has nothing left to do.
        if (process.platform !== 'win32' && await waitForStart()) {
          console.log('\nPadam is starting in the background. On your TV browser, open:  http://padam.local');
          await ask('\nPress Enter to close this window.');
          return;
        }
      } catch (e) {
        console.log('Could not set that up (' + e.message + '). You can still start Padam by double-clicking.');
      }
    }
    console.log('');
  }

  doneAsking();
  if (!background) console.log('Keep this window open while you watch. Closing it stops Padam.\n');
  require('./server');
}

main().then(() => { if (rl) doneAsking(); }, (e) => {
  console.error('Padam could not start: ' + e.message);
  process.exit(1);
});
