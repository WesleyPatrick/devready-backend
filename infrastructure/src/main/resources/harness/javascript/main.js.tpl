;(function __devreadyRun() {
  const fs = require('fs')
  const toStderr = (...args) => console.error(...args)
  console.log = toStderr
  console.info = toStderr
  console.debug = toStderr
  process.stdout.write = process.stderr.write.bind(process.stderr)
  const cases = JSON.parse(fs.readFileSync(0, 'utf8'))
  for (const testCase of cases) {
    let output = null
    let error = null
    const started = process.hrtime()
    try {
      const solution = new Solution()
      const result = solution['__DEVREADY_FUNCTION__'](...testCase.args)
      output = result === undefined ? null : result
    } catch (e) {
      error = e instanceof Error ? e.name + ': ' + e.message : 'Erro: ' + String(e)
    }
    const spent = process.hrtime(started)
    const timeMs = Math.round(spent[0] * 1000 + spent[1] / 1e6)
    let line
    try {
      line = JSON.stringify({ position: testCase.position, output: output, error: error, timeMs: timeMs })
    } catch (e) {
      line = JSON.stringify({
        position: testCase.position,
        output: null,
        error: 'Saída que não vira JSON: ' + String(e),
        timeMs: timeMs,
      })
    }
    fs.writeSync(1, '__DEVREADY_MARKER__' + line + '\n')
  }
})()
