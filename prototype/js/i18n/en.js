// English: everything the game says and shows. {slots} are filled by tr(); a function entry gets the params.
// The page text (ui) is in en-ui.js and must match index.html (a test checks).
import ui from './en-ui.js';

export default {
  ui,
  // describe.js: clock positions, thirds, where am I, the goal guide
  metres: ({ n }) => `${n} metre${n === 1 ? '' : 's'}`,
  clock: { feet: 'at your feet', at: "{hour} o'clock, {metres}" },
  thirds: ['Defending third', 'Middle third', 'Attacking third'],
  side: { left: 'by the left board', right: 'by the right board', centre: 'centre' },
  dir: { left: 'left', right: 'right' },
  where: { place: '{third}, {side}.', ball: 'Ball {pos}.', guide: '{text}.', keeper: 'Keeper {pos}.', round: 'Round {round} of {total}. {text}' },
  guide: {
    head: 'Goal, {m}', far: '{head}, too far to shoot', ahead: '{head}, straight ahead',
    slightly: '{head}, slightly {side}', move: '{head}, move {m} {side}',
  },
  // spoken side calls (speakers mode) and ambient cues
  call: { ahead: 'ahead', left: 'left', right: 'right', behind: 'behind' },
  behind: 'Ball behind you',
  // game events
  gotIt: 'Got it. {time} seconds.', gotBall: 'Got the ball', goal: 'Goal!', saved: 'Saved!', post: 'Post!',
  wide: 'Wide {side}', wideGoal: 'Wide {side} of the goal', voy: 'Voy!', noKick: 'You need the ball to kick',
  timeUp: 'Time is up.', gaveUp: 'Round given up.', nextRound: 'Next round in a moment.',
  paused: 'Paused. Space resumes.', pausedBack: 'Paused. Space resumes, Escape for the menu.',
  speed: 'Speed {rate}', speechSpeed: 'Speech speed {rate}', resume: 'Resume level {level}, round {round}',
  status: {
    idle: 'Choose a level to start', done: 'Level {level} done', paused: 'Paused',
    play: 'Level {level}, {name} · Round {round} of {total} · {time} s',
  },
  // levels.js
  levels: {
    1: { name: 'Find the ball', task: 'Find the ball.', miss: 'not found' },
    2: { name: 'Score', task: 'Score a goal.', miss: 'not scored' },
    3: { name: 'Beat the keeper', task: 'Beat the keeper.', miss: 'not scored' },
  },
  roundIntro: 'Round {round}. {task}',
  extra: {
    2: ['Space kicks straight up the pitch. Line up with the goal, within about twenty metres, and kick.'],
    3: ['Space kicks straight up the pitch.',
      "The keeper sets himself on the ball and can't move once you kick. Get the goal slightly to your left or right, hold that arrow, and kick."],
  },
  summary: { done: 'Level {level} done.', stopped: 'Level {level} stopped after {n} of {total} rounds.', times: '{head} Times: {list}.', sep: ', ' },
  // H
  help: [
    'Arrow up runs toward the goal. Arrow down runs back.',
    'Left and right arrows run to your left and right.',
    'Hold two arrows to run diagonally.',
    'Space kicks the ball straight up the pitch.',
    'Hold left or right while you kick to angle the shot.',
    'W tells you where you are.',
    'G gives up the round.',
    'The bracket keys, or minus and plus, change the speech speed.',
    'Escape pauses. Space resumes.',
    'H says this again.',
  ],
  // the start-screen tour and the headphone check
  learn: {
    left: 'Left', right: 'Right', end: 'That is all the sounds.',
    calls: 'A voice calls where the target is: ahead, left, right or behind. The target is the ball, or the goal when you have the ball.',
    ahead: 'Ball ticking, ahead of you.', behind: 'The same ball, behind you. It sounds duller.',
    roll: 'Ball rolling from your left to your right.', chime: 'The goal chime, ahead.',
    chimeNear: 'The goal chime beeps faster the closer you get to the goal.',
    boardL: 'A knock on the left board.', boardR: 'And on the right board.', board: 'A knock on a board.',
    feet: 'Your footsteps.', voy: 'The keeper shouts voy.',
  },
  // menu messages
  num: '{n}', online: 'online',
  voiceDefault: 'Default voice', voiceIs: 'This is the voice.', noSound: 'This browser cannot play sound.',
  touch: 'This needs a computer with a keyboard. It does not work on a phone or tablet.',
  copy: { blocked: 'Copying was blocked. The text is selected: press {keys} to copy it.', done: 'Copied.' },
  lang: { set: 'English', fallback: 'This browser has no Persian voice. From now on your screen reader speaks the game.' },
  canvas: { left: 'YOUR LEFT · BOARDS', right: 'YOUR RIGHT · BOARDS', attack: 'ATTACKING →', you: 'YOU', keeper: 'GK' },
};
