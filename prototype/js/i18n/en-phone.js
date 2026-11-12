// English for the phone controls test (prototype/phone/). The page text (ui) must match phone/index.html
// (a test checks). Everything else is heard, not read: short, plain sentences.
export default {
  ui: {
    title: 'Audio Soccer phone test',
    h1: 'Phone controls test',
    lede: 'This short test tries touch controls with the screen reader off. Use the phone speaker, not headphones. Keep your screen reader on for now and press Start. The game checks its own voice, then asks you to turn the screen reader off. From then on the game speaks for itself. At the end it asks you to turn the screen reader back on.',
    start: 'Start',
  },
  check: 'Hello. This is the game voice.',
  english: 'The game found no Persian voice it can use offline on this phone, so it speaks English.',
  noVoice: 'The game found no English or Persian voice on this phone. Keep your screen reader on. The results below say what was found.',
  silent: 'The game tried the voices on this phone, but none of them started speaking. Keep your screen reader on. The results below say what was tried.',
  handover: 'Turn your screen reader off now. Then swipe up in the middle of the screen.',
  menu: {
    how: 'Menu. Swipe right or left to move. Double-tap to choose.',
    back: 'Back to the menu.',
  },
  options: { drag: 'Drag to steer', tilt: 'Tilt to steer', finish: 'Finish' },
  intro: {
    drag: ['Put a finger anywhere on the screen and drag it the way you want to run.', 'Up the screen is toward the goal. Lift your finger to stop.'],
    tilt: ['Hold the phone flat in front of you, and keep it still as each round starts.', 'Tilt it the way you want to run. Tip the top edge down to run toward the goal. Hold it level to stop.'],
    common: ['Tap to kick, with any finger, even while you steer.', 'Tap with two fingers to hear where you are. Tap with three fingers to go back to the menu.'],
  },
  tip: 'A kick goes straight up the pitch. Line up with the goal, within about twenty metres, and kick.',
  noTilt: 'The game cannot read the tilt of this phone. Back to the menu.',
  done: 'Turn your screen reader back on. Your results are on the page.',
  copyBlocked: 'Copying was blocked. The text is selected, so you can copy it yourself.',
};
