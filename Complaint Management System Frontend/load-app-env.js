# Loads .env (APP_ENV=dev|prod) then .env.<APP_ENV> before running the command.
# Already-set process env vars win (so Docker can force REACT_APP_API_BASE_URL).
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

function parseEnvFile(filePath) {
  const parsed = {};
  if (!fs.existsSync(filePath)) {
    return parsed;
  }
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }
    const separator = trimmed.indexOf('=');
    if (separator <= 0) {
      continue;
    }
    parsed[trimmed.slice(0, separator).trim()] = trimmed.slice(separator + 1).trim();
  }
  return parsed;
}

const root = __dirname;
const selector = parseEnvFile(path.join(root, '.env'));
const appEnv = process.env.APP_ENV || selector.APP_ENV || 'dev';
const selectedFile = path.join(root, `.env.${appEnv}`);
if (!fs.existsSync(selectedFile)) {
  console.error(`Missing ${path.basename(selectedFile)}. Set APP_ENV=dev or APP_ENV=prod in .env`);
  process.exit(1);
}

const env = { ...parseEnvFile(selectedFile), ...process.env, APP_ENV: appEnv };
const args = process.argv.slice(2);
if (args.length === 0) {
  console.error('Usage: node load-app-env.js <command> [args...]');
  process.exit(1);
}

const child = spawn(args[0], args.slice(1), { stdio: 'inherit', shell: true, env, cwd: root });
child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 1);
});
