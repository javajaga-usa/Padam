// Tiny mDNS responder: answers "padam.local" with this computer's LAN address so the TV can open
// http://padam.local instead of an IP. Zero dependencies; only answers A-record questions for one name.
'use strict';
const dgram = require('dgram');
const os = require('os');

const MDNS_ADDR = '224.0.0.251';
const MDNS_PORT = 5353;
const TYPE_A = 1;
const TYPE_ANY = 255;

function lanAddresses() {
  return Object.values(os.networkInterfaces()).flat()
    .filter((i) => i && i.family === 'IPv4' && !i.internal);
}

// Pick the address on the same subnet as whoever asked, so multi-homed computers answer correctly.
function addressFor(peer) {
  const toInt = (ip) => ip.split('.').reduce((n, o) => (n << 8) + Number(o), 0) >>> 0;
  const all = lanAddresses();
  const match = all.find((i) => ((toInt(i.address) ^ toInt(peer)) & toInt(i.netmask)) === 0);
  return (match || all[0] || {}).address;
}

function readName(buf, offset) {
  const labels = [];
  let jumped = false;
  let next = offset;
  for (let guard = 0; guard < 64; guard++) {
    const len = buf[offset];
    if (len === undefined) return null;
    if ((len & 0xc0) === 0xc0) {
      if (!jumped) next = offset + 2;
      offset = ((len & 0x3f) << 8) | buf[offset + 1];
      jumped = true;
      continue;
    }
    if (len === 0) return { name: labels.join('.'), next: jumped ? next : offset + 1 };
    labels.push(buf.toString('utf8', offset + 1, offset + 1 + len));
    offset += 1 + len;
  }
  return null;
}

function encodeName(name) {
  const parts = name.split('.').map((l) => {
    const b = Buffer.from(l, 'utf8');
    return Buffer.concat([Buffer.from([b.length]), b]);
  });
  return Buffer.concat([...parts, Buffer.from([0])]);
}

function answer(id, name, ip, question) {
  const header = Buffer.alloc(12);
  header.writeUInt16BE(id, 0);
  header.writeUInt16BE(0x8400, 2); // response, authoritative
  header.writeUInt16BE(question ? 1 : 0, 4);
  header.writeUInt16BE(1, 6);
  const rr = Buffer.alloc(14);
  rr.writeUInt16BE(TYPE_A, 0);
  rr.writeUInt16BE(0x8001, 2); // class IN, cache-flush
  rr.writeUInt32BE(120, 4);
  rr.writeUInt16BE(4, 8);
  ip.split('.').forEach((o, i) => rr.writeUInt8(Number(o), 10 + i));
  const q = question ? Buffer.concat([encodeName(name), Buffer.from([0, TYPE_A, 0, 1])]) : Buffer.alloc(0);
  return Buffer.concat([header, q, encodeName(name), rr]);
}

function start(hostname, log = console.log) {
  const wanted = hostname.toLowerCase();
  const socket = dgram.createSocket({ type: 'udp4', reuseAddr: true });

  socket.on('message', (buf, peer) => {
    if (buf.length < 12 || (buf.readUInt16BE(2) & 0x8000)) return; // ignore responses
    let offset = 12;
    for (let i = 0, n = buf.readUInt16BE(4); i < n; i++) {
      const q = readName(buf, offset);
      if (!q) return;
      const type = buf.readUInt16BE(q.next);
      offset = q.next + 4;
      if (q.name.toLowerCase() !== wanted || (type !== TYPE_A && type !== TYPE_ANY)) continue;
      const ip = addressFor(peer.address);
      if (!ip) return;
      if (peer.port !== MDNS_PORT) {
        // "Legacy" one-shot query: reply straight back with the same id and the question echoed.
        socket.send(answer(buf.readUInt16BE(0), hostname, ip, true), peer.port, peer.address);
      } else {
        socket.send(answer(0, hostname, ip, false), MDNS_PORT, MDNS_ADDR);
      }
      return;
    }
  });

  socket.on('error', (e) => {
    log('Could not announce ' + hostname + ' (' + e.code + '). Use the IP address instead.');
    try { socket.close(); } catch (_) { /* already closed */ }
  });

  socket.bind(MDNS_PORT, () => {
    for (const i of lanAddresses()) {
      try { socket.addMembership(MDNS_ADDR, i.address); } catch (_) { /* interface without multicast */ }
    }
    socket.setMulticastTTL(255);
  });
  return socket;
}

module.exports = { start, lanAddresses };
