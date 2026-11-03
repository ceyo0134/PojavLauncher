// Mobile on-screen controls for noVNC (WASD, keyboard, double/right click, drag, latches)
import UI from "./app/ui.js";

const K = {
  w: [0x77, "KeyW"], a: [0x61, "KeyA"], s: [0x73, "KeyS"], d: [0x64, "KeyD"],
  e: [0x65, "KeyE"], q: [0x71, "KeyQ"], f: [0x66, "KeyF"], r: [0x72, "KeyR"],
  space: [0x20, "Space"], shift: [0xffe1, "ShiftLeft"], ctrl: [0xffe3, "ControlLeft"],
  alt: [0xffe9, "AltLeft"], win: [0xffeb, "MetaLeft"], esc: [0xff1b, "Escape"],
  enter: [0xff0d, "Enter"], tab: [0xff09, "Tab"], bksp: [0xff08, "Backspace"],
  del: [0xffff, "Delete"],
};

const held = new Set();
function keyDown(n) {
  const r = UI.rfb; if (!r || !K[n]) return;
  r.sendKey(K[n][0], K[n][1], true); held.add(n);
}
function keyUp(n) {
  const r = UI.rfb; if (!r || !K[n]) return;
  r.sendKey(K[n][0], K[n][1], false); held.delete(n);
}
function releaseAll() {
  [...held].forEach(keyUp);
  document.querySelectorAll("#mc-root [data-latch], #mc-root [data-hold]").forEach((b) => b.classList.remove("on"));
}

const css = `
#mc-root, #mc-root * { box-sizing: border-box; -webkit-user-select: none; user-select: none;
  -webkit-touch-callout: none; touch-action: none; -webkit-tap-highlight-color: transparent; }
#mc-toggle { position: fixed; top: 8px; right: 8px; width: 46px; height: 46px; border-radius: 12px;
  border: 1px solid rgba(255,255,255,.35); background: rgba(30,30,45,.75); color: #fff; font-size: 22px;
  z-index: 2147483001; }
#mc-panel { position: fixed; left: 0; right: 0; bottom: 0; z-index: 2147483000;
  padding: 6px 8px calc(6px + env(safe-area-inset-bottom, 0px)); background: rgba(15,15,25,.55);
  backdrop-filter: blur(3px); display: block; }
#mc-panel.hidden { display: none; }
#mc-root button { min-height: 42px; border-radius: 10px; border: 1px solid rgba(255,255,255,.3);
  background: rgba(60,70,100,.85); color: #fff; font-size: 14px; font-weight: 600; padding: 0 8px; }
#mc-root button.on { background: #2e8b57; border-color: #9fe8bd; }
.mc-bar { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 6px; touch-action: pan-x !important; }
.mc-bar button { flex: 0 0 auto; white-space: nowrap; }
.mc-main { display: flex; justify-content: space-between; gap: 10px; }
.mc-dpad { display: grid; grid-template-columns: repeat(3, 52px); grid-auto-rows: 46px; gap: 5px; }
.mc-dpad button { min-height: 46px; font-size: 18px; }
.mc-acts { flex: 1; display: grid; grid-template-columns: repeat(4, 1fr); grid-auto-rows: 42px; gap: 5px; }
.mc-acts .wide { grid-column: span 2; }
#mc-capture { position: fixed; inset: 0; z-index: 2147482000; display: none; background: rgba(46,139,87,.12); }
#mc-hint { position: fixed; top: 10px; left: 50%; transform: translateX(-50%); padding: 6px 12px;
  background: rgba(0,0,0,.7); color: #fff; border-radius: 8px; font-size: 13px; z-index: 2147483001; display: none; }
`;
const style = document.createElement("style");
style.textContent = css;
document.head.appendChild(style);

const root = document.createElement("div");
root.id = "mc-root";
root.innerHTML = `
<button id="mc-toggle">\u{1F3AE}</button>
<div id="mc-hint"></div>
<div id="mc-capture"></div>
<div id="mc-panel">
  <div class="mc-bar">
    <button data-mode="double">Cift tik</button>
    <button data-mode="right">Sag tik</button>
    <button data-mode="drag">Surukle</button>
    <button data-act="kbd">Klavye</button>
    <button data-latch="alt">Alt</button>
    <button data-latch="win">Win</button>
    <button data-hold="del">Del</button>
    <button data-act="cad">Ctrl+Alt+Del</button>
    <button data-act="full">Tam ekran</button>
  </div>
  <div class="mc-main">
    <div class="mc-dpad">
      <span></span><button data-hold="w">W</button><span></span>
      <button data-hold="a">A</button><button data-hold="s">S</button><button data-hold="d">D</button>
    </div>
    <div class="mc-acts">
      <button class="wide" data-hold="space">Space</button>
      <button data-latch="shift">Shift</button>
      <button data-latch="ctrl">Ctrl</button>
      <button data-hold="e">E</button><button data-hold="q">Q</button>
      <button data-hold="f">F</button><button data-hold="r">R</button>
      <button data-hold="esc">Esc</button><button data-hold="tab">Tab</button>
      <button data-hold="enter">Enter</button><button data-hold="bksp">&#9003;</button>
    </div>
  </div>
</div>`;
document.body.appendChild(root);

const panel = root.querySelector("#mc-panel");
const cap = root.querySelector("#mc-capture");
const hint = root.querySelector("#mc-hint");
root.querySelector("#mc-toggle").addEventListener("pointerdown", (e) => {
  e.preventDefault();
  panel.classList.toggle("hidden");
});

// ---- mouse modes (double click / right click / drag) ----
let mode = null;
const HINTS = {
  double: "Cift tiklamak istedigin yere dokun",
  right: "Sag tiklamak istedigin yere dokun",
  drag: "Surukleme modu acik: parmakla surukle",
};
function canvasEl() {
  return document.querySelector("#noVNC_container canvas") || document.querySelector("canvas");
}
function mouseEv(type, x, y, btn) {
  const c = canvasEl(); if (!c) return;
  c.dispatchEvent(new MouseEvent(type, {
    bubbles: true, cancelable: true, view: window, clientX: x, clientY: y,
    button: btn, buttons: type === "mouseup" ? 0 : (1 << btn),
  }));
}
function clicks(x, y, btn, times) {
  mouseEv("mousemove", x, y, 0);
  let t = 20;
  for (let i = 0; i < times; i++) {
    setTimeout(() => mouseEv("mousedown", x, y, btn), t);
    setTimeout(() => mouseEv("mouseup", x, y, btn), t + 40);
    t += 100;
  }
}
function setMode(m) {
  mode = (mode === m) ? null : m;
  root.querySelectorAll("[data-mode]").forEach((b) => b.classList.toggle("on", b.dataset.mode === mode));
  cap.style.display = mode ? "block" : "none";
  hint.style.display = mode ? "block" : "none";
  hint.textContent = mode ? HINTS[mode] : "";
}
let dragging = false;
cap.addEventListener("pointerdown", (e) => {
  e.preventDefault();
  if (mode === "drag") {
    dragging = true;
    cap.setPointerCapture(e.pointerId);
    mouseEv("mousemove", e.clientX, e.clientY, 0);
    mouseEv("mousedown", e.clientX, e.clientY, 0);
  }
});
cap.addEventListener("pointermove", (e) => {
  if (dragging) { e.preventDefault(); mouseEv("mousemove", e.clientX, e.clientY, 0); }
});
function endPointer(e) {
  e.preventDefault();
  if (dragging) {
    mouseEv("mouseup", e.clientX, e.clientY, 0);
    dragging = false;
  } else if (mode === "double") {
    clicks(e.clientX, e.clientY, 0, 2); setMode(null);
  } else if (mode === "right") {
    clicks(e.clientX, e.clientY, 2, 1); setMode(null);
  }
}
cap.addEventListener("pointerup", endPointer);
cap.addEventListener("pointercancel", (e) => {
  if (dragging) { mouseEv("mouseup", e.clientX, e.clientY, 0); dragging = false; }
});

// ---- buttons ----
root.querySelectorAll("button[data-hold]").forEach((b) => {
  const k = b.dataset.hold;
  b.addEventListener("pointerdown", (e) => {
    e.preventDefault(); b.setPointerCapture(e.pointerId);
    keyDown(k); b.classList.add("on");
  });
  const up = (e) => { e.preventDefault(); keyUp(k); b.classList.remove("on"); };
  b.addEventListener("pointerup", up);
  b.addEventListener("pointercancel", up);
});
root.querySelectorAll("button[data-latch]").forEach((b) => {
  const k = b.dataset.latch;
  b.addEventListener("pointerdown", (e) => {
    e.preventDefault();
    if (held.has(k)) { keyUp(k); b.classList.remove("on"); }
    else { keyDown(k); b.classList.add("on"); }
  });
});
root.querySelectorAll("button[data-mode]").forEach((b) => {
  b.addEventListener("pointerdown", (e) => { e.preventDefault(); setMode(b.dataset.mode); });
});
root.querySelectorAll("button[data-act]").forEach((b) => {
  b.addEventListener("pointerdown", (e) => {
    e.preventDefault();
    const a = b.dataset.act;
    if (a === "kbd") { UI.toggleVirtualKeyboard(); }
    else if (a === "cad") { if (UI.rfb) UI.rfb.sendCtrlAltDel(); }
    else if (a === "full") {
      if (document.fullscreenElement) document.exitFullscreen();
      else if (document.documentElement.requestFullscreen) document.documentElement.requestFullscreen();
    }
  });
});
document.addEventListener("visibilitychange", () => { if (document.hidden) releaseAll(); });
window.addEventListener("blur", releaseAll);
